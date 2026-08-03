package com.am.mcp.tools;

import com.am.portfolio.client.model.PortfolioSummaryV1;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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

    @Test
    void slimHoldings_keepsTopRowsByValue_andTruncates() {
        java.util.List<Map<String, Object>> equity = new java.util.ArrayList<>();
        for (int i = 0; i < 45; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("symbol", "S" + i);
            row.put("name", "Name " + i);
            row.put("quantity", 1);
            row.put("currentValue", (double) i);
            row.put("brokerType", "UPSTOX");
            equity.add(row);
        }
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("lastUpdated", "2026-08-02T10:00:00");
        raw.put("equityHoldings", equity);

        Map<String, Object> slim = PortfolioTools.slimHoldings(raw);
        assertEquals(45, slim.get("count"));
        assertEquals(true, slim.get("truncated"));
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> holdings =
                (java.util.List<Map<String, Object>>) slim.get("holdings");
        assertEquals(20, holdings.size());
        assertEquals("S44", holdings.get(0).get("symbol"));
        assertFalse(holdings.get(0).containsKey("brokerType"));
    }

    @Test
    void slimHoldings_emptyEquity() {
        Map<String, Object> slim = PortfolioTools.slimHoldings(Map.of());
        assertEquals(0, slim.get("count"));
        assertEquals(List.of(), slim.get("holdings"));
    }

    @Test
    void findHoldingBySymbol_caseInsensitive() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("equityHoldings", List.of(
                Map.of("symbol", "Reliance", "currentValue", 10, "brokerType", "UPSTOX")
        ));
        Map<String, Object> match = PortfolioTools.findHoldingBySymbol(raw, "RELIANCE");
        assertEquals("Reliance", match.get("symbol"));
        assertEquals(null, PortfolioTools.findHoldingBySymbol(raw, "TCS"));
    }

    @Test
    void slimHoldings_serializedUnderMaxChars() throws Exception {
        java.util.List<Map<String, Object>> equity = new java.util.ArrayList<>();
        for (int i = 0; i < 120; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("symbol", "SYMBOL" + i);
            row.put("name", "Very Long Company Name Industries Limited " + i);
            row.put("quantity", 1234.567);
            row.put("currentValue", 123456.789 + i);
            row.put("investmentCost", 100000.123);
            row.put("gainLoss", 12345.67);
            row.put("gainLossPercentage", 12.345678);
            row.put("currentPrice", 987.654);
            row.put("averageBuyingPrice", 876.543);
            row.put("portfolioName", "My Long Portfolio Name Growth Account");
            row.put("brokerPortfolios", Map.of("UPSTOX", Map.of("qty", 1)));
            equity.add(row);
        }
        Map<String, Object> slim = PortfolioTools.slimHoldings(Map.of("equityHoldings", equity));
        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(Map.of("ok", true, "data", slim));
        assertTrue(json.length() < 8000, "length=" + json.length());
        assertEquals(20, ((java.util.List<?>) slim.get("holdings")).size());
    }
}
