package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.dto.TopMoversResponse;
import com.am.analysis.service.validator.AnalysisAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopMoversAnalysisService {

    private final AnalysisRepository repository;
    private final AnalysisAccessValidator accessValidator;

    public TopMoversResponse getTopMovers(String id, AnalysisEntityType type, String timeFrame, String userId) {
        if (id == null) {
            log.info("Processing Top Movers by Category: Type={}, TimeFrame={}, User={}", type, timeFrame, userId);
            return getTopMoversByCategory(type, timeFrame, userId);
        } else {
            log.info("Processing Top Movers within Entity: ID={}, Type={}, TimeFrame={}, User={}", id, type, timeFrame, userId);
            return getTopMoversWithinEntity(id, type, timeFrame, userId);
        }
    }

    private TopMoversResponse getTopMoversByCategory(AnalysisEntityType type, String timeFrame, String userId) {
        if (type == AnalysisEntityType.PORTFOLIO && userId != null) {
            log.debug("Aggregating portfolio holdings for user: {}", userId);
            // Aggregate all holdings from all user portfolios
            List<AnalysisEntity> portfolios = repository.findByOwnerIdAndType(userId, AnalysisEntityType.PORTFOLIO);
            
            List<com.am.analysis.adapter.model.AnalysisHolding> allHoldings = portfolios.stream()
                .filter(p -> p.getHoldings() != null)
                .flatMap(p -> p.getHoldings().stream())
                .collect(java.util.stream.Collectors.toList());
            
            log.debug("Found {} total holdings from {} portfolios", allHoldings.size(), portfolios.size());

            // Deduplicate by symbol
            Map<String, com.am.analysis.adapter.model.AnalysisHolding> uniqueHoldings = allHoldings.stream()
                .collect(java.util.stream.Collectors.toMap(
                    com.am.analysis.adapter.model.AnalysisHolding::getSymbol,
                    h -> h,
                    (existing, replacement) -> existing // Keep existing
                ));
            
            List<com.am.analysis.adapter.model.AnalysisHolding> holdings = new java.util.ArrayList<>(uniqueHoldings.values());
            
            boolean useDaily = timeFrame == null || "1D".equalsIgnoreCase(timeFrame);

            List<com.am.analysis.adapter.model.AnalysisHolding> gainers = holdings.stream()
                    .sorted((h1, h2) -> Double.compare(getPercentage(h2, useDaily), getPercentage(h1, useDaily))) // Descending
                    .limit(10)
                    .toList();
            
            List<com.am.analysis.adapter.model.AnalysisHolding> losers = holdings.stream()
                    .sorted((h1, h2) -> Double.compare(getPercentage(h1, useDaily), getPercentage(h2, useDaily))) // Ascending
                    .limit(10)
                    .toList();

            return buildTopMoversResponseFromHoldings(gainers, losers, useDaily);
        }

        // Fallback for non-portfolio types or public types (if any)
        List<AnalysisEntity> gainers = repository.findTop10ByTypeOrderByTotalGainLossPercentageDesc(type);
        List<AnalysisEntity> losers = repository.findTop10ByTypeOrderByTotalGainLossPercentageAsc(type);
        return buildTopMoversResponse(gainers, losers);
    }

    private TopMoversResponse getTopMoversWithinEntity(String id, AnalysisEntityType type, String timeFrame, String userId) {
        String compositeId = type.name() + "_" + id;
        Optional<AnalysisEntity> entityOpt = repository.findById(compositeId);

        if (entityOpt.isPresent()) {
            accessValidator.verifyAccess(entityOpt.get(), userId);

            if (entityOpt.get().getHoldings() != null) {
                List<com.am.analysis.adapter.model.AnalysisHolding> holdings = entityOpt.get().getHoldings();

                boolean useDaily = timeFrame == null || "1D".equalsIgnoreCase(timeFrame);

                List<com.am.analysis.adapter.model.AnalysisHolding> gainers = holdings.stream()
                        .sorted((h1, h2) -> Double.compare(getPercentage(h2, useDaily), getPercentage(h1, useDaily))) // Descending
                        .limit(10)
                        .toList();
                
                List<com.am.analysis.adapter.model.AnalysisHolding> losers = holdings.stream()
                        .sorted((h1, h2) -> Double.compare(getPercentage(h1, useDaily), getPercentage(h2, useDaily))) // Ascending
                        .limit(10)
                        .toList();

                return buildTopMoversResponseFromHoldings(gainers, losers, useDaily);
            }
        }
        
        return TopMoversResponse.builder().gainers(List.of()).losers(List.of()).build();
    }

    private double getPercentage(com.am.analysis.adapter.model.AnalysisHolding h, boolean useDaily) {
        if (useDaily) {
            return h.getDayChangePercentage() != null ? h.getDayChangePercentage() : 0.0;
        } else {
            return h.getProfitLossPercentage() != null ? h.getProfitLossPercentage() : 0.0;
        }
    }

    private TopMoversResponse buildTopMoversResponse(List<AnalysisEntity> gainers, List<AnalysisEntity> losers) {
        return TopMoversResponse.builder()
                .gainers(gainers.stream().map(this::mapToMoverItem).toList())
                .losers(losers.stream().map(this::mapToMoverItem).toList())
                .build();
    }

    private TopMoversResponse buildTopMoversResponseFromHoldings(
            List<com.am.analysis.adapter.model.AnalysisHolding> gainers, 
            List<com.am.analysis.adapter.model.AnalysisHolding> losers,
            boolean useDaily) {
        return TopMoversResponse.builder()
                .gainers(gainers.stream().map(h -> mapToMoverItem(h, useDaily)).toList())
                .losers(losers.stream().map(h -> mapToMoverItem(h, useDaily)).toList())
                .build();
    }

    private TopMoversResponse.MoverItem mapToMoverItem(AnalysisEntity entity) {
        log.debug("Mapping AnalysisEntity to MoverItem - ID: {}, TotalGainLoss: {}, TotalGainLossPercentage: {}", 
                entity.getSourceId(), entity.getTotalGainLoss(), entity.getTotalGainLossPercentage());
        return TopMoversResponse.MoverItem.builder()
                .symbol(entity.getSourceId())
                .name(entity.getSourceId())
                .price(BigDecimal.valueOf(entity.getTotalValue() != null ? entity.getTotalValue() : 0.0))
                .changePercentage(entity.getTotalGainLossPercentage() != null ? entity.getTotalGainLossPercentage() : 0.0)
                .changeAmount(BigDecimal.valueOf(entity.getTotalGainLoss() != null ? entity.getTotalGainLoss() : 0.0))
                .build();
    }

    private TopMoversResponse.MoverItem mapToMoverItem(com.am.analysis.adapter.model.AnalysisHolding h, boolean useDaily) {
        log.debug("Mapping AnalysisHolding to MoverItem - Symbol: {}, Name: {}, CurrentPrice: {}, DayChangePercentage: {}, ProfitLossPercentage: {}, DayChange: {}, ProfitLoss: {}, TodayProfitLoss: {}, TodayProfitLossPercentage: {}", 
                h.getSymbol(), h.getName(), h.getCurrentPrice(), h.getDayChangePercentage(), h.getProfitLossPercentage(), h.getDayChange(), h.getProfitLoss(), h.getTodayProfitLoss(), h.getTodayProfitLossPercentage());
        
        double pct = 0.0;
        double amt = 0.0;
        
        if (useDaily) {
            // First try dayChangePercentage (from market data/calculation), fallback to todayProfitLossPercentage (from holding)
            if (h.getDayChangePercentage() != null) {
                pct = h.getDayChangePercentage();
            } else if (h.getTodayProfitLossPercentage() != null) {
                pct = h.getTodayProfitLossPercentage();
            }
            
            // First try dayChange, fallback to todayProfitLoss
            if (h.getDayChange() != null) {
                amt = h.getDayChange();
            } else if (h.getTodayProfitLoss() != null) {
                amt = h.getTodayProfitLoss();
            }
        } else {
             pct = h.getProfitLossPercentage() != null ? h.getProfitLossPercentage() : 0.0;
             amt = h.getProfitLoss() != null ? h.getProfitLoss() : 0.0;
        }

        return TopMoversResponse.MoverItem.builder()
                .symbol(h.getSymbol())
                .name(h.getName() != null ? h.getName() : h.getSymbol())
                .price(BigDecimal.valueOf(h.getCurrentPrice() != null ? h.getCurrentPrice() : 0.0).setScale(2, java.math.RoundingMode.HALF_UP))
                .changePercentage(BigDecimal.valueOf(pct).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                .changeAmount(BigDecimal.valueOf(amt).setScale(2, java.math.RoundingMode.HALF_UP))
                .build();
    }
}
