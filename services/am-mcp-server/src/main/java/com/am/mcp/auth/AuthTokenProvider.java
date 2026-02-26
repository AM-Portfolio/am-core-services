package com.am.mcp.auth;

import com.am.mcp.config.AmMcpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe JWT provider. Priority:
 *   1. am.auth.static-token (set in application.yaml) → use directly, no network
 *   2. Valid cached JWT (TTL not expired)              → return cached
 *   3. POST am-auth /api/v1/auth/token                → fetch, cache, return
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenProvider {

    private final AmMcpProperties props;
    private final RestClient       restClient;

    private volatile String  cachedToken   = null;
    private volatile Instant tokenExpiry   = Instant.EPOCH;
    private final ReentrantLock lock        = new ReentrantLock();

    public String getToken() {
        String staticToken = props.getAuth().getStaticToken();
        if (staticToken != null && !staticToken.isBlank()) {
            return staticToken;
        }
        if (isValid()) return cachedToken;
        return fetch();
    }

    /** Returns Authorization header map ready for injection. */
    public HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        String token = getToken();
        if (token != null && !token.isBlank()) h.setBearerAuth(token);
        return h;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private boolean isValid() {
        return cachedToken != null
                && Instant.now().isBefore(
                        tokenExpiry.minusSeconds(props.getAuth().getRefreshBufferSec()));
    }

    @SuppressWarnings("unchecked")
    private String fetch() {
        lock.lock();
        try {
            if (isValid()) return cachedToken;  // double-check after lock

            String username = props.getAuth().getUsername();
            String password = props.getAuth().getPassword();
            if (username == null || username.isBlank()) {
                log.warn("No auth credentials configured — requests will be unauthenticated");
                return "";
            }

            String url = props.getAuth().getUrl() + "/api/v1/auth/token";
            Map<String, Object> body = Map.of("username", username, "password", password);

            Map<String, Object> data = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (data == null) throw new IllegalStateException("Empty auth response");

            String token = (String) data.getOrDefault("token",
                           data.getOrDefault("accessToken",
                           data.getOrDefault("access_token", "")));
            if (token == null || token.isBlank())
                throw new IllegalStateException("Auth response missing token: " + data);

            int ttl = ((Number) data.getOrDefault("expiresIn",
                              data.getOrDefault("expires_in", 3600))).intValue();
            cachedToken  = token;
            tokenExpiry  = Instant.now().plusSeconds(ttl);
            log.info("JWT acquired from am-auth (TTL={}s)", ttl);
            return token;

        } catch (RestClientException e) {
            log.error("Auth call to am-auth failed: {}", e.getMessage());
            return "";
        } catch (Exception e) {
            log.error("Auth error: {}", e.getMessage(), e);
            return "";
        } finally {
            lock.unlock();
        }
    }
}
