package com.am.mcp.util;

import com.am.mcp.config.AmMcpProperties;
import com.am.security.context.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves tool userId: inbound JWT sub → explicit arg (fin-agent JWT fallback) → configured default.
 * JWT wins so a tool argument cannot spoof another user when the request is authenticated.
 */
public final class UserIdResolver {

    private static final Logger log = LoggerFactory.getLogger(UserIdResolver.class);

    private UserIdResolver() {
    }

    public static String resolve(String userId, AmMcpProperties props) {
        String fromJwt = UserContext.getUserId();
        if (fromJwt != null && !fromJwt.isBlank()) {
            return fromJwt;
        }
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        String fallback = props.getDefaults().getUserId();
        log.warn("UserIdResolver: no JWT and no userId arg; using default userId={}", fallback);
        return fallback;
    }
}
