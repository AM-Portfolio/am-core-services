package com.am.analysis.dto;

/**
 * Discriminator enum for ActivityItem.type.
 *
 * HOLDING         → A live position in a portfolio (most common).
 *                   Has: symbol, avgBuyingPrice, currentPrice, P&L, WIN/LOSS
 * PORTFOLIO_UPDATE → A portfolio-level recalculation event.
 *                   Has: portfolioId, portfolioName, summary metrics
 * ALERT           → System-generated notification (price target, threshold breach).
 *                   Has: title, description only
 */
public enum ActivityType {
    HOLDING,
    PORTFOLIO_UPDATE,
    ALERT
}
