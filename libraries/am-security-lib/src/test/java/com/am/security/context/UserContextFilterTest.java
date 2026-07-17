package com.am.security.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class UserContextFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
        UserContext.clear();
    }

    @Test
    void putUserIdInMdcWritesMdcAndRequestAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(UserContextFilter.putUserIdInMdc(request, "jwt-sub-123")).isTrue();
        assertThat(MDC.get(UserContextFilter.MDC_USER_ID)).isEqualTo("jwt-sub-123");
        assertThat(request.getAttribute(UserContextFilter.REQUEST_ATTR_USER_ID)).isEqualTo("jwt-sub-123");
    }

    @Test
    void putUserIdInMdcIgnoresBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(UserContextFilter.putUserIdInMdc(request, "  ")).isFalse();
        assertThat(MDC.get(UserContextFilter.MDC_USER_ID)).isNull();
        assertThat(request.getAttribute(UserContextFilter.REQUEST_ATTR_USER_ID)).isNull();
    }

    @Test
    void jwtSubjectOverridesPropagationHeaderInMdc() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MDC.put(UserContextFilter.MDC_USER_ID, "spoofed-header");

        assertThat(UserContextFilter.putUserIdInMdc(request, "jwt-sub-123")).isTrue();
        assertThat(MDC.get(UserContextFilter.MDC_USER_ID)).isEqualTo("jwt-sub-123");
    }
}
