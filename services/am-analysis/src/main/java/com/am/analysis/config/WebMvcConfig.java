package com.am.analysis.config;

import com.am.kafka.config.Timeframe;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Binds OpenAPI / query wire codes (e.g. {@code 1D}) to {@link Timeframe}.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        registry.addConverter(new Converter<String, Timeframe>() {
            @Override
            public Timeframe convert(@NonNull String source) {
                if (source.isBlank()) {
                    return null;
                }
                return Timeframe.fromCode(source);
            }
        });
    }
}
