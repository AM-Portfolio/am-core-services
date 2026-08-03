package com.am.mcp.tools;

import com.am.portfolio.client.model.PortfolioSummaryV1;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioToolsSlimSummaryTest {

    @Test
    void slimSummary_keepsScalarsAndBrokerNames_dropsHeavyTrees() {
        PortfolioSummaryV1 summary = new PortfolioSummaryV1();
        summary.setCurrentValue(100.0);
        summary.setInvestmentValue(80.0);
        summary.setTotalGainLoss(20.0);
        Map<String, com.am.portfolio.client.model.BrokerPortfolioSummary> brokers = new LinkedHashMap<>();
        brokers.put("ZERODHA", new com.am.portfolio.client.model.BrokerPortfolioSummary());
        brokers.put("UPSTOX", new com.am.portfolio.client.model.BrokerPortfolioSummary());
        summary.setBrokerPortfolios(brokers);
        summary.setMarketCapHoldings(Map.of("LARGE", java.util.List.of()));
        summary.setSectorialHoldings(Map.of("IT", java.util.List.of()));

        Map<String, Object> slim = PortfolioTools.slimSummary(summary);
        assertEquals(100.0, slim.get("currentValue"));
        assertEquals(80.0, slim.get("investmentValue"));
        assertTrue(((java.util.Collection<?>) slim.get("brokers")).contains("ZERODHA"));
        assertFalse(slim.containsKey("marketCapHoldings"));
        assertFalse(slim.containsKey("sectorialHoldings"));
        assertFalse(slim.containsKey("brokerPortfolios"));
    }
}
