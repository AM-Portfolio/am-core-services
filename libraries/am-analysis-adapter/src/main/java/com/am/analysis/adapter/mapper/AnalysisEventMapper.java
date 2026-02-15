package com.am.analysis.adapter.mapper;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.*;
import com.am.portfolio.domain.events.PortfolioUpdateEvent;
import com.am.portfolio.domain.model.EquityModel;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AnalysisEventMapper {

    public AnalysisEntity mapPortfolioEvent(PortfolioUpdateEvent event) {
        // Map equities to holdings
        List<AnalysisHolding> holdings = mapEquitiesToHoldings(event.getEquities(), event.getTotalValue());

        return AnalysisEntity.builder()
                .id("PORTFOLIO_" + event.getPortfolioId())
                .sourceId(event.getPortfolioId())
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId(event.getUserId())
                .holdings(holdings)
                .performance(PerformanceSummary.builder()
                        .totalValue(event.getTotalValue())
                        .totalInvestment(event.getTotalInvestment())
                        .totalGainLoss(event.getTotalGainLoss())
                        .totalGainLossPercentage(event.getTotalGainLossPercentage())
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
                                    .investmentValue(equity.getInvestmentValue())
                                    .currentValue(equity.getCurrentValue())
                                    .profitLoss(equity.getProfitLoss())
                                    .profitLossPercentage(equity.getProfitLossPercentage())
                                    .weight(weight)
                                    .value(value)
                                    .build())
                            .market(MarketStats.builder()
                                    .currentPrice(equity.getCurrentPrice())
                                    .previousClose(equity.getPreviousClose())
                                    .dayChange(equity.getDayChange())
                                    .dayChangePercentage(equity.getDayChangePercentage())
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
        return event.getEquityPrices().stream().map(price -> AnalysisEntity.builder()
                .id("MARKET_" + price.getSymbol())
                .sourceId(price.getSymbol())
                .type(AnalysisEntityType.MARKET_INDEX)
                .performance(PerformanceSummary.builder()
                        .totalValue(price.getLastPrice() != null ? price.getLastPrice() : 0.0)
                        .dayChange(0.0) // If available in PriceUpdate
                        .dayChangePercentage(0.0)
                        .build())
                .lastUpdated(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .build()).collect(Collectors.toList());
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
