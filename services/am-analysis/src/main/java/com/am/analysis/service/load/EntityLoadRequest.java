package com.am.analysis.service.load;

/**
 * Portfolio entity load request.
 *
 * @param sourceId null or "ALL" = all portfolios for user; otherwise a specific portfolioId
 */
public record EntityLoadRequest(
        String sourceId,
        String userId,
        BootstrapTrigger triggerSource
) {
    public static EntityLoadRequest allPortfolios(String userId, BootstrapTrigger trigger) {
        return new EntityLoadRequest(null, userId, trigger);
    }

    public static EntityLoadRequest onePortfolio(String portfolioId, String userId, BootstrapTrigger trigger) {
        return new EntityLoadRequest(portfolioId, userId, trigger);
    }
}
