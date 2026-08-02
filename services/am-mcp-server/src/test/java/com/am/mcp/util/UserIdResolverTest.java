package com.am.mcp.util;

import com.am.mcp.config.AmMcpProperties;
import com.am.security.context.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdResolverTest {

    @AfterEach
    void clear() {
        UserContext.clear();
    }

    @Test
    void prefersExplicitArg() {
        AmMcpProperties props = new AmMcpProperties();
        props.getDefaults().setUserId("default-user");
        UserContext.setUserId("jwt-user");

        assertThat(UserIdResolver.resolve("arg-user", props)).isEqualTo("arg-user");
    }

    @Test
    void usesJwtWhenArgBlank() {
        AmMcpProperties props = new AmMcpProperties();
        props.getDefaults().setUserId("default-user");
        UserContext.setUserId("jwt-user");

        assertThat(UserIdResolver.resolve("  ", props)).isEqualTo("jwt-user");
        assertThat(UserIdResolver.resolve(null, props)).isEqualTo("jwt-user");
    }

    @Test
    void usesDefaultWhenNoJwt() {
        AmMcpProperties props = new AmMcpProperties();
        props.getDefaults().setUserId("default-user");

        assertThat(UserIdResolver.resolve(null, props)).isEqualTo("default-user");
    }
}
