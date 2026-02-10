package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.dto.TopMoversResponse;
import com.am.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisServiceImpl implements AnalysisService {

    private final AnalysisRepository repository;
    private final AnalysisCalculationService calculationService;

    @Override
    public AllocationResponse getAllocation(String id, AnalysisEntityType type, String userId) {
        String compositeId = type.name() + "_" + id;
        Optional<AnalysisEntity> entityOpt = repository.findById(compositeId);

        if (entityOpt.isPresent()) {
            verifyAccess(entityOpt.get(), userId);
            return calculationService.calculateAllocation(entityOpt.get());
        }
        
        log.warn("Entity not found for Analysis: ID={}, Type={}", id, type);
        return AllocationResponse.builder()
                .portfolioId(id)
                .sectors(List.of())
                .assetClasses(List.of())
                .build();
    }

    @Override
    public PerformanceResponse getPerformance(String id, AnalysisEntityType type, String timeFrame, String userId) {
        String compositeId = type.name() + "_" + id;
        Optional<AnalysisEntity> entityOpt = repository.findById(compositeId);

        if (entityOpt.isPresent()) {
            verifyAccess(entityOpt.get(), userId);
            return calculationService.calculatePerformance(entityOpt.get(), timeFrame);
        }

        log.warn("Entity not found for Performance: ID={}, Type={}", id, type);
        return PerformanceResponse.builder()
                .portfolioId(id)
                .timeFrame(timeFrame)
                .chartData(List.of())
                .build();
    }

    @Override
    public TopMoversResponse getTopMovers(String id, AnalysisEntityType type, String timeFrame, String userId) {
        if (id == null) {
            // Case 1: Top Movers OF this Category
            // Public Category (e.g. Top ETFs) -> No verification needed
            // Private Category (e.g. Top Portfolios) -> Only show USER'S portfolios? 
            // Current repository query finds ALL. 
            // Security: If type is private, we should arguably only return user's entities or deny generic list.
            // For now, assuming "Top Private Portfolios" is not a valid public feature, 
            // but if requested, we should probably filter by OwnerId in the query.
            // Given the requirement of "generic top movers", let's assume it's for Public Types or Global stats.
            return getTopMoversByCategory(type);
        } else {
            // Case 2: Top Movers WITHIN this Entity
            return getTopMoversWithinEntity(id, type, userId);
        }
    }

    private TopMoversResponse getTopMoversByCategory(AnalysisEntityType type) {
        List<AnalysisEntity> gainers = repository.findTop10ByTypeOrderByTotalGainLossPercentageDesc(type);
        List<AnalysisEntity> losers = repository.findTop10ByTypeOrderByTotalGainLossPercentageAsc(type);
        return buildTopMoversResponse(gainers, losers);
    }

    private TopMoversResponse getTopMoversWithinEntity(String id, AnalysisEntityType type, String userId) {
        String compositeId = type.name() + "_" + id;
        Optional<AnalysisEntity> entityOpt = repository.findById(compositeId);

        if (entityOpt.isPresent()) {
            verifyAccess(entityOpt.get(), userId);

            if (entityOpt.get().getHoldings() != null) {
                List<String> holdingSymbols = entityOpt.get().getHoldings().stream()
                        .map(h -> h.getSymbol())
                        .toList();

                // Fetch current market data for these holdings
                List<AnalysisEntity> holdingEntities = repository.findBySourceIdInAndType(holdingSymbols, AnalysisEntityType.MARKET_INDEX);

                // Sort by Performance
                List<AnalysisEntity> gainers = holdingEntities.stream()
                        .sorted((e1, e2) -> Double.compare(getPercentage(e2), getPercentage(e1))) // Descending
                        .limit(10)
                        .toList();
                
                List<AnalysisEntity> losers = holdingEntities.stream()
                        .sorted((e1, e2) -> Double.compare(getPercentage(e1), getPercentage(e2))) // Ascending
                        .limit(10)
                        .toList();

                return buildTopMoversResponse(gainers, losers);
            }
        }
        
        return TopMoversResponse.builder().gainers(List.of()).losers(List.of()).build();
    }

    /**
     * Verifies if the user is authorized to access the entity.
     * - Private entities (Portfolio, Trade, Basket) MUST be owned by the requesting user.
     * - Public entities (Market Index, ETF, Mutual Fund) have no owner (null) and are accessible to anyone.
     */
    private void verifyAccess(AnalysisEntity entity, String userId) {
        // If type is explicitly private OR if the entity has an owner defined
        boolean isPrivate = isPrivateEntity(entity.getType());
        
        if (isPrivate) {
             // Strict check for private types
             if (entity.getOwnerId() == null || !userId.equals(entity.getOwnerId())) {
                 log.warn("Unauthorized access: User {} attempted to access Private Entity {} (Owner: {})", userId, entity.getId(), entity.getOwnerId());
                 throw new SecurityException("Unauthorized access to private resource");
             }
        }
        // Implicit check: If not private type, and ownerId IS set (e.g. private basket?), should we check?
        // Ideally yes.
        else if (entity.getOwnerId() != null && !userId.equals(entity.getOwnerId())) {
             log.warn("Unauthorized access: User {} attempted to access User-Owned Entity {} (Owner: {})", userId, entity.getId(), entity.getOwnerId());
             throw new SecurityException("Unauthorized access to user-owned resource");
        }
        
        // If OwnerId is null (Public Data), access is GRANTED.
    }

    private boolean isPrivateEntity(AnalysisEntityType type) {
        return type == AnalysisEntityType.PORTFOLIO || type == AnalysisEntityType.TRADE || type == AnalysisEntityType.BASKET;
    }

    private double getPercentage(AnalysisEntity e) {
        return e.getTotalGainLossPercentage() != null ? e.getTotalGainLossPercentage() : 0.0;
    }

    private TopMoversResponse buildTopMoversResponse(List<AnalysisEntity> gainers, List<AnalysisEntity> losers) {
        return TopMoversResponse.builder()
                .gainers(gainers.stream().map(this::mapToMoverItem).toList())
                .losers(losers.stream().map(this::mapToMoverItem).toList())
                .build();
    }

    private TopMoversResponse.MoverItem mapToMoverItem(AnalysisEntity entity) {
        return TopMoversResponse.MoverItem.builder()
                .symbol(entity.getSourceId())
                .name(entity.getSourceId())
                .price(BigDecimal.valueOf(entity.getTotalValue() != null ? entity.getTotalValue() : 0.0))
                .changePercentage(entity.getTotalGainLossPercentage() != null ? entity.getTotalGainLossPercentage() : 0.0)
                .changeAmount(BigDecimal.valueOf(entity.getTotalGainLoss() != null ? entity.getTotalGainLoss() : 0.0))
                .build();
    }
}
