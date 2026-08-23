package com.am.market.client.service;

import com.am.market.domain.enums.TimeFrame;
import com.am.market.domain.model.HistoricalData;
import com.am.market.domain.model.OHLCVTPoint;
import com.am.portfolio.client.market.api.IndicesApi;
import com.am.portfolio.client.market.api.MarketAnalyticsApi;
import com.am.portfolio.client.market.api.MarketDataApi;
import com.am.portfolio.client.market.api.SecurityExplorerApi;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.model.HistoricalDataResponseV1;
import com.am.portfolio.client.market.model.SecurityDocument;
import com.am.portfolio.client.market.model.SecurityMetadata;
import com.am.portfolio.client.market.model.SecuritySearchRequest;
import com.am.portfolio.client.market.model.StockIndicesMarketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataClientService {

    private final MarketDataApi marketDataApi;
    private final SecurityExplorerApi securityExplorerApi;
    private final MarketAnalyticsApi marketAnalyticsApi;
    private final IndicesApi indicesApi;

    public Map<String, SecurityMetadata> searchSecurities(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            SecuritySearchRequest request = new SecuritySearchRequest();
            request.setSymbols(symbols);

            List<SecurityDocument> response = securityExplorerApi.searchAdvanced(request);

            if (response != null) {
                return response.stream()
                        .filter(doc -> doc.getKey() != null && doc.getKey().getSymbol() != null
                                && doc.getMetadata() != null)
                        .collect(Collectors.toMap(
                                doc -> doc.getKey().getSymbol(),
                                SecurityDocument::getMetadata,
                                (existing, replacement) -> existing));
            }
        } catch (ApiException e) {
            log.error("Failed to search securities from SDK: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error searching securities: {}", e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    public Map<String, String> resolveIsinsToTickers(List<String> isins) {
        if (isins == null || isins.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            SecuritySearchRequest request = new SecuritySearchRequest();
            request.setSymbols(isins);
            List<SecurityDocument> response = securityExplorerApi.searchAdvanced(request);
            if (response != null) {
                return response.stream()
                        .filter(doc -> doc.getKey() != null && doc.getKey().getSymbol() != null && doc.getKey().getIsin() != null)
                        .collect(Collectors.toMap(
                                doc -> doc.getKey().getIsin(),
                                doc -> doc.getKey().getSymbol(),
                                (existing, replacement) -> existing));
            }
        } catch (Exception e) {
            log.error("Failed to resolve ISINs: {}", e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    public Map<String, Object> getQuotes(String symbols, String timeFrame, Boolean refresh) {
        try {
            return marketDataApi.getQuotes(symbols, timeFrame, refresh);
        } catch (ApiException e) {
            log.error("Failed to fetch quotes: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage() != null ? e.getMessage() : "quotes failed");
        } catch (Exception e) {
            log.error("Unexpected error fetching quotes: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage() != null ? e.getMessage() : "quotes failed");
        }
    }

    public List<Map<String, Object>> getMovers(String type, Integer limit, String indexSymbol,
                                               String timeFrame, Boolean expandIndices) {
        try {
            List<Map<String, Object>> movers = marketAnalyticsApi.getMovers(
                    type, limit, indexSymbol, timeFrame, expandIndices);
            return movers != null ? movers : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch movers: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getSectorPerformance(String indexSymbol, String timeFrame,
                                                          Boolean expandIndices) {
        try {
            List<Map<String, Object>> sectors = marketAnalyticsApi.getSectorPerformance(
                    indexSymbol, timeFrame, expandIndices);
            return sectors != null ? sectors : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch sector performance: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public StockIndicesMarketData getIndicesData(List<String> symbols, Boolean forceRefresh) {
        try {
            return indicesApi.getLatestIndicesData(symbols, forceRefresh);
        } catch (Exception e) {
            log.error("Failed to fetch indices data: {}", e.getMessage(), e);
            return null;
        }
    }

    public Map<String, HistoricalData> getHistoricalDataBatch(String symbols, String fromDate,
                                                              String toDate, TimeFrame interval) {
        try {
            com.am.portfolio.client.market.model.HistoricalDataRequest sdkRequest =
                    new com.am.portfolio.client.market.model.HistoricalDataRequest();
            sdkRequest.setSymbols(symbols);
            sdkRequest.setFrom(fromDate);
            sdkRequest.setTo(toDate);
            sdkRequest.setInterval(mapInterval(interval));

            HistoricalDataResponseV1 response = marketDataApi.getHistoricalData(sdkRequest);

            if (response != null && response.getData() != null) {
                return response.getData().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> mapToDomain(entry.getValue())));
            }
        } catch (ApiException e) {
            log.error("Failed to fetch historical data from SDK: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching historical data: {}", e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    private HistoricalData mapToDomain(com.am.portfolio.client.market.model.HistoricalData sdkData) {
        if (sdkData == null) {
            return null;
        }

        return HistoricalData.builder()
                .symbol(sdkData.getTradingSymbol())
                .interval(TimeFrame.fromValue(sdkData.getInterval()))
                .dataPoints(mapDataPoints(sdkData.getDataPoints()))
                .build();
    }

    private List<OHLCVTPoint> mapDataPoints(
            List<com.am.portfolio.client.market.model.OHLCVTPoint> sdkPoints) {
        if (sdkPoints == null) {
            return Collections.emptyList();
        }

        return sdkPoints.stream()
                .map(p -> OHLCVTPoint.builder()
                        .time(p.getTime() != null ? p.getTime().toLocalDateTime() : null)
                        .open(p.getOpen())
                        .high(p.getHigh())
                        .low(p.getLow())
                        .close(p.getClose())
                        .volume(p.getVolume() != null ? p.getVolume().doubleValue() : 0.0)
                        .build())
                .collect(Collectors.toList());
    }

    private com.am.portfolio.client.market.model.HistoricalDataRequest.IntervalEnum mapInterval(
            TimeFrame interval) {
        if (interval == null) {
            return com.am.portfolio.client.market.model.HistoricalDataRequest.IntervalEnum.DAY;
        }

        switch (interval) {
            case DAY:
                return com.am.portfolio.client.market.model.HistoricalDataRequest.IntervalEnum.DAY;
            case WEEK:
                return com.am.portfolio.client.market.model.HistoricalDataRequest.IntervalEnum.WEEK;
            case MONTH:
                return com.am.portfolio.client.market.model.HistoricalDataRequest.IntervalEnum.MONTH;
            default:
                return com.am.portfolio.client.market.model.HistoricalDataRequest.IntervalEnum.DAY;
        }
    }
}
