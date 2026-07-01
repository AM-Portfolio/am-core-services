package com.am.analysis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "am.analysis.portfolio.streaming")
@Data
public class PortfolioStreamingProperties {

    /** When true, am-analysis publishes live portfolio updates to am-portfolio-stream. */
    private boolean enabled = true;

    /** When true, keep emitting am-trigger-calculation for rollback during rollout. */
    private boolean legacyTriggerCalc = false;
}
