package com.am.mcp.client;

import com.am.mcp.auth.AuthTokenProvider;
import com.am.mcp.config.AmMcpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
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
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_MAP =
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
        return getMap(uri.toUriString());
    }

    public Map<String, Object> getPortfolioHoldings(String portfolioId) {
        String base = props.getServices().getPortfolioUrl();
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromHttpUrl(base + "/v1/portfolios/holdings");
        if (portfolioId != null && !portfolioId.isBlank()) {
            uri.queryParam("portfolioId", portfolioId);
        }
        return getMap(uri.toUriString());
    }

    public List<Map<String, Object>> getPortfolioBasicDetails() {
        String base = props.getServices().getPortfolioUrl();
        return getList(base + "/v1/portfolios/list");
    }

    public Map<String, Object> getPortfolioById(String portfolioId) {
        String base = props.getServices().getPortfolioUrl();
        return getMap(base + "/v1/portfolios/" + portfolioId);
    }

    private Map<String, Object> getMap(String url) {
        Map<String, Object> body = authorized(url).retrieve().body(MAP);
        return body != null ? body : Map.of();
    }

    private List<Map<String, Object>> getList(String url) {
        List<Map<String, Object>> body = authorized(url).retrieve().body(LIST_MAP);
        return body != null ? body : List.of();
    }

    private RestClient.RequestHeadersSpec<?> authorized(String url) {
        String token = authTokenProvider.getToken();
        RestClient.RequestHeadersSpec<?> spec = restClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON);
        if (token != null && !token.isBlank()) {
            spec = spec.header("Authorization", "Bearer " + token);
        }
        return spec;
    }
}
