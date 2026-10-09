package com.am.analysis.adapter.service;

/**
 * Port (outbound interface) that allows the adapter layer ({@link AnalysisIngestionService})
 * to trigger a dashboard refresh without depending directly on the service layer.
 *
 * <p>The concrete implementation is provided by the {@code am-analysis} service module
 * (which wires it to {@code DashboardAnalysisService}). When running in isolation
 * (e.g. tests, or if the service layer is not on the classpath), the bean is absent
 * and the call is safely skipped via {@code @Autowired(required = false)}.
 */
public interface DashboardRefreshPort {

    /**
     * Publish a dashboard recalculation for the given user.
     *
     * @param userId the user whose dashboard widgets should be refreshed
     */
    void publishDashboardUpdate(String userId);
}
