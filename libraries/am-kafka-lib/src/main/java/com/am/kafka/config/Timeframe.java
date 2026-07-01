package com.am.kafka.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

/**
 * Canonical analysis / market-data window identifiers.
 * <p>
 * Wire format (e.g. {@code "1D"}) is used as:
 * <ul>
 *   <li>Redis hash field in {@code prev-close:{symbol}}</li>
 *   <li>REST {@code timeFrame} query param on dashboard endpoints</li>
 *   <li>Kafka {@link com.am.kafka.schema.PreviousCloseSnapshot} map keys</li>
 * </ul>
 * Superset of {@code com.am.market.domain.enums.TimeFrame} (1D, 1W, 1M only).
 */
public enum Timeframe {

    ONE_DAY("1D"),
    ONE_WEEK("1W"),
    ONE_MONTH("1M"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    ONE_YEAR("1Y"),
    FIVE_YEARS("5Y");

    private final String code;

    Timeframe(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    /** Whether this window is updated intraday (live tick append). */
    public boolean isIntraday() {
        return this == ONE_DAY;
    }

    @JsonCreator
    public static Timeframe fromCode(String value) {
        return tryFromCode(value)
                .orElseThrow(() -> new IllegalArgumentException("Unknown timeframe: " + value));
    }

    public static Optional<Timeframe> tryFromCode(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(tf -> tf.code.equalsIgnoreCase(value.trim())
                        || tf.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
