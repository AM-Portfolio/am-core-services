package com.am.analysis.integration;

import com.am.analysis.dto.MarketDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${am.services.market-data.url}")
    private String marketDataUrl;

    public Map<String, MarketDataResponse.Match> getSecurities(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Map.of();
        }

        String url = marketDataUrl + "/v1/securities/batch-search";
        Map<String, List<String>> request = Map.of("queries", symbols);

        try {
            MarketDataResponse response = restTemplate.postForObject(url, request, MarketDataResponse.class);
            
            if (response != null && response.getResults() != null) {
                Map<String, MarketDataResponse.Match> resultMap = new HashMap<>();
                for (MarketDataResponse.Result result : response.getResults()) {
                    if (result.getMatches() != null && !result.getMatches().isEmpty()) {
                        // Taking the first match as the best match (score 1.0 usually first)
                        resultMap.put(result.getQuery(), result.getMatches().get(0));
                    }
                }
                return resultMap;
            }
        } catch (Exception e) {
            log.error("Failed to fetch market data for symbols: {}", symbols, e);
        }

        return Map.of();
    }
}
