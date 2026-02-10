package com.am.gateway.security;

import org.springframework.stereotype.Component;

@Component
public class JwtUtilMock {

    public boolean validateToken(String token) {
        // Mock validation: accept any non-empty token
        // SecurityConfig already strips "Bearer " prefix, so we just check the token
        // body
        return token != null && !token.isEmpty();
    }

    public String getUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payload = new String(java.util.Base64.getDecoder().decode(parts[1]));
            // Simple string search to avoid Jackson dependency issues if not present,
            // though ObjectMapper is available
            // Looking for "sub":"..." or "userId":"..."
            // "id":"..." is also possible

            // Let's use Jackson since we have it in the project
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(payload);

            if (node.has("sub"))
                return node.get("sub").asText();
            if (node.has("userId"))
                return node.get("userId").asText();
            if (node.has("id"))
                return node.get("id").asText();

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
