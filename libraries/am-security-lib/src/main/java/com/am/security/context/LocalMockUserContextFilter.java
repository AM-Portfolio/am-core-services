package com.am.security.context;

import com.am.security.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter that injects a mock user profile into the UserContext and Spring SecurityContextHolder
 * when local mocking is enabled.
 */
public class LocalMockUserContextFilter extends OncePerRequestFilter {

    private final SecurityProperties.LocalMockProperties mockProps;

    public LocalMockUserContextFilter(SecurityProperties.LocalMockProperties mockProps) {
        this.mockProps = mockProps;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        boolean putUserId = false;
            
        // If real security (like a real JWT filter) already populated the context, do not override
        if (UserContext.getUserProfile() == null) {
            AmUserProfile mockProfile = AmUserProfile.builder()
                    .userId(mockProps.getUserId())
                    .username(mockProps.getUsername())
                    .email(mockProps.getEmail())
                    .roles(mockProps.getRoles())
                    .build();
            
            UserContext.setUserProfile(mockProfile);
            putUserId = UserContextFilter.putUserIdInMdc(request, mockProps.getUserId());

            // Populate Spring Security Context so @PreAuthorize works
            if (SecurityContextHolder.getContext().getAuthentication() == null && mockProps.getRoles() != null) {
                List<SimpleGrantedAuthority> authorities = mockProps.getRoles().stream()
                        .map(role -> {
                            if (!role.startsWith("ROLE_")) {
                                return new SimpleGrantedAuthority("ROLE_" + role);
                            }
                            return new SimpleGrantedAuthority(role);
                        })
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth = 
                        new UsernamePasswordAuthenticationToken(mockProps.getUsername(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (putUserId) {
                MDC.remove(UserContextFilter.MDC_USER_ID);
            }
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
