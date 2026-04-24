package com.am.market.client.service;

import com.am.market.domain.enums.TimeFrame;
import com.am.market.domain.model.HistoricalData;
import com.am.market.domain.model.OHLCVTPoint;
import com.am.portfolio.client.market.api.MarketDataApi;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.model.HistoricalDataResponseV1;
import com.am.portfolio.client.market.api.SecurityExplorerApi;
import com.am.portfolio.client.market.model.SecuritySearchRequest;
import com.am.portfolio.client.market.model.SecurityDocument;
import com.am.portfolio.client.market.model.SecurityMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
                    .filter(doc -> doc.getKey() != null && doc.getKey().getSymbol() != null && doc.getMetadata() != null)
                    .collect(Collectors.toMap(
                        doc -> doc.getKey().getSymbol(),
                        SecurityDocument::getMetadata,
                        (existing, replacement) -> existing
                    ));
            }
        } catch (ApiException e) {
            log.error("Failed to search securities from SDK: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error searching securities: {}", e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    public Map<String, HistoricalData> getHistoricalDataBatch(String symbols, String fromDate, String toDate, TimeFrame interval) {
        try {
            com.am.portfolio.client.market.model.HistoricalDataRequest sdkRequest = new com.am.portfolio.client.market.model.HistoricalDataRequest();
            sdkRequest.setSymbols(symbols);
            sdkRequest.setFrom(fromDate);
            sdkRequest.setTo(toDate);
            sdkRequest.setInterval(mapInterval(interval));

            HistoricalDataResponseV1 response = marketDataApi.getHistoricalData(sdkRequest);

            if (response != null && response.getData() != null) {
                return response.getData().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> mapToDomain(entry.getValue())
                        ));
            }
        } catch (ApiException e) {
            log.error("Failed to fetch historical data from SDK: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching historical data: {}", e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    private HistoricalData mapToDomain(com.am.portfolio.client.market.model.HistoricalData sdkData) {
        if (sdkData == null) return null;

        return HistoricalData.builder()
                .symbol(sdkData.getTradingSymbol())
                .interval(TimeFrame.fromValue(sdkData.getInterval()))
                .dataPoints(mapDataPoints(sdkData.getDataPoints()))
                .build();
    }

    private List<OHLCVTPoint> mapDataPoints(List<com.am.portfolio.client.market.model.OHLCVTPoint> sdkPoints) {
        if (sdkPoints == null) return Collections.emptyList();

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
    
    private com.am.portfolio.client.market.model.HistoricalDataRequest.IntervalEnum mapInterval(TimeFrame interval) {
        if (interval == null) return com.am.portfolio.client.market.model.HistoricalDataRequest.IntervalEnum.DAY;
        
        switch (interval) {
            case DAY: return com.am.portfolio.client.market.model.HistoricalDataRequest.IntervalEnum.DAY;
            case WEEK: return com.am.portfolio.client.market.model.HistoricalDataRequest.IntervalEnum.WEEK;
            case MONTH: return com.am.portfolio.client.market.model.HistoricalDataRequest.IntervalEnum.MONTH;
            default: return com.am.portfolio.client.market.model.HistoricalDataRequest.IntervalEnum.DAY;
        }
    }
}
