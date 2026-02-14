package com.am.market.domain.enums;

public enum TimeFrame {
    DAY("1D"),
    WEEK("1W"),
    MONTH("1M");

    private final String value;

    TimeFrame(String value) {
        this.value = value;
    }

    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() {
        return value;
    }

    @com.fasterxml.jackson.annotation.JsonCreator
    public static TimeFrame fromValue(String value) {
        for (TimeFrame tf : values()) {
            if (tf.value.equalsIgnoreCase(value) || tf.name().equalsIgnoreCase(value)) {
                return tf;
            }
        }
        throw new IllegalArgumentException("Unknown TimeFrame value: " + value);
    }
}
