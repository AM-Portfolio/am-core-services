package com.am.api.core.model;

import com.am.api.core.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Enterprise standard representation of API errors, extending Spring Boot 3's native ProblemDetail (RFC 7807).
 */
public class ApiError extends ProblemDetail {

    protected ApiError() {
        super();
    }

    public static ApiError from(ErrorCode errorCode) {
        ApiError error = new ApiError();
        error.setStatus(errorCode.getStatus().value());
        error.setTitle(errorCode.getStatus().getReasonPhrase());
        error.setDetail(errorCode.getDefaultMessage());
        error.setProperty("code", errorCode.name());
        return error;
    }

    public static ApiError from(ErrorCode errorCode, String customMessage) {
        ApiError error = new ApiError();
        error.setStatus(errorCode.getStatus().value());
        error.setTitle(errorCode.getStatus().getReasonPhrase());
        error.setDetail(customMessage);
        error.setProperty("code", errorCode.name());
        return error;
    }

    public static ApiError from(HttpStatus status, String message, String code) {
        ApiError error = new ApiError();
        error.setStatus(status.value());
        error.setTitle(status.getReasonPhrase());
        error.setDetail(message);
        error.setProperty("code", code);
        return error;
    }
}
