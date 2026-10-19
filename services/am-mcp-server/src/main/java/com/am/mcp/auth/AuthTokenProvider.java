package com.am.mcp.auth;

import com.am.mcp.config.AmMcpProperties;
import com.am.security.context.UserContext;
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
 * JWT provider for outbound calls to portfolio / market / trade / AI.
 * Priority:
 *   1. Inbound request Bearer ({@link UserContext#getToken()}) — only when it looks like a
 *      <em>user</em> JWT (not Keycloak client_credentials / service-account)
 *   2. {@code am.auth.static-token} — ops override
 *   3. Cached service login via am-identity — fallback for MCP service tokens / prove scripts
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenProvider {

    private final AmMcpProperties props;
    private final RestClient restClient;

    private volatile String cachedToken = null;
    private volatile Instant tokenExpiry = Instant.EPOCH;
    private final ReentrantLock lock = new ReentrantLock();

    public String getToken() {
        String requestToken = UserContext.getToken();
        if (requestToken != null && !requestToken.isBlank() && looksLikeUserJwt(requestToken)) {
            return requestToken;
        }

        String staticToken = props.getAuth().getStaticToken();
        if (staticToken != null && !staticToken.isBlank()) {
            return staticToken;
        }

        if (isValid()) {
            return cachedToken;
        }
        return fetch();
    }

    /**
     * Client-credentials tokens (e.g. am-mcp-service) have {@code gty=client_credentials}
     * or {@code preferred_username=service-account-*} — portfolio/trade reject those with 401.
     */
    static boolean looksLikeUserJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return false;
            }
            String payloadJson = new String(
                    java.util.Base64.getUrlDecoder().decode(padBase64(parts[1])),
                    java.nio.charset.StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(payloadJson, Map.class);
            if (payload == null) {
                return false;
            }
            Object gty = payload.get("gty");
            if (gty != null && "client_credentials".equalsIgnoreCase(String.valueOf(gty))) {
                return false;
            }
            Object pref = payload.get("preferred_username");
            if (pref != null && String.valueOf(pref).startsWith("service-account-")) {
                return false;
            }
            return payload.get("email") != null
                    || (pref != null && !String.valueOf(pref).isBlank());
        } catch (Exception e) {
            return false;
        }
    }

    private static String padBase64(String s) {
        int mod = s.length() % 4;
        if (mod == 0) {
            return s;
        }
        return s + "====".substring(mod);
    }

    /** Returns Authorization header map ready for injection. */
    public HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        String token = getToken();
        if (token != null && !token.isBlank()) {
            h.setBearerAuth(token);
        }
        return h;
    }

    private boolean isValid() {
        return cachedToken != null
                && Instant.now().isBefore(
                        tokenExpiry.minusSeconds(props.getAuth().getRefreshBufferSec()));
    }

    @SuppressWarnings("unchecked")
    private String fetch() {
        lock.lock();
        try {
            if (isValid()) {
                return cachedToken;
            }

            String username = props.getAuth().getUsername();
            String password = props.getAuth().getPassword();
            if (username == null || username.isBlank()) {
                log.warn("No auth credentials configured — requests will be unauthenticated");
                return "";
            }

            String url = props.getAuth().getUrl() + "/auth/login";
            Map<String, Object> body = Map.of("username", username, "password", password);

            Map<String, Object> data = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (data == null) {
                throw new IllegalStateException("Empty auth response");
            }

            String token = (String) data.getOrDefault("access_token",
                    data.getOrDefault("accessToken",
                            data.getOrDefault("token", "")));
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("Auth response missing access_token: " + data);
            }

            int ttl = ((Number) data.getOrDefault("expires_in",
                    data.getOrDefault("expiresIn", 3600))).intValue();
            cachedToken = token;
            tokenExpiry = Instant.now().plusSeconds(ttl);
            log.info("JWT acquired from am-identity (TTL={}s)", ttl);
            return token;

        } catch (RestClientException e) {
            log.error("Auth call to am-identity failed: {}", e.getMessage());
            return "";
        } catch (Exception e) {
            log.error("Auth error: {}", e.getMessage(), e);
            return "";
        } finally {
            lock.unlock();
        }
    }
}
