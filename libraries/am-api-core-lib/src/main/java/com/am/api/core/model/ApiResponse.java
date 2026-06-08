package com.am.api.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private String status;
    private T data;
    private ApiError error;
    private Map<String, Object> meta;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .data(data)
                .meta(buildMeta())
                .build();
    }

    public static <T> ApiResponse<T> error(ApiError apiError) {
        return ApiResponse.<T>builder()
                .status("ERROR")
                .error(apiError)
                .meta(buildMeta())
                .build();
    }

    private static Map<String, Object> buildMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("timestamp", Instant.now().toString());
        
        // Propagate traceId from MDC if available
        String traceId = MDC.get("trace_id");
        if (traceId != null) {
            meta.put("traceId", traceId);
        } else {
            traceId = MDC.get("traceId");
            if (traceId != null) {
                meta.put("traceId", traceId);
            }
        }
        return meta;
    }
}
