package com.am.kafka.config;

public final class KafkaTopics {
    private KafkaTopics() {
    }

    // Stock Updates
    public static final String STOCK_UPDATE = "am-stock-update";

    // Portfolio Updates
    public static final String PORTFOLIO_UPDATE = "am-portfolio-stream";

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

    // User Watching (emitted by Gateway on subscription)
    public static final String USER_WATCHING = "am-user-watching";

    // Dead Letter Queues (DLQ) - Failed events land here for inspection/retry
    public static final String TRIGGER_CALCULATION_DLQ = "am-trigger-calculation.DLQ";
    public static final String PORTFOLIO_UPDATE_DLQ   = "am-portfolio-stream.DLQ";
    public static final String USER_WATCHING_DLQ      = "am-user-watching.DLQ";
    public static final String TRADE_UPDATE_DLQ       = "am-trade-update.DLQ";
}
