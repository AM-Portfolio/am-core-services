package com.am.api.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Global registry for common system, database, and connection errors.
 */
@Getter
public enum CommonErrorCode implements ErrorCode {
    
    // Client Errors (4xx)
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "The request payload contains invalid values"),
    MALFORMED_JSON(HttpStatus.BAD_REQUEST, "The request body could not be parsed"),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "The request parameter type does not match the expected type"),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "A required request parameter is missing"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "The requested HTTP method is not supported for this endpoint"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The request Content-Type is not supported"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource could not be found"),
    CONFLICTING_STATE(HttpStatus.CONFLICT, "The request conflicts with the current state of the resource"),

    // Server Errors (5xx)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "A database access or constraint error occurred"),
    DOWNSTREAM_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "An error occurred while calling a downstream service"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The service is temporarily unavailable");

    private final HttpStatus status;
    private final String defaultMessage;

    CommonErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
