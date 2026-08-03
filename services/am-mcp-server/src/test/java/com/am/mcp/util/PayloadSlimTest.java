package com.am.mcp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadSlimTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapList_truncatesAndWhitelists() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("symbol", "S" + i);
            row.put("heavy", "x".repeat(200));
            rows.add(row);
        }
        Map<String, Object> slim = PayloadSlim.mapList(rows, "items", 20, "symbol");
        assertEquals(50, slim.get("count"));
        assertEquals(true, slim.get("truncated"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> kept = (List<Map<String, Object>>) slim.get("items");
        assertEquals(20, kept.size());
        assertFalse(kept.get(0).containsKey("heavy"));
    }

    @Test
    void lastN_keepsTail() {
        List<Map<String, Object>> bars = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            bars.add(Map.of("close", i, "noise", "n"));
        }
        Map<String, Object> slim = PayloadSlim.lastN(bars, "bars", 60, "close");
        assertEquals(100, slim.get("count"));
        assertEquals(true, slim.get("truncated"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> kept = (List<Map<String, Object>>) slim.get("bars");
        assertEquals(60, kept.size());
        assertEquals(99, kept.get(59).get("close"));
    }

    @Test
    void slimNestedListPayload_underMaxChars() throws Exception {
        List<Map<String, Object>> content = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("symbol", "SYM" + i);
            row.put("type", "BUY");
            row.put("quantity", 10);
            row.put("price", 100.5);
            row.put("blob", "y".repeat(500));
            content.add(row);
        }
        Map<String, Object> raw = Map.of("content", content, "totalElements", 200);
        Map<String, Object> slim = PayloadSlim.slimNestedListPayload(
                raw, PayloadSlim.TRADE_HARD_MAX, "symbol", "type", "quantity", "price");
        String json = mapper.writeValueAsString(Map.of("ok", true, "data", slim));
        assertTrue(json.length() < 8000, "length=" + json.length());
        assertEquals(200, slim.get("count"));
    }

    @Test
    void clampLimit_respectsHardMax() {
        assertEquals(20, PayloadSlim.clampLimit(null, 20, 40));
        assertEquals(40, PayloadSlim.clampLimit(100, 20, 40));
        assertEquals(10, PayloadSlim.clampLimit(10, 20, 40));
    }
}
