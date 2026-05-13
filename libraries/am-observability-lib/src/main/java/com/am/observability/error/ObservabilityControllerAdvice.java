package com.am.observability.error;

import com.am.common.dto.ApiResponse;
import com.am.common.dto.ErrorDetails;
import com.am.observability.mdc.MdcKeys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler that:
 * <ul>
 *     <li>Logs every uncaught exception once, with {@code traceId} in MDC.</li>
 *     <li>Returns the project-wide {@link ApiResponse} error shape from
 *         {@code am-common-lib} so all services produce consistent error JSON.</li>
 *     <li>Respects {@link ResponseStatus} annotations on custom exceptions.</li>
 * </ul>
 *
 * <p>This is why {@code e.printStackTrace()} is banned in service code: it
 * bypasses MDC enrichment and produces error responses with no shape.</p>
 */
@Slf4j
@RestControllerAdvice
public class ObservabilityControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", details, ex);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception ex) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", safeMessage(ex), null, ex);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", safeMessage(ex), null, ex);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResponse<Object>> handleAny(Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ResponseStatus rs = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
        if (rs != null) {
            status = rs.code();
        }
        String code = status.is5xxServerError() ? "INTERNAL_ERROR" : "REQUEST_FAILED";
        return respond(status, code, safeMessage(ex), null, ex);
    }

    private ResponseEntity<ApiResponse<Object>> respond(HttpStatus status,
                                                        String code,
                                                        String message,
                                                        List<String> details,
                                                        Throwable cause) {
        String traceId = MDC.get(MdcKeys.TRACE_ID);
        String flowStep = MDC.get(MdcKeys.FLOW_STEP);
        String requestPath = MDC.get(MdcKeys.REQUEST_PATH);

        if (status.is5xxServerError()) {
            log.error("Unhandled {} on {} step={} traceId={} cause={}",
                    status, requestPath, flowStep, traceId,
                    cause == null ? "n/a" : cause.getClass().getSimpleName(),
                    cause);
        } else {
            log.warn("Request rejected {} on {} step={} traceId={} cause={}: {}",
                    status, requestPath, flowStep, traceId,
                    cause == null ? "n/a" : cause.getClass().getSimpleName(),
                    message);
        }

        ErrorDetails error = ErrorDetails.builder()
                .code(code)
                .message(message)
                .details(details == null ? Collections.emptyList() : details)
                .build();
        ApiResponse<Object> body = ApiResponse.error(message, error);
        return ResponseEntity.status(status).body(body);
    }

    private static String safeMessage(Throwable ex) {
        if (ex == null || ex.getMessage() == null) {
            return "Unexpected error";
        }
        String msg = ex.getMessage();
        return msg.length() > 256 ? msg.substring(0, 256) + "..." : msg;
    }
}
