package com.am.security.config;

import com.am.security.context.LocalMockUserContextFilter;
import com.am.security.exception.AmAccessDeniedHandler;
import com.am.security.exception.AmAuthenticationEntryPoint;
import com.am.security.util.TokenExtractor;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@PropertySource(value = "classpath:application-security.yml", ignoreResourceNotFound = true)
public class SecurityAutoConfiguration {

    private final SecurityProperties properties;

    public SecurityAutoConfiguration(SecurityProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(prefix = "am.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, org.springframework.beans.factory.ObjectProvider<JwtDecoder> jwtDecoderProvider) throws Exception {
        
        boolean isMockEnabled = properties.getLocalMock() != null && properties.getLocalMock().isEnabled();

        AmAuthenticationEntryPoint entryPoint = new AmAuthenticationEntryPoint();
        AmAccessDeniedHandler accessDeniedHandler = new AmAccessDeniedHandler();

        http
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(authorize -> {
                // Configure public paths
                if (properties.getPublicPaths() != null && !properties.getPublicPaths().isEmpty()) {
                    authorize.requestMatchers(properties.getPublicPaths().toArray(new String[0])).permitAll();
                }

                // Configure proxy-specific paths (requiring auth)
                if (properties.getProxyPaths() != null && !properties.getProxyPaths().isEmpty()) {
                    authorize.requestMatchers(properties.getProxyPaths().toArray(new String[0])).authenticated();
                }

                // Configure standard protected paths
                if (properties.getProtectedPaths() != null && !properties.getProtectedPaths().isEmpty()) {
                    authorize.requestMatchers(properties.getProtectedPaths().toArray(new String[0])).authenticated();
                }

                // Default fallback
                authorize.anyRequest().authenticated();
            });

        if (isMockEnabled) {
            http.addFilterBefore(new LocalMockUserContextFilter(properties.getLocalMock()), UsernamePasswordAuthenticationFilter.class);
        } else {
            JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
            if (jwtDecoder != null) {
                http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.decoder(jwtDecoder))
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                );
            }
        }

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "am.security", name = "local-mock.enabled", havingValue = "false", matchIfMissing = true)
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = properties.getOidc() != null ? properties.getOidc().getJwkSetUri() : null;
        String issuerUri = properties.getOidc() != null ? properties.getOidc().getIssuerUri() : null;

        if (StringUtils.isNotBlank(jwkSetUri)) {
            // Secure path: stateless verification of signature via JWKS from am-identity
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } else if (StringUtils.isNotBlank(issuerUri)) {
            // Secure path: auto-discovery from issuer URI
            return JwtDecoders.fromIssuerLocation(issuerUri);
        } else {
            // Unsafe / gateway-trusted path: parse claims directly without signature check
            return new JwtDecoder() {
                @Override
                public Jwt decode(String token) {
                    Claims claims = TokenExtractor.extractClaims(token);
                    Map<String, Object> headers = new HashMap<>();
                    headers.put("alg", "none");

                    Map<String, Object> jwtClaims = new HashMap<>(claims);
                    Instant issuedAt = claims.getIssuedAt() != null ? claims.getIssuedAt().toInstant() : Instant.now();
                    Instant expiresAt = claims.getExpiration() != null ? claims.getExpiration().toInstant() : Instant.now().plusSeconds(3600);

                    return new Jwt(
                        token,
                        issuedAt,
                        expiresAt,
                        headers,
                        jwtClaims
                    );
                }
            };
        }
    }
}
