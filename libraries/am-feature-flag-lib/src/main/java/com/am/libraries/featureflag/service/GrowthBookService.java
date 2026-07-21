package com.am.libraries.featureflag.service;

import com.am.libraries.featureflag.config.FeatureFlagProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import growthbook.sdk.java.model.GBContext;
import growthbook.sdk.java.GrowthBook;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RequiredArgsConstructor
public class GrowthBookService {

    private final FeatureFlagProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private GrowthBook growthBook;
    private ExecutorService sseExecutor;
    private volatile boolean running = true;
    private volatile boolean connected = false;

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("GrowthBook Feature Flagging is disabled via configuration.");
            return;
        }

        if (properties.getApiHost() == null || properties.getClientKey() == null) {
            log.warn("GrowthBook is enabled but apiHost or clientKey is missing. Feature flags will default to true.");
            return;
        }

        // Initialize with default fallback state
        initializeDefaultFallback();

        // Start SSE stream listener in a background thread
        sseExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "growthbook-sse-pool");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        sseExecutor.submit(this::startSseConnectionLoop);
    }

    private void initializeDefaultFallback() {
        try {
            // Default rules: If server is down, we default "redis-enabled" to true
            String defaultFeatures = "{\"features\":{\"redis-enabled\":{\"defaultValue\":true}}}";
            GBContext context = GBContext.builder()
                    .featuresJson(defaultFeatures)
                    .build();
            this.growthBook = new GrowthBook(context);
        } catch (Exception e) {
            log.error("Failed to initialize GrowthBook default fallback", e);
        }
    }

    private void startSseConnectionLoop() {
        String sseUrl = properties.getApiHost() + "/api/features/" + properties.getClientKey();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        while (running) {
            try {
                log.info("Establishing SSE connection to GrowthBook: {}", sseUrl);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(sseUrl))
                        .header("Accept", "text/event-stream")
                        .header("User-Agent", "AM-Core-Services/1.0 (GrowthBookClient)")
                        .timeout(Duration.ofHours(1)) // Keep connection alive
                        .GET()
                        .build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200) {
                    connected = true;
                    log.info("Successfully connected to GrowthBook SSE Stream.");
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                        String line;
                        while (running && (line = reader.readLine()) != null) {
                            if (line.startsWith("data:")) {
                                String jsonPayload = line.substring(5).trim();
                                updateFeatures(jsonPayload);
                            }
                        }
                    }
                } else {
                    log.warn("GrowthBook SSE returned HTTP status: {}. Retrying via polling fallback.", response.statusCode());
                    connected = false;
                    fetchViaPolling(client, sseUrl);
                }
            } catch (Exception e) {
                log.error("Error in GrowthBook SSE connection: {}. Retrying in 10 seconds...", e.getMessage());
                connected = false;
            }

            // Sleep before reconnecting or retrying
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void fetchViaPolling(HttpClient client, String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", "AM-Core-Services/1.0 (GrowthBookClient)")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                updateFeatures(response.body());
            }
        } catch (Exception e) {
            log.error("Failed to poll GrowthBook features fallback: {}", e.getMessage());
        }
    }

    private synchronized void updateFeatures(String jsonPayload) {
        try {
            // Validate JSON before passing to GrowthBook
            JsonNode root = objectMapper.readTree(jsonPayload);
            if (root.has("features")) {
                GBContext context = GBContext.builder()
                        .featuresJson(jsonPayload)
                        .build();
                this.growthBook = new GrowthBook(context);
                log.debug("GrowthBook feature rules updated successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to update features from payload: {}", e.getMessage());
        }
    }

    /**
     * Evaluate if a feature flag is enabled.
     */
    public boolean isOn(String featureKey) {
        if (!properties.isEnabled() || growthBook == null) {
            return true; // Default to true if disabled/uninitialized
        }
        try {
            return growthBook.isOn(featureKey);
        } catch (Exception e) {
            log.error("Error evaluating feature flag {}: {}", featureKey, e.getMessage());
            return true; // Safe fallback
        }
    }

    public boolean isConnected() {
        return connected;
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (sseExecutor != null) {
            sseExecutor.shutdownNow();
        }
        log.info("GrowthBook SSE client stopped.");
    }
}
