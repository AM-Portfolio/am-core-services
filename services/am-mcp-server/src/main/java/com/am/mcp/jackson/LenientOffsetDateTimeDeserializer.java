package com.am.mcp.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Portfolio APIs often emit {@code LocalDateTime} strings (no offset).
 * Generated client models use {@link OffsetDateTime}; accept both.
 */
public final class LenientOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

    @Override
    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDateTime.parse(text).atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return (OffsetDateTime) ctxt.handleWeirdStringValue(
                    OffsetDateTime.class, text, "not a LocalDateTime or OffsetDateTime");
        }
    }
}
