package com.am.analysis.adapter.mapper;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
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
                                .totalValue(event.getTotalValue())
                                .totalInvestment(event.getTotalInvestment())
                                .totalGainLoss(event.getTotalGainLoss())
                                .totalGainLossPercentage(event.getTotalGainLossPercentage())
                                .lastUpdated(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                                .additionalStats(java.util.Map.of(
                                                "dayChange",
                                                event.getTodayGainLoss() != null ? event.getTodayGainLoss() : 0.0,
                                                "dayChangePercentage",
                                                event.getTodayGainLossPercentage() != null
                                                                ? event.getTodayGainLossPercentage()
                                                                : 0.0))
                                .build();
        }

        private List<AnalysisHolding> mapEquitiesToHoldings(List<EquityModel> equities, Double totalValue) {
                if (equities == null || equities.isEmpty()) {
                        return Collections.emptyList();
                }

                // Use investment value if total value is not available or is zero
                final double validTotalValue = (totalValue != null && totalValue > 0) ? totalValue : 1.0;

                return equities.stream()
                                .map(equity -> {
                                        double value = equity.getCurrentValue() != null ? equity.getCurrentValue()
                                                        : equity.getInvestmentValue();
                                        double weight = (value / validTotalValue) * 100.0;

                                        return AnalysisHolding.builder()
                                                        // Basic Identifiers
                                                        .symbol(equity.getSymbol())
                                                        .name(equity.getName())
                                                        .assetClass("EQUITY") // All items are equities from EquityModel
                                                        .isin(equity.getIsin())
                                                        .companyName(equity.getCompanyName())

                                                        // Holding Information
                                                        .quantity(equity.getQuantity())
                                                        .averagePrice(equity.getAveragePrice())

                                                        // Current Market Data
                                                        .currentPrice(equity.getCurrentPrice())
                                                        .previousClose(equity.getPreviousClose())
                                                        .lastUpdatedTime(equity.getLastUpdatedTime())

                                                        // Investment Values
                                                        .investmentValue(equity.getInvestmentValue())
                                                        .currentValue(equity.getCurrentValue())

                                                        // Portfolio Metrics
                                                        .value(value) // Backward compatibility
                                                        .weight(weight)

                                                        // Profit/Loss Metrics
                                                        .profitLoss(equity.getProfitLoss())
                                                        .profitLossPercentage(equity.getProfitLossPercentage())

                                                        // Intraday Metrics
                                                        .todayProfitLoss(equity.getTodayProfitLoss())
                                                        .todayProfitLossPercentage(
                                                                        equity.getTodayProfitLossPercentage())
                                                        .dayChange(equity.getDayChange())
                                                        .dayChangePercentage(equity.getDayChangePercentage())

                                                        // Classification (for sector allocation)
                                                        .sector(equity.getSector())
                                                        .industry(equity.getIndustry())
                                                        .exchange(equity.getExchange())
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        public AnalysisEntity mapTradeEvent(am.trade.kafka.model.TradeEvent event) {
                return AnalysisEntity.builder()
                                .id("TRADE_" + event.getTradeId())
                                .sourceId(event.getSymbol())
                                .type(AnalysisEntityType.TRADE)
                                .ownerId(event.getAccountId())
                                .additionalStats(java.util.Map.of(
                                                "price", event.getPrice(),
                                                "quantity", event.getQuantity(),
                                                "side", event.getSide(),
                                                "status", event.getStatus()))
                                .lastUpdated(java.time.LocalDateTime.now())
                                .build();
        }

        public java.util.List<AnalysisEntity> mapMarketEvent(
                        com.am.common.investment.model.events.EquityPriceUpdateEvent event) {
                return event.getEquityPrices().stream().map(price -> AnalysisEntity.builder()
                                .id("MARKET_" + price.getSymbol())
                                .sourceId(price.getSymbol())
                                .type(AnalysisEntityType.MARKET_INDEX) // Treating individual stocks as index/entity for
                                                                       // analysis
                                .totalValue(price.getLastPrice() != null ? price.getLastPrice() : 0.0)
                                .totalGainLoss(0.0)
                                .totalGainLossPercentage(0.0)
                                .additionalStats(java.util.Map.of(
                                                "price", price.getLastPrice() != null ? price.getLastPrice() : 0.0,
                                                "dayChange", 0.0,
                                                "dayChangePercentage", 0.0))
                                .lastUpdated(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                                .build()).collect(java.util.stream.Collectors.toList());
        }
}
