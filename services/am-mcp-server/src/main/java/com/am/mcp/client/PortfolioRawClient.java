package com.am.mcp.client;

import com.am.mcp.auth.AuthTokenProvider;
import com.am.mcp.config.AmMcpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Raw portfolio HTTP calls that avoid OpenAPI model deserialization mismatches
 * (LocalDateTime vs OffsetDateTime, unknown broker enums, etc.).
 */
@Component
@RequiredArgsConstructor
public class PortfolioRawClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final AuthTokenProvider authTokenProvider;
    private final AmMcpProperties props;

    public Map<String, Object> getPortfolioSummary(String portfolioId) {
        String base = props.getServices().getPortfolioUrl();
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromHttpUrl(base + "/v1/portfolios/summary");
        if (portfolioId != null && !portfolioId.isBlank()) {
            uri.queryParam("portfolioId", portfolioId);
        }
        return get(uri.toUriString());
    }

    private Map<String, Object> get(String url) {
        String token = authTokenProvider.getToken();
        RestClient.RequestHeadersSpec<?> spec = restClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON);
        if (token != null && !token.isBlank()) {
            spec = spec.header("Authorization", "Bearer " + token);
        }
        Map<String, Object> body = spec.retrieve().body(MAP);
        return body != null ? body : Map.of();
    }
}
