package com.am.mcp.jackson;

import com.am.portfolio.client.model.EquityBrokerHolding;
import com.am.portfolio.client.model.PortfolioSummaryV1;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LenientOffsetDateTimeDeserializerTest {

    private final ObjectMapper mapper = PortfolioObjectMappers.create();

    @Test
    void parsesLocalDateTimeWithoutOffsetAsUtc() throws Exception {
        String json = """
                {"lastUpdated":"2026-08-03T22:19:27.974212146","currentValue":1.0}
                """;
        PortfolioSummaryV1 summary = mapper.readValue(json, PortfolioSummaryV1.class);
        assertNotNull(summary.getLastUpdated());
        assertEquals(
                OffsetDateTime.of(2026, 8, 3, 22, 19, 27, 974212146, ZoneOffset.UTC),
                summary.getLastUpdated());
        assertEquals(1.0, summary.getCurrentValue());
    }

    @Test
    void stillParsesOffsetDateTime() throws Exception {
        String json = """
                {"lastUpdated":"2026-08-03T22:19:27.974Z"}
                """;
        PortfolioSummaryV1 summary = mapper.readValue(json, PortfolioSummaryV1.class);
        assertEquals(ZoneOffset.UTC, summary.getLastUpdated().getOffset());
    }

    @Test
    void unknownBrokerTypeBecomesNull() throws Exception {
        String json = """
                {"brokerType":"UPSTOX","quantity":1.0}
                """;
        EquityBrokerHolding holding = mapper.readValue(json, EquityBrokerHolding.class);
        assertNull(holding.getBrokerType());
        assertEquals(1.0, holding.getQuantity());
    }
}
