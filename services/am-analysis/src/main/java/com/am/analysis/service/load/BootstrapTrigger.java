package com.am.analysis.service.load;

/** Source that initiated a portfolio entity load (used for bootstrap tracing). */
public enum BootstrapTrigger {
    HTTP_READ,
    DASHBOARD,
    WS_SUBSCRIBE
}
