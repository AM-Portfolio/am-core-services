package com.am.mcp.util;

import com.am.mcp.config.AmMcpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseHelperTest {

    private ResponseHelper helper;

    @BeforeEach
    void setUp() {
        AmMcpProperties props = new AmMcpProperties();
        props.getMcp().setMaxResponseChars(200);
        helper = new ResponseHelper(new ObjectMapper(), props);
    }

    @Test
    void successEnvelopeHasOkAndData() {
        String json = helper.toJson(Map.of("hello", "world"));
        assertTrue(json.contains("\"ok\":true"));
        assertTrue(json.contains("\"data\""));
        assertTrue(json.contains("hello"));
    }

    @Test
    void failureEnvelopeHasOkFalse() {
        String json = helper.failure("t", "CODE", "boom", true, "hint");
        assertTrue(json.contains("\"ok\":false"));
        assertTrue(json.contains("\"error\":\"CODE\""));
        assertFalse(json.contains("\"ok\":true"));
    }

    @Test
    void truncateReturnsValidJsonFailureNotSpliced() {
        String big = helper.toJson(Map.of("blob", "x".repeat(500)));
        assertTrue(big.startsWith("{"));
        assertTrue(big.contains("RESPONSE_TOO_LARGE"));
        assertFalse(big.endsWith("]\""));
    }
}
