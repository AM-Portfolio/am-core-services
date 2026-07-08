package com.am.kafka.config;

public final class KafkaTopics {
    private KafkaTopics() {
    }

    // Stock Updates
    public static final String STOCK_UPDATE = "am-stock-price-update";

    // Previous-close snapshot — emitted daily by Market-Data-Scheduler.
    // Payload: { id, stockName, snapshotDate, previousCloseValues: {1D,1W,1M,3M,6M,1Y,5Y} }
    public static final String PREVIOUS_CLOSE_SNAPSHOT = "am-previous-close-snapshot";

    // Portfolio Updates
    public static final String PORTFOLIO_UPDATE = "am-portfolio-update";
    /** Trigger-calc / stream path from am-portfolio PortfolioCalculationService */
    public static final String PORTFOLIO_STREAM = "am-portfolio-stream";

    // Trade Updates
    public static final String TRADE_UPDATE = "am-trade-update";

    // Analytics Events
    public static final String ANALYTICS_CALCULATION = "am-analytics-calculation";

    // Holding Updates
    public static final String HOLDING_UPDATE = "am-holding-update";

    // Calculation Triggers
    public static final String TRIGGER_CALCULATION = "am-trigger-calculation";

    // Dashboard Updates
    public static final String DASHBOARD_UPDATE = "dashboard-update";
    public static final String DASHBOARD_SUMMARY_UPDATE = "dashboard-summary-update";
    public static final String DASHBOARD_MOVERS_UPDATE = "dashboard-movers-update";
    public static final String DASHBOARD_ACTIVITY_UPDATE = "dashboard-activity-update";
    public static final String DASHBOARD_ALLOCATION_UPDATE = "dashboard-allocation-update";
    public static final String DASHBOARD_HISTORY_UPDATE = "dashboard-history-update";

    // User Watching (emitted by Gateway on subscription)
    public static final String USER_WATCHING = "am-user-watching";

    // Dead Letter Queues (DLQ) - Failed events land here for inspection/retry
    public static final String TRIGGER_CALCULATION_DLQ = "am-trigger-calculation.DLQ";
    public static final String PORTFOLIO_UPDATE_DLQ = "am-portfolio-stream.DLQ";
    public static final String USER_WATCHING_DLQ = "am-user-watching.DLQ";
    public static final String TRADE_UPDATE_DLQ = "am-trade-update.DLQ";
}
