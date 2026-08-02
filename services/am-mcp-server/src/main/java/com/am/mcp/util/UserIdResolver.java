package com.am.mcp.util;

import com.am.mcp.config.AmMcpProperties;
import com.am.security.context.UserContext;

/**
 * Resolves tool userId: explicit arg → inbound JWT sub → configured default.
 */
public final class UserIdResolver {

    private UserIdResolver() {
    }

    public static String resolve(String userId, AmMcpProperties props) {
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        String fromJwt = UserContext.getUserId();
        if (fromJwt != null && !fromJwt.isBlank()) {
            return fromJwt;
        }
        return props.getDefaults().getUserId();
    }
}
