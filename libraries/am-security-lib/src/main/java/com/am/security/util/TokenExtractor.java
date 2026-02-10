package com.am.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@UtilityClass
public class TokenExtractor {

    private static final String BEARER_PREFIX = "Bearer";

    /**
     * Extracts all claims from a JWT token.
     * This method parses the token without signature verification for extraction purposes only.
     *
     * @param token The JWT token (with or without 'Bearer ' prefix, case-insensitive)
     * @return Claims object
     */
    public static Claims extractClaims(String token) {
        return extractAllClaimsUnsafe(token);
    }

    public static String extractEmail(String token) {
         return extractClaim(token, "email", String.class);
    }

    public static String extractUserId(String token) {
        // 'sub' is standard for User ID
        Claims claims = extractAllClaimsUnsafe(token);
        return claims.getSubject();
    }
    
    public static String extractUsername(String token) {
        return extractClaim(token, "username", String.class);
    }

    public static <T> T extractClaim(String token, String claimKey, Class<T> type) {
        Claims claims = extractAllClaimsUnsafe(token);
        return claims.get(claimKey, type);
    }
    
    /**
     * Extracts claims without verifying signature. 
     * Useful for services that trust the Gateway to have already verified the token.
     */
    private static Claims extractAllClaimsUnsafe(String token) {
        if (StringUtils.isBlank(token)) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        
        String cleanToken = token.trim();
        
        // Handle "Bearer " prefix case-insensitively
        if (StringUtils.startsWithIgnoreCase(cleanToken, BEARER_PREFIX + " ")) {
            cleanToken = cleanToken.substring(BEARER_PREFIX.length() + 1).trim();
        } else if (StringUtils.startsWithIgnoreCase(cleanToken, BEARER_PREFIX)) {
            // Handle case where space might be missing or different format if needed, 
            // but usually it's "Bearer " + token. 
            // Just protecting against "Bearer" without space if valid in some contexts (unlikely but safe).
             cleanToken = cleanToken.substring(BEARER_PREFIX.length()).trim();
        }
        
        try {
            String[] parts = cleanToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT token format");
            }
            
            // Payload is the second part
            String payload = parts[1];
            
            // Base64 decode
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payloadJson = new String(decoder.decode(payload), StandardCharsets.UTF_8);
            
            // Convert JSON to Claims Map using ObjectMapper (Jackson)
            // Jackson is provided by spring-boot-starter-web or jjwt-jackson
            return new DefaultClaims(
                new com.fasterxml.jackson.databind.ObjectMapper().readValue(payloadJson, Map.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token claims: " + e.getMessage(), e);
        }
    }
}
