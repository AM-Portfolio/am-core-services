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
}
