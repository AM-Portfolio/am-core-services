package com.am.gateway.controller;

import com.am.observability.flow.FlowLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

/**
 * Resolves authenticated user id from STOMP JWT Principal (never from client payload alone).
 */
@Component
@RequiredArgsConstructor
public class StompPrincipalResolver {

    private final FlowLogger flowLogger;

    public String resolve(SimpMessageHeaderAccessor accessor, Map<String, String> payload) {
        return resolve(accessor, payload != null ? payload.get("userId") : null);
    }

    public String resolve(SimpMessageHeaderAccessor accessor) {
        return resolve(accessor, (String) null);
    }

    public String resolve(SimpMessageHeaderAccessor accessor, String payloadUserId) {
        Principal principal = accessor.getUser();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalArgumentException("Unauthorized: STOMP Principal required");
        }
        String authUserId = principal.getName();
        if (payloadUserId != null && !payloadUserId.isBlank() && !payloadUserId.equals(authUserId)) {
            flowLogger.step("gateway.stomp.user_id_mismatch",
                    "principal", authUserId, "payload", payloadUserId);
            throw new IllegalArgumentException("userId mismatch");
        }
        return authUserId;
    }
}
