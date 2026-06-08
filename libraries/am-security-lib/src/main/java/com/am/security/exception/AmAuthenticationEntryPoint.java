package com.am.security.exception;

import com.am.api.core.exception.AuthErrorCode;
import com.am.api.core.model.ApiError;
import com.am.api.core.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * AM standard entry point to format all standard security authentication failures using ApiError.
 */
public class AmAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String message = authException.getMessage();
        AuthErrorCode errorCode = AuthErrorCode.UNAUTHORIZED;

        if (message != null && message.toLowerCase().contains("expired")) {
            errorCode = AuthErrorCode.TOKEN_EXPIRED;
        } else if (message != null && (message.toLowerCase().contains("format") || message.toLowerCase().contains("malformed"))) {
            errorCode = AuthErrorCode.INVALID_TOKEN_FORMAT;
        } else if (message != null && message.toLowerCase().contains("invalid")) {
            errorCode = AuthErrorCode.TOKEN_INVALID;
        }

        ApiError apiError = ApiError.from(errorCode, message != null ? message : errorCode.getDefaultMessage());
        apiError.setInstance(java.net.URI.create(request.getRequestURI()));
        
        ApiResponse<Void> apiResponse = ApiResponse.error(apiError);
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
}
