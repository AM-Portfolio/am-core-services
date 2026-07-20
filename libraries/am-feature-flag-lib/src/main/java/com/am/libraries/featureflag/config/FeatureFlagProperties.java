package com.am.libraries.featureflag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "am.feature-flag")
public class FeatureFlagProperties {
    /**
     * Enable or disable feature flagging. If false, starter is bypassed.
     */
    private boolean enabled = true;

    /**
     * GrowthBook API/CDN Host URL (e.g. https://growthbook.asrax.in/gbapi).
     */
    private String apiHost;

    /**
     * GrowthBook Client Key (e.g. sdk-8WmIFrhYLe1AXr8).
     */
    private String clientKey;
}
