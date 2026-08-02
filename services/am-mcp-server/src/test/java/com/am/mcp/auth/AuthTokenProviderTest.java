package com.am.mcp.auth;

import com.am.mcp.config.AmMcpProperties;
import com.am.security.context.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthTokenProviderTest {

    @AfterEach
    void clear() {
        UserContext.clear();
    }

    @Test
    void prefersInboundUserTokenOverStaticAndServiceLogin() {
        AmMcpProperties props = new AmMcpProperties();
        props.getAuth().setStaticToken("static-token");
        props.getAuth().setUsername("svc");
        props.getAuth().setPassword("pw");
        props.getAuth().setUrl("http://identity");

        AuthTokenProvider provider = new AuthTokenProvider(props, mock(RestClient.class));
        UserContext.setToken("user-jwt");

        assertThat(provider.getToken()).isEqualTo("user-jwt");
    }

    @Test
    void fallsBackToStaticWhenNoUserContext() {
        AmMcpProperties props = new AmMcpProperties();
        props.getAuth().setStaticToken("static-token");

        AuthTokenProvider provider = new AuthTokenProvider(props, mock(RestClient.class));

        assertThat(provider.getToken()).isEqualTo("static-token");
    }
}
