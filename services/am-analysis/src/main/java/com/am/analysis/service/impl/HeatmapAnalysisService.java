package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.dto.HeatmapResponse;
import com.am.analysis.service.load.AnalysisEntityLoadService;
import com.am.analysis.service.load.BootstrapTrigger;
import com.am.analysis.service.load.EntityLoadRequest;
import com.am.analysis.service.load.EntityLoadResult;
import com.am.analysis.service.validator.AnalysisAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HeatmapAnalysisService {
    private final AnalysisEntityLoadService entityLoadService;
    private final AnalysisAccessValidator accessValidator;
    private final com.am.analysis.adapter.repository.AnalysisRepository repository;
    private final com.am.market.client.service.MarketDataClientService marketDataClientService;

    public HeatmapResponse getHeatmap(String id, AnalysisEntityType type, String userId) {
        List<AnalysisHolding> holdings = List.of();

        if ("ALL".equals(id) && type == AnalysisEntityType.PORTFOLIO) {
            Optional<AnalysisEntity> globalOpt = entityLoadService.loadGlobalPortfolio(userId);
            if (globalOpt.isPresent()) {
                AnalysisEntity global = globalOpt.get();
                enrichWithMarketData(global);
                holdings = global.getHoldings() != null ? global.getHoldings() : List.of();
            } else {
                EntityLoadResult result = entityLoadService.loadPortfoliosForUser(userId, BootstrapTrigger.HTTP_READ);
                List<AnalysisEntity> allPortfolios = result.entities();
                
                AnalysisEntity globalEntity = AnalysisEntity.builder()
                        .id("VIRTUAL_ALL")
                        .type(type)
                        .ownerId(userId)
                        .holdings(allPortfolios.stream()
                                .filter(p -> p.getHoldings() != null)
                                .flatMap(p -> p.getHoldings().stream())
                                .toList())
                        .build();
                enrichWithMarketData(globalEntity);
                holdings = globalEntity.getHoldings() != null ? globalEntity.getHoldings() : List.of();
            }
        } else if (type == AnalysisEntityType.PORTFOLIO) {
            EntityLoadResult result = entityLoadService.loadOne(
                    EntityLoadRequest.onePortfolio(id, userId, BootstrapTrigger.HTTP_READ));
            if (!result.empty()) {
                AnalysisEntity entity = result.entities().get(0);
                enrichWithMarketData(entity);
                holdings = entity.getHoldings() != null ? entity.getHoldings() : List.of();
            }
        } else {
            String compositeId = type.name() + "_" + id;
            Optional<AnalysisEntity> entityOpt = repository.findById(compositeId);
            if (entityOpt.isPresent()) {
                accessValidator.verifyAccess(entityOpt.get(), userId);
                AnalysisEntity entity = entityOpt.get();
                enrichWithMarketData(entity);
                holdings = entity.getHoldings() != null ? entity.getHoldings() : List.of();
            }
        }

        // Group by sector
        Map<String, List<AnalysisHolding>> bySector = holdings.stream()
            .filter(h -> h.getClassification() != null && h.getClassification().getSector() != null)
            .collect(Collectors.groupingBy(h -> h.getClassification().getSector()));

        double totalPortfolioValue = holdings.stream()
            .mapToDouble(h -> h.getInvestment() != null && h.getInvestment().getCurrentValue() != null ? h.getInvestment().getCurrentValue() : 0.0)
            .sum();

        List<HeatmapResponse.SectorTile> tiles = bySector.entrySet().stream()
            .map(entry -> buildSectorTile(entry.getKey(), entry.getValue(), totalPortfolioValue))
            .sorted(Comparator.comparingDouble(HeatmapResponse.SectorTile::getChangePercent).reversed())
            .collect(Collectors.toList());

        return HeatmapResponse.builder()
            .portfolioId(id)
            .sectors(tiles)
            .build();
    }

    private HeatmapResponse.SectorTile buildSectorTile(String sectorName, List<AnalysisHolding> sectorHoldings, double totalPortfolioValue) {
        double sectorTotalValue = sectorHoldings.stream()
            .mapToDouble(h -> h.getInvestment() != null && h.getInvestment().getCurrentValue() != null ? h.getInvestment().getCurrentValue() : 0.0)
            .sum();

        double weightage = totalPortfolioValue > 0 ? (sectorTotalValue / totalPortfolioValue) * 100 : 0.0;

        List<HeatmapResponse.StockTile> stocks = sectorHoldings.stream().map(h -> {
            double value = h.getInvestment() != null && h.getInvestment().getCurrentValue() != null ? h.getInvestment().getCurrentValue() : 0.0;
            double changePercent = h.getMarket() != null && h.getMarket().getDayChangePercentage() != null ? h.getMarket().getDayChangePercentage() : 0.0;
            double weightInSector = sectorTotalValue > 0 ? (value / sectorTotalValue) * 100 : 0.0;
            
            return HeatmapResponse.StockTile.builder()
                .symbol(h.getIdentity() != null ? h.getIdentity().getSymbol() : "UNKNOWN")
                .name(h.getIdentity() != null ? h.getIdentity().getName() : "UNKNOWN")
                .value(value)
                .changePercent(changePercent)
                .weightInSector(weightInSector)
                .build();
        }).collect(Collectors.toList());

        // Calculate weighted average change for the sector
        double sectorChangePercent = stocks.stream()
            .mapToDouble(s -> s.getChangePercent() * (s.getWeightInSector() / 100.0))
            .sum();

        return HeatmapResponse.SectorTile.builder()
            .sectorName(sectorName)
            .changePercent(sectorChangePercent)
            .weightage(weightage)
            .stockCount(stocks.size())
            .totalValue(sectorTotalValue)
            .stocks(stocks)
            .build();
    }
    
    private void enrichWithMarketData(AnalysisEntity entity) {
        if (entity.getHoldings() == null || entity.getHoldings().isEmpty()) {
            return;
        }

        List<String> symbolsToFetch = entity.getHoldings().stream()
                .filter(h -> h.getClassification() == null ||
                            h.getClassification().getSector() == null ||
                            h.getClassification().getSector().isEmpty())
                .filter(h -> h.getIdentity() != null && h.getIdentity().getSymbol() != null)
                .map(h -> h.getIdentity().getSymbol())
                .distinct()
                .toList();

        if (symbolsToFetch.isEmpty()) {
            return;
        }

        Map<String, com.am.portfolio.client.market.model.SecurityMetadata> marketData = marketDataClientService.searchSecurities(symbolsToFetch);

        entity.getHoldings().forEach(h -> {
            if (h.getIdentity() != null && marketData.containsKey(h.getIdentity().getSymbol())) {
                var metadata = marketData.get(h.getIdentity().getSymbol());

                if (h.getClassification() == null) {
                    h.setClassification(new com.am.analysis.adapter.model.components.AssetClassification());
                }

                var cls = h.getClassification();
                if (cls.getSector() == null || cls.getSector().isEmpty()) {
                    cls.setSector(metadata.getSector());
                }
                if (cls.getIndustry() == null || cls.getIndustry().isEmpty()) {
                    cls.setIndustry(metadata.getIndustry());
                }
                if (cls.getMarketCapType() == null || cls.getMarketCapType().isEmpty()) {
                    cls.setMarketCapType(metadata.getMarketCapType());
                }
            }
        });
    }
}
