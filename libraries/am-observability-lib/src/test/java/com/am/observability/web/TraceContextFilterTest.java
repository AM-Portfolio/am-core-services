package com.am.observability.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;

class TraceContextFilterTest {

    @Test
    void authenticatedPrincipalTakesPrecedenceOverPropagationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(() -> "jwt-subject");
        request.addHeader(TraceContextFilter.HEADER_USER_ID, "spoofed-header");

        assertThat(TraceContextFilter.resolveUserId(request)).isEqualTo("jwt-subject");
    }

    @Test
    void propagationHeaderIsUsedWhenRequestIsNotAuthenticated() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContextFilter.HEADER_USER_ID, "service-user");

        assertThat(TraceContextFilter.resolveUserId(request)).isEqualTo("service-user");
    }

    @Test
    void queryParameterIsNotAcceptedAsUserIdentity() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("userId", "query-user");

        assertThat(TraceContextFilter.resolveUserId(request)).isNull();
    }

    @Test
    void blankPrincipalFallsBackToPropagationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(new Principal() {
            @Override
            public String getName() {
                return " ";
            }
        });
        request.addHeader(TraceContextFilter.HEADER_USER_ID, "service-user");

        assertThat(TraceContextFilter.resolveUserId(request)).isEqualTo("service-user");
    }
}
