package com.am.api.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enterprise standard registry for all Authentication and Authorization errors.
 */
@Getter
public enum AuthErrorCode implements ErrorCode {
    
    // 400 Bad Request
    INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "Invalid username or password provided"),
    MISSING_TOKEN(HttpStatus.BAD_REQUEST, "Authentication token is missing from the request"),
    INVALID_TOKEN_FORMAT(HttpStatus.BAD_REQUEST, "The provided token format is invalid"),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "You are not authorized to access this resource"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "The authentication token has expired"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "The authentication token is invalid or corrupted"),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "Your session has expired, please log in again"),

    // 403 Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied: insufficient permissions"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Your account has been locked due to security reasons"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Your account is currently disabled"),

    // 404 Not Found (Auth context)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested user account was not found"),

    // 409 Conflict (Auth context)
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "A user with these credentials already exists"),

    // 500 Internal Server Error
    AUTH_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred during authentication"),
    PROVIDER_UNAVAILABLE(HttpStatus.INTERNAL_SERVER_ERROR, "The identity provider is currently unavailable");

    private final HttpStatus status;
    private final String defaultMessage;

    AuthErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
