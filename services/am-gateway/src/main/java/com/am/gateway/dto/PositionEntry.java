package com.am.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight snapshot of a single equity position held in the gateway's Position Book.
 *
 * Loaded once from the portfolio service when a user subscribes.
 * Contains only the static fields needed for inline P&L math — market prices
 * arrive live via the STOCK_UPDATE Kafka topic and are never stored here.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionEntry {

    /** NSE/BSE ticker symbol (e.g. "RELIANCE", "TCS"). Used as lookup key. */
    private String symbol;

    /** ISIN — used as a secondary lookup key when symbol is absent. */
    private String isin;

    /** Number of shares held. Updated on trade invalidation reload. */
    private Double quantity;

    /** Average buy price per share (cost basis). Static between trades. */
    private Double avgBuyPrice;

    /**
     * Previous trading-day close price.
     * Loaded once per session and used to compute today's gain/loss:
     *   todayGainLoss = quantity × (currentPrice − prevClosePrice)
     */
    private Double prevClosePrice;

    /**
     * Total investment cost = quantity × avgBuyPrice.
     * Cached to avoid recomputation on every price tick.
     */
    private Double investmentValue;
}
