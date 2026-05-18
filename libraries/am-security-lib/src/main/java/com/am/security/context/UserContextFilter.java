package com.am.security.context;

import com.am.security.util.TokenExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class UserContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String userId = TokenExtractor.extractUserId(token);
                String email = TokenExtractor.extractEmail(token);
                
                UserContext.setUserId(userId);
                UserContext.setEmail(email);
                UserContext.setToken(token);
            } catch (Exception e) {
                // Log exception but let request proceed - downstream filters/security can block if needed
                logger.warn("Failed to extract user context from token: " + e.getMessage());
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear(); // Always clean up thread locals!
        }
    }
}
