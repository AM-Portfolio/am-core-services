package com.am.analysis.adapter.mapper;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.*;
import com.am.kafka.config.AnalysisEntityKeys;
import com.am.portfolio.domain.events.PortfolioUpdateEvent;
import com.am.portfolio.domain.model.EquityModel;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AnalysisEventMapper {

    public AnalysisEntity mapPortfolioEvent(PortfolioUpdateEvent event) {
        List<AnalysisHolding> holdings = mapEquitiesToHoldings(event.getEquities(), event.getTotalValue());

        double computedTotalInvestment = (event.getTotalInvestment() != null && event.getTotalInvestment() > 0)
                ? event.getTotalInvestment()
                : holdings.stream()
                        .filter(h -> h.getInvestment() != null && h.getInvestment().getInvestmentValue() != null)
                        .mapToDouble(h -> h.getInvestment().getInvestmentValue())
                        .sum();

        double totalVal = event.getTotalValue() != null ? event.getTotalValue() : 0.0;
        double gainLoss = totalVal - computedTotalInvestment;
        double gainLossPct = computedTotalInvestment > 0 ? (gainLoss / computedTotalInvestment) * 100.0 : 0.0;

        String rawPortfolioId = event.getPortfolioId();
        String effectivePortfolioId = rawPortfolioId != null && !rawPortfolioId.isBlank()
                ? rawPortfolioId
                : AnalysisEntityKeys.GLOBAL_SOURCE_ID;

        String entityId;
        if (AnalysisEntityKeys.isGlobalSourceId(effectivePortfolioId)) {
            entityId = AnalysisEntityKeys.globalEntityId(event.getUserId());
            effectivePortfolioId = AnalysisEntityKeys.GLOBAL_SOURCE_ID;
        } else {
            entityId = AnalysisEntityKeys.portfolioEntityId(effectivePortfolioId, event.getUserId());
        }

        return AnalysisEntity.builder()
                .id(entityId)
                .sourceId(effectivePortfolioId)
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId(event.getUserId())
                .holdings(holdings)
                .performance(PerformanceSummary.builder()
                        .totalValue(totalVal)
                        .totalInvestment(computedTotalInvestment)
                        .totalGainLoss(gainLoss)
                        .totalGainLossPercentage(gainLossPct)
                        .dayChange(event.getTodayGainLoss() != null ? event.getTodayGainLoss() : 0.0)
                        .dayChangePercentage(event.getTodayGainLossPercentage() != null ? event.getTodayGainLossPercentage() : 0.0)
                        .build())
                .lastUpdated(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .build();
    }

    private List<AnalysisHolding> mapEquitiesToHoldings(List<EquityModel> equities, Double totalValue) {
        if (equities == null || equities.isEmpty()) {
            return Collections.emptyList();
        }

        final double validTotalValue = (totalValue != null && totalValue > 0) ? totalValue : 1.0;

        return equities.stream()
                .map(equity -> {
                    double value = equity.getCurrentValue() != null ? equity.getCurrentValue() : equity.getInvestmentValue();
                    double weight = (value / validTotalValue) * 100.0;

                    Double invVal = equity.getInvestmentValue() != null ? equity.getInvestmentValue() : 0.0;
                    Double curVal = equity.getCurrentValue() != null ? equity.getCurrentValue() : value;
                    Double pnl = equity.getProfitLoss() != null ? equity.getProfitLoss() : (curVal - invVal);
                    Double pnlPct = equity.getProfitLossPercentage() != null ? equity.getProfitLossPercentage()
                            : (invVal > 0 ? (pnl / invVal) * 100.0 : 0.0);

                    return AnalysisHolding.builder()
                            .identity(HoldingIdentity.builder()
                                    .symbol(equity.getSymbol())
                                    .name(equity.getName())
                                    .assetClass("EQUITY")
                                    .isin(equity.getIsin())
                                    .companyName(equity.getCompanyName())
                                    .exchange(equity.getExchange())
                                    .build())
                            .investment(InvestmentStats.builder()
                                    .quantity(equity.getQuantity())
                                    .averagePrice(equity.getAveragePrice())
                                    .investmentValue(invVal)
                                    .currentValue(curVal)
                                    .profitLoss(pnl)
                                    .profitLossPercentage(pnlPct)
                                    .weight(weight)
                                    .value(value)
                                    .build())
                            .market(MarketStats.builder()
                                    .currentPrice(equity.getCurrentPrice())
                                    .previousClose(equity.getPreviousClose())
                                    .dayChange(equity.getDayChange() != null
                                            ? equity.getDayChange()
                                            : equity.getTodayProfitLoss())
                                    .dayChangePercentage(equity.getDayChangePercentage() != null
                                            ? equity.getDayChangePercentage()
                                            : equity.getTodayProfitLossPercentage())
                                    .lastUpdatedTime(equity.getLastUpdatedTime())
                                    .build())
                            .classification(AssetClassification.builder()
                                    .sector(equity.getSector())
                                    .industry(equity.getIndustry())
                                    .marketCapType(equity.getMarketCap())
                                    .build())
                            .transactions(mapTransactions(equity.getTransactions()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<Transaction> mapTransactions(List<com.am.portfolio.domain.model.TransactionModel> models) {
        if (models == null || models.isEmpty()) {
            return Collections.emptyList();
        }
        return models.stream()
                .map(m -> Transaction.builder()
                        .date(m.getDate())
                        .quantity(m.getQuantity())
                        .price(m.getPrice())
                        .type(m.getType())
                        .charges(m.getCharges())
                        .tradeId(m.getTradeId())
                        .build())
                .collect(Collectors.toList());
    }

    public AnalysisEntity mapTradeEvent(am.trade.kafka.model.TradeEvent event) {
        double price = event.getPrice() != null ? event.getPrice().doubleValue() : 0.0;
        double quantity = event.getQuantity() != null ? event.getQuantity() : 0.0;
        double tradeValue = price * quantity;

        // Create a single transaction record
        Transaction txn = Transaction.builder()
                .date(LocalDateTime.now()) // Or event timestamp if available
                .quantity(quantity)
                .price(price)
                .type(event.getSide()) // BUY/SELL
                .tradeId(event.getTradeId())
                .build();

        AnalysisHolding holding = AnalysisHolding.builder()
                .identity(HoldingIdentity.builder()
                        .symbol(event.getSymbol())
                        .assetClass("EQUITY")
                        .build())
                .investment(InvestmentStats.builder()
                        .quantity(quantity)
                        .averagePrice(price)
                        .value(tradeValue)
                        .build())
                .transactions(java.util.List.of(txn))
                .build();

        return AnalysisEntity.builder()
                .id("TRADE_" + event.getTradeId())
                .sourceId(event.getSymbol())
                .type(AnalysisEntityType.TRADE)
                .ownerId(event.getAccountId())
                .holdings(java.util.List.of(holding))
                .performance(PerformanceSummary.builder()
                        .totalValue(tradeValue)
                        .totalInvestment(tradeValue)
                        .build())
                .lastUpdated(java.time.LocalDateTime.now())
                .build();
    }

    public List<AnalysisEntity> mapMarketEvent(com.am.common.investment.model.events.EquityPriceUpdateEvent event) {
        return event.getEquityPrices().stream().map(price -> {
            double last = price.getLastPrice() != null ? price.getLastPrice() : 0.0;
            Double prevClose = price.getOhlcv() != null ? price.getOhlcv().getClose() : null;
            double change = prevClose != null && prevClose > 0 ? last - prevClose : 0.0;
            double changePct = prevClose != null && prevClose > 0 ? (change / prevClose) * 100.0 : 0.0;
            return AnalysisEntity.builder()
                .id("MARKET_" + price.getSymbol())
                .sourceId(price.getSymbol())
                .type(AnalysisEntityType.MARKET_INDEX)
                .performance(PerformanceSummary.builder()
                        .totalValue(last)
                        .dayChange(change)
                        .dayChangePercentage(changePct)
                        .build())
                .lastUpdated(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .build();
        }).collect(Collectors.toList());
    }

    public PortfolioUpdateEvent mapEntityToPortfolioUpdateEvent(AnalysisEntity entity) {
        if (entity == null) {
            return null;
        }

        List<EquityModel> equities = Collections.emptyList();
        if (entity.getHoldings() != null) {
            equities = entity.getHoldings().stream()
                    .map(this::mapHoldingToEquity)
                    .collect(Collectors.toList());
        }

        PerformanceSummary perf = entity.getPerformance();
        PortfolioUpdateEvent event = PortfolioUpdateEvent.builder()
                .id(UUID.randomUUID())
                .userId(entity.getOwnerId())
                .portfolioId(entity.getSourceId())
                .equities(equities)
                .timestamp(entity.getLastUpdated() != null ? entity.getLastUpdated() : LocalDateTime.now())
                .build();

        if (perf != null) {
            event.setTotalValue(perf.getTotalValue());
            event.setTotalInvestment(perf.getTotalInvestment());
            event.setTotalGainLoss(perf.getTotalGainLoss());
            event.setTotalGainLossPercentage(perf.getTotalGainLossPercentage());
            event.setTodayGainLoss(perf.getDayChange());
            event.setTodayGainLossPercentage(perf.getDayChangePercentage());
        }

        return event;
    }

    private EquityModel mapHoldingToEquity(AnalysisHolding holding) {
        HoldingIdentity identity = holding.getIdentity();
        InvestmentStats inv = holding.getInvestment();
        MarketStats market = holding.getMarket();
        AssetClassification cls = holding.getClassification();

        EquityModel model = new EquityModel();
        if (identity != null) {
            model.setIsin(identity.getIsin());
            model.setSymbol(identity.getSymbol());
            model.setName(identity.getName());
            model.setCompanyName(identity.getCompanyName());
            model.setExchange(identity.getExchange());
        }
        if (inv != null) {
            model.setQuantity(inv.getQuantity());
            model.setAveragePrice(inv.getAveragePrice());
            model.setInvestmentValue(inv.getInvestmentValue());
            model.setCurrentValue(inv.getCurrentValue());
            model.setProfitLoss(inv.getProfitLoss());
            model.setProfitLossPercentage(inv.getProfitLossPercentage());
        }
        if (market != null) {
            model.setCurrentPrice(market.getCurrentPrice());
            model.setPreviousClose(market.getPreviousClose());
            model.setDayChange(market.getDayChange());
            model.setDayChangePercentage(market.getDayChangePercentage());
            model.setLastUpdatedTime(market.getLastUpdatedTime());
            model.setTodayProfitLoss(market.getDayChange());
            model.setTodayProfitLossPercentage(market.getDayChangePercentage());
        }
        if (cls != null) {
            model.setSector(cls.getSector());
            model.setIndustry(cls.getIndustry());
            model.setMarketCap(cls.getMarketCapType());
        }
        return model;
    }

    public com.am.portfolio.domain.dto.PortfolioUpdateDto mapToDto(PortfolioUpdateEvent event) {
        if (event == null) {
            return null;
        }

        com.am.portfolio.domain.dto.PortfolioUpdateDto dto = com.am.portfolio.domain.dto.PortfolioUpdateDto.builder()
                .userId(event.getUserId())
                .currentValue(event.getTotalValue())
                .investmentValue(event.getTotalInvestment())
                .totalGainLoss(event.getTotalGainLoss())
                .totalGainLossPercentage(event.getTotalGainLossPercentage())
                .todayGainLoss(event.getTodayGainLoss())
                .todayGainLossPercentage(event.getTodayGainLossPercentage())
                .build();

        if (event.getEquities() != null) {
            List<com.am.portfolio.domain.dto.EquityHoldingDto> dtos = event.getEquities().stream()
                    .map(this::mapEquityToDto)
                    .collect(Collectors.toList());
            dto.setEquities(dtos);
        }

        return dto;
    }

    private com.am.portfolio.domain.dto.EquityHoldingDto mapEquityToDto(EquityModel model) {
        return com.am.portfolio.domain.dto.EquityHoldingDto.builder()
                .isin(model.getIsin())
                .symbol(model.getSymbol())
                .quantity(model.getQuantity())
                .currentPrice(model.getCurrentPrice())
                .currentValue(model.getCurrentValue())
                .investmentValue(model.getInvestmentValue())
                .investmentCost(model.getInvestmentValue()) // Assuming cost is same as init value for now
                .profitLoss(model.getProfitLoss())
                .profitLossPercentage(model.getProfitLossPercentage())
                .todayProfitLoss(model.getTodayProfitLoss())
                .todayProfitLossPercentage(model.getTodayProfitLossPercentage())
                .build();
    }
}
