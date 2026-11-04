package com.am.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketConfigExtractTokenTest {

    @Test
    void extractToken_fromAuthorizationBearer() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer jwt-from-header");
        assertEquals("jwt-from-header", WebSocketConfig.extractToken(accessor));
    }

    @Test
    void extractToken_fromAccessTokenNativeHeader() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("access_token", "jwt-native-access");
        assertEquals("jwt-native-access", WebSocketConfig.extractToken(accessor));
    }

    @Test
    void extractToken_fromTokenNativeHeader() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("token", "jwt-native-token");
        assertEquals("jwt-native-token", WebSocketConfig.extractToken(accessor));
    }

    @Test
    void extractToken_fromHandshakeSessionAttributeAccessToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("access_token", "jwt-from-query");
        accessor.setSessionAttributes(attrs);
        assertEquals("jwt-from-query", WebSocketConfig.extractToken(accessor));
    }

    @Test
    void extractToken_fromHandshakeSessionAttributeToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("token", "jwt-from-token-query");
        accessor.setSessionAttributes(attrs);
        assertEquals("jwt-from-token-query", WebSocketConfig.extractToken(accessor));
    }

    @Test
    void extractToken_prefersAuthorizationOverQuery() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer preferred");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("access_token", "from-query");
        accessor.setSessionAttributes(attrs);
        assertEquals("preferred", WebSocketConfig.extractToken(accessor));
    }

    @Test
    void extractToken_returnsNullWhenMissing() {
        assertNull(WebSocketConfig.extractToken(StompHeaderAccessor.create(StompCommand.CONNECT)));
    }

    @Test
    void handshakeInterceptor_copiesAccessTokenQueryParam() {
        Map<String, Object> attributes = new HashMap<>();
        WebSocketConfig.QueryTokenHandshakeInterceptor interceptor =
                new WebSocketConfig.QueryTokenHandshakeInterceptor();

        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/v1/streams");
        servletRequest.setQueryString("access_token=query-jwt%2B&token=legacy");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        ServletServerHttpResponse response =
                new ServletServerHttpResponse(new MockHttpServletResponse());

        assertTrue(interceptor.beforeHandshake(request, response, null, attributes));
        assertEquals("query-jwt+", attributes.get("access_token"));
        assertEquals("legacy", attributes.get("token"));
    }
}
