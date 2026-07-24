package com.am.mcp.client;

import com.am.mcp.auth.AuthTokenProvider;
import com.am.mcp.config.AmMcpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Thin HTTP client for portfolio basket APIs (not yet in am-portfolio-client-lib OpenAPI).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BasketApiClient {

    private final RestClient restClient;
    private final AuthTokenProvider authTokenProvider;
    private final AmMcpProperties props;

    public Object getOpportunities(Map<String, Object> body) {
        return post("/v1/basket/opportunities", body);
    }

    public Object getExposure(Map<String, Object> body) {
        return post("/v1/basket/exposure", body);
    }

    public Object getAllocations(Map<String, Object> body) {
        return post("/v1/basket/allocations", body);
    }

    public Object getPreview(Map<String, Object> body) {
        return post("/v1/basket/preview", body);
    }

    public Object calculateQuantities(Map<String, Object> body) {
        return post("/v1/basket/calculate-quantities", body);
    }

    private Object post(String path, Map<String, Object> body) {
        String base = props.getServices().getPortfolioUrl();
        String token = authTokenProvider.getToken();
        RestClient.RequestBodySpec spec = restClient.post()
                .uri(base + path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        if (token != null && !token.isBlank()) {
            spec = spec.header("Authorization", "Bearer " + token);
        }
        return spec.body(body).retrieve().body(Object.class);
    }
}
