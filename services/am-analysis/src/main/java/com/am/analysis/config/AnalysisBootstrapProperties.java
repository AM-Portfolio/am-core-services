package com.am.analysis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "am.analysis.bootstrap")
@Data
public class AnalysisBootstrapProperties {

    /** When true, empty portfolio reads emit am-trigger-calculation (debounced). */
    private boolean enabled = true;

    private long debounceMs = 60_000L;
}
