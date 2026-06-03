package com.am.security.exception;

import com.am.api.core.exception.AuthErrorCode;
import com.am.api.core.model.ApiError;
import com.am.api.core.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * AM standard handler to format all standard security authorization failures using ApiError.
 */
public class AmAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        AuthErrorCode errorCode = AuthErrorCode.ACCESS_DENIED;
        ApiError apiError = ApiError.from(errorCode, accessDeniedException.getMessage());
        apiError.setInstance(java.net.URI.create(request.getRequestURI()));

        ApiResponse<Void> apiResponse = ApiResponse.error(apiError);
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
}
