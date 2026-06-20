package com.am.analysis.service;

import com.am.analysis.adapter.mapper.AnalysisEventMapper;
import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.MarketStats;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.config.PortfolioStreamingProperties;
import com.am.kafka.config.AnalysisEntityKeys;
import com.am.kafka.config.InterestRegistryKeys;
import com.am.kafka.config.KafkaTopics;
import com.am.kafka.service.GainLossCalculator;
import com.am.kafka.service.InterestRegistryService;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.portfolio.domain.events.PortfolioUpdateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Computes portfolio snapshots from Mongo {@link AnalysisEntity} and publishes
 * to {@link KafkaTopics#PORTFOLIO_STREAM} for gateway WebSocket relay.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioStreamingService {

    private final AnalysisRepository analysisRepository;
    private final AnalysisEventMapper analysisEventMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final FlowLogger flowLogger;
    private final InterestRegistryService interestRegistry;
    private final PortfolioStreamingProperties properties;

    /**
     * Load entity, optionally overlay live tick prices, map to event, publish Kafka.
     *
     * @param livePrices symbol → lastPrice from STOCK_UPDATE batch; may be null or empty
     */
    public void publishPortfolioStream(String userId, String portfolioId, Map<String, Double> livePrices) {
        if (!properties.isEnabled()) {
            flowLogger.step("analysis.portfolio_stream.disabled", "userId", userId);
            return;
        }
        if (userId == null || userId.isBlank()) {
            return;
        }

        String entityId = AnalysisEntityKeys.portfolioEntityId(portfolioId, userId);
        Optional<AnalysisEntity> entityOpt = analysisRepository.findById(entityId);
        if (entityOpt.isEmpty()) {
            flowLogger.step("analysis.portfolio_stream.entity_not_found",
                    "userId", userId,
                    "portfolioId", portfolioId != null ? portfolioId : "GLOBAL",
                    "entityId", entityId);
            return;
        }

        AnalysisEntity entity = entityOpt.get();
        if (!userId.equals(entity.getOwnerId())) {
            flowLogger.step("analysis.portfolio_stream.owner_mismatch",
                    "userId", userId, "entityId", entityId);
            return;
        }

        applyLivePrices(entity, livePrices);
        PortfolioUpdateEvent event = analysisEventMapper.mapEntityToPortfolioUpdateEvent(entity);
        if (event == null) {
            return;
        }

        publishEvent(event, userId, portfolioId);
    }

    /**
     * After adapter ingest: push stream only if this user is actively watching the matching portfolio channel.
     */
    public void publishIfUserWatching(AnalysisEntity entity) {
        if (!properties.isEnabled() || entity == null
                || entity.getType() != AnalysisEntityType.PORTFOLIO
                || entity.getOwnerId() == null) {
            return;
        }

        String userId = entity.getOwnerId();
        Optional<String> watchedOpt = interestRegistry.getWatchedPortfolio(userId);
        if (watchedOpt.isEmpty()) {
            return;
        }

        String watched = watchedOpt.get();
        if (InterestRegistryKeys.isDashboardChannel(watched)) {
            return;
        }

        if (!matchesWatchTarget(watched, entity.getSourceId())) {
            return;
        }

        publishPortfolioStream(userId, watched, null);
    }

    static boolean matchesWatchTarget(String watchedPortfolioId, String entitySourceId) {
        if (watchedPortfolioId == null || watchedPortfolioId.isBlank()) {
            return AnalysisEntityKeys.isGlobalSourceId(entitySourceId);
        }
        if (AnalysisEntityKeys.isGlobalSourceId(watchedPortfolioId)
                || "ALL".equalsIgnoreCase(watchedPortfolioId)) {
            return AnalysisEntityKeys.isGlobalSourceId(entitySourceId);
        }
        return watchedPortfolioId.equals(entitySourceId);
    }

    private void publishEvent(PortfolioUpdateEvent event, String userId, String portfolioId) {
        try (FlowSpan span = flowLogger.start("analysis.kafka.publish.portfolio_stream",
                "userId", userId,
                "portfolioId", portfolioId != null ? portfolioId : "GLOBAL",
                "topic", KafkaTopics.PORTFOLIO_STREAM)) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(KafkaTopics.PORTFOLIO_STREAM, userId, payload);
                int holdings = event.getEquities() != null ? event.getEquities().size() : 0;
                flowLogger.complete(span, "payload_bytes", payload.length(), "holdings", holdings);
            } catch (JsonProcessingException e) {
                flowLogger.fail(span, e);
            }
        }
    }

    void applyLivePrices(AnalysisEntity entity, Map<String, Double> livePrices) {
        if (entity.getHoldings() == null || entity.getHoldings().isEmpty()) {
            return;
        }

        double totalValue = 0.0;
        double totalInvestment = 0.0;
        double totalGainLoss = 0.0;
        double todayGainLoss = 0.0;

        for (AnalysisHolding holding : entity.getHoldings()) {
            recalculateHolding(holding, livePrices);

            InvestmentStats inv = holding.getInvestment();
            MarketStats market = holding.getMarket();
            if (inv == null) {
                continue;
            }

            double currentValue = inv.getCurrentValue() != null ? inv.getCurrentValue() : 0.0;
            double investmentValue = inv.getInvestmentValue() != null ? inv.getInvestmentValue() : 0.0;
            double pl = inv.getProfitLoss() != null ? inv.getProfitLoss() : 0.0;
            double dayPl = market != null && market.getDayChange() != null ? market.getDayChange() : 0.0;

            totalValue += currentValue;
            totalInvestment += investmentValue;
            totalGainLoss += pl;
            todayGainLoss += dayPl;
        }

        double totalGlPct = GainLossCalculator.gainLossPercent(totalGainLoss, totalInvestment);
        double todayGlPct = GainLossCalculator.gainLossPercent(todayGainLoss, totalInvestment);

        PerformanceSummary perf = entity.getPerformance();
        if (perf == null) {
            perf = new PerformanceSummary();
            entity.setPerformance(perf);
        }
        perf.setTotalValue(totalValue);
        perf.setTotalInvestment(totalInvestment);
        perf.setTotalGainLoss(totalGainLoss);
        perf.setTotalGainLossPercentage(totalGlPct);
        perf.setDayChange(todayGainLoss);
        perf.setDayChangePercentage(todayGlPct);
        entity.setLastUpdated(LocalDateTime.now());
    }

    private void recalculateHolding(AnalysisHolding holding, Map<String, Double> livePrices) {
        if (holding.getIdentity() == null || holding.getInvestment() == null) {
            return;
        }

        String symbol = holding.getIdentity().getSymbol();
        InvestmentStats inv = holding.getInvestment();
        MarketStats market = holding.getMarket();
        if (market == null) {
            market = new MarketStats();
            holding.setMarket(market);
        }

        double qty = inv.getQuantity() != null ? inv.getQuantity() : 0.0;
        double avgBuy = inv.getAveragePrice() != null ? inv.getAveragePrice() : 0.0;
        double investmentValue = inv.getInvestmentValue() != null ? inv.getInvestmentValue() : 0.0;

        double price = market.getCurrentPrice() != null ? market.getCurrentPrice() : 0.0;
        if (livePrices != null && symbol != null && livePrices.containsKey(symbol)) {
            price = livePrices.get(symbol);
            market.setCurrentPrice(price);
        }

        double prevClose = market.getPreviousClose() != null ? market.getPreviousClose() : price;
        double currentValue = GainLossCalculator.currentValue(qty, price);
        double profitLoss = GainLossCalculator.totalGainLoss(qty, price, avgBuy);
        double dayChange = GainLossCalculator.todayGainLoss(qty, price, prevClose);

        inv.setCurrentValue(currentValue);
        inv.setProfitLoss(profitLoss);
        inv.setProfitLossPercentage(GainLossCalculator.gainLossPercent(profitLoss, investmentValue));
        inv.setValue(currentValue);

        market.setDayChange(dayChange);
        market.setDayChangePercentage(GainLossCalculator.gainLossPercent(dayChange, investmentValue));
    }
}
