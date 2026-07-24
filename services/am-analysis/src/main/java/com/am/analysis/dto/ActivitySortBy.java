package com.am.analysis.dto;

/**
 * Sort field for recent activity. Wire names match historical query values.
 */
public enum ActivitySortBy {
    TIMESTAMP,
    PROFIT_LOSS,
    PROFIT_LOSS_ASC,
    DAY_CHANGE,
    DAY_CHANGE_PERCENT,
    CURRENT_VALUE,
    SYMBOL,
    PROFIT_LOSS_PERCENT,
    QUANTITY
}
