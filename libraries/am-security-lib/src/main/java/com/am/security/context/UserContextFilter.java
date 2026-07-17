package com.am.security.context;

import com.am.api.core.exception.AuthErrorCode;
import com.am.api.core.model.ApiError;
import com.am.api.core.model.ApiResponse;
import com.am.security.util.TokenExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extracts JWT claims into {@link UserContext} and mirrors {@code userId}
 * into SLF4J MDC so access logs / Loki can attribute requests without an
 * {@code X-User-Id} header. Prefers JWT {@code sub} over any prior MDC value.
 *
 * <p>Also stores the id as request attribute {@link #REQUEST_ATTR_USER_ID}
 * because outer servlet filters (request logging) run their {@code finally}
 * after this filter clears MDC.</p>
 */
public class UserContextFilter extends OncePerRequestFilter {

    /** Keep in sync with am-observability-lib {@code MdcKeys.USER_ID}. */
    static final String MDC_USER_ID = "userId";

    /** Survives MDC cleanup for outer request-logging filters. */
    public static final String REQUEST_ATTR_USER_ID = "am.observability.userId";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        boolean putUserId = false;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String userId = TokenExtractor.extractUserId(token);
                String email = TokenExtractor.extractEmail(token);
                String username = TokenExtractor.extractUsername(token);
                java.util.List<String> roles = TokenExtractor.extractRoles(token);
                
                AmUserProfile profile = AmUserProfile.builder()
                        .userId(userId)
                        .email(email)
                        .username(username)
                        .roles(roles)
                        .token(token)
                        .build();
                        
                UserContext.setUserProfile(profile);
                putUserId = putUserIdInMdc(request, userId);
            } catch (Exception e) {
                logger.warn("Failed to extract user context from token: " + e.getMessage());
                
                // Write standard error response immediately since token validation failed
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                
                AuthErrorCode errorCode = AuthErrorCode.TOKEN_INVALID;
                String msg = e.getMessage();
                if (msg != null && msg.toLowerCase().contains("expired")) {
                    errorCode = AuthErrorCode.TOKEN_EXPIRED;
                } else if (msg != null && (msg.toLowerCase().contains("format") || msg.toLowerCase().contains("malformed"))) {
                    errorCode = AuthErrorCode.INVALID_TOKEN_FORMAT;
                }
                
                ApiError apiError = ApiError.from(errorCode, msg != null ? msg : errorCode.getDefaultMessage());
                apiError.setInstance(java.net.URI.create(request.getRequestURI()));
                
                ApiResponse<Void> apiResponse = ApiResponse.error(apiError);
                objectMapper.writeValue(response.getWriter(), apiResponse);
                return; // Stop filter chain execution
            }
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (putUserId) {
                MDC.remove(MDC_USER_ID);
            }
            UserContext.clear(); // Always clean up thread locals!
        }
    }

    static boolean putUserIdInMdc(HttpServletRequest request, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        MDC.put(MDC_USER_ID, userId);
        request.setAttribute(REQUEST_ATTR_USER_ID, userId);
        return true;
    }
}
