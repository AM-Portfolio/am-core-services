package com.am.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Per-user in-memory position book held inside the gateway process.
 *
 * Lifecycle:
 *   - Created: when user sends /portfolio/subscribe
 *   - Updated: when a POSITION_INVALIDATE Kafka event arrives after a trade
 *   - Evicted: when user disconnects or TTL expires
 *
 * The bySymbol map is the primary price-tick lookup path:
 *   symbol → PositionEntry
 * This allows O(1) lookup on every STOCK_UPDATE without any Redis or DB call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionBook {

    /** Keycloak user ID. */
    private String userId;

    /**
     * The specific portfolio ID this book covers, or "ALL" if the user
     * subscribed without specifying a particular portfolio.
     */
    private String portfolioId;

    /**
     * Primary index: NSE/BSE symbol → PositionEntry.
     * Built at load time; O(1) lookup on every price tick.
     */
    private Map<String, PositionEntry> bySymbol;

    /**
     * Secondary index: ISIN → symbol. Used to resolve price events
     * that carry ISIN but not symbol (e.g. BSE feed).
     */
    private Map<String, String> isinToSymbol;

    /**
     * Wall-clock time when this book was last loaded from the portfolio service.
     * Used for staleness detection and logging.
     */
    private Instant loadedAt;
}
