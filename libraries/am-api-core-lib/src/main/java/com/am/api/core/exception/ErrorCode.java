package com.am.api.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Enterprise standard interface for defining strongly typed error codes.
 * All domain-specific error enums must implement this interface.
 */
public interface ErrorCode {
    
    /**
     * @return The HTTP status to return when this error occurs
     */
    HttpStatus getStatus();
    
    /**
     * @return The default human-readable message for this error
     */
    String getDefaultMessage();

    /**
     * @return The name of the error code constant (e.g. "INVALID_CREDENTIALS")
     */
    String name();
}
