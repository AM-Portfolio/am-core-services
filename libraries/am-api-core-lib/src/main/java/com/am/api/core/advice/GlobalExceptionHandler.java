package com.am.api.core.advice;

import org.springframework.core.Ordered;
import org.springframework.beans.factory.annotation.Value;

import com.am.api.core.exception.BaseDomainException;
import com.am.api.core.exception.CommonErrorCode;
import com.am.api.core.model.ApiError;
import com.am.api.core.model.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler implements Ordered {

    @Value("${am.api.core.exception-handler.order:" + Ordered.LOWEST_PRECEDENCE + "}")
    private int order;

    @Override
    public int getOrder() {
        return this.order;
    }

    @ExceptionHandler(BaseDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(BaseDomainException ex) {
        log.warn("Domain exception occurred: {}", ex.getMessage());
        
        ApiError apiError = ApiError.from(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error(apiError), ex.getErrorCode().getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation failed");
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError apiError = ApiError.from(status, "Input validation failed", CommonErrorCode.INVALID_INPUT.name());
        
        List<String> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.add(error.getField() + ": " + error.getDefaultMessage());
        }
        apiError.setProperty("details", details);

        return new ResponseEntity<>(ApiResponse.error(apiError), status);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Constraint violation occurred");
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError apiError = ApiError.from(status, "Constraint validation failed", CommonErrorCode.INVALID_INPUT.name());
        
        List<String> details = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation -> {
            details.add(violation.getPropertyPath().toString() + ": " + violation.getMessage());
        });
        apiError.setProperty("details", details);

        return new ResponseEntity<>(ApiResponse.error(apiError), status);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        log.warn("Binding validation failed");
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError apiError = ApiError.from(status, "Request binding failed", CommonErrorCode.INVALID_INPUT.name());
        
        List<String> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.add(error.getField() + ": " + error.getDefaultMessage());
        }
        apiError.setProperty("details", details);

        return new ResponseEntity<>(ApiResponse.error(apiError), status);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request received");
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError apiError = ApiError.from(status, "Malformed JSON request body", CommonErrorCode.MALFORMED_JSON.name());

        return new ResponseEntity<>(ApiResponse.error(apiError), status);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Method argument type mismatch: {}", ex.getName());
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = String.format("Parameter '%s' should be of type %s", ex.getName(), 
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        ApiError apiError = ApiError.from(status, message, CommonErrorCode.TYPE_MISMATCH.name());

        return new ResponseEntity<>(ApiResponse.error(apiError), status);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.warn("Missing servlet request parameter: {}", ex.getParameterName());
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError apiError = ApiError.from(status, ex.getMessage(), CommonErrorCode.MISSING_PARAMETER.name());

        return new ResponseEntity<>(ApiResponse.error(apiError), status);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        log.warn("Missing request header: {}", ex.getHeaderName());
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError apiError = ApiError.from(status, ex.getMessage(), CommonErrorCode.MISSING_PARAMETER.name());

        return new ResponseEntity<>(ApiResponse.error(apiError), status);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMethod());
        
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        ApiError apiError = ApiError.from(status, ex.getMessage(), CommonErrorCode.METHOD_NOT_ALLOWED.name());

        return new ResponseEntity<>(ApiResponse.error(apiError), status);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        log.warn("HTTP media type not supported: {}", ex.getContentType());
        
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        ApiError apiError = ApiError.from(status, ex.getMessage(), CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.name());

        return new ResponseEntity<>(ApiResponse.error(apiError), status);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getResourcePath());
        
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiError apiError = ApiError.from(status, ex.getMessage(), CommonErrorCode.RESOURCE_NOT_FOUND.name());

        return new ResponseEntity<>(ApiResponse.error(apiError), status);
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleRestClientResponseException(RestClientResponseException ex) {
        log.error("RestClient call failed with status: {}", ex.getStatusCode());
        
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        ApiError apiError = ApiError.from(status, "An error occurred while contacting an external integration service.", CommonErrorCode.DOWNSTREAM_SERVICE_ERROR.name());
        apiError.setProperty("statusCode", ex.getStatusCode().value());

        return new ResponseEntity<>(ApiResponse.error(apiError), status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception ex) {
        // Check database/persistence exceptions dynamically to avoid classloading errors 
        // in services (like gateways) that do not package spring-tx or spring-orm.
        String exClassName = ex.getClass().getName();
        
        if (exClassName.equals("org.springframework.dao.DataIntegrityViolationException")) {
            log.error("Database integrity violation occurred", ex);
            HttpStatus status = HttpStatus.CONFLICT;
            ApiError apiError = ApiError.from(status, "Database conflict: duplicate entry or constraint violation", CommonErrorCode.CONFLICTING_STATE.name());
            return new ResponseEntity<>(ApiResponse.error(apiError), status);
        }
        
        if (exClassName.equals("org.springframework.orm.ObjectOptimisticLockingFailureException")) {
            log.error("Database optimistic locking failure occurred", ex);
            HttpStatus status = HttpStatus.CONFLICT;
            ApiError apiError = ApiError.from(status, "Database conflict: resource was concurrently modified", CommonErrorCode.CONFLICTING_STATE.name());
            return new ResponseEntity<>(ApiResponse.error(apiError), status);
        }
        
        if (isInstanceOf(ex, "org.springframework.dao.DataAccessException")) {
            log.error("Database access error occurred", ex);
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            ApiError apiError = ApiError.from(status, "A database error occurred. Please try again later.", CommonErrorCode.DATABASE_ERROR.name());
            return new ResponseEntity<>(ApiResponse.error(apiError), status);
        }
        
        log.error("Unhandled exception occurred", ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiError apiError = ApiError.from(status, "An unexpected error occurred. Please try again later.", CommonErrorCode.INTERNAL_SERVER_ERROR.name());

        return new ResponseEntity<>(ApiResponse.error(apiError), status);
    }

    private boolean isInstanceOf(Throwable ex, String className) {
        Class<?> clazz = ex.getClass();
        while (clazz != null) {
            if (clazz.getName().equals(className)) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }
}
