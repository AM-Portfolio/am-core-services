package com.am.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Redis-based Interest Registry for tracking which users are actively watching which portfolios.
 *
 * Design:
 *   - Key: "interest:{userId}"         → Value: portfolioId (or "ALL" for all portfolios)
 *   - Key: "interest:{userId}:session" → Value: sessionId
 *   - TTL: 35 seconds (must be refreshed every 30s via heartbeat)
 *
 * Scalability:
 *   - Fully stateless: any number of Gateway instances share the same Redis view.
 *   - Ghost User prevention: Redis TTL auto-expires disconnected users after 35s.
 *
 * Fallback:
 *   - If Redis is unavailable, the service degrades gracefully by returning empty.
 *   - The Orchestrator will stop triggering calculations for unregistered portfolios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterestRegistryService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "interest:";
    private static final Duration TTL = Duration.ofSeconds(35);

    /**
     * Register a user as actively watching a portfolio.
     * If portfolioId is null/empty, the user is treated as watching ALL portfolios.
     *
     * @param userId      The authenticated user ID.
     * @param portfolioId The portfolio UUID. Null = ALL.
     * @param sessionId   The WebSocket session ID.
     */
    public void register(String userId, String portfolioId, String sessionId) {
        try {
            String watchTarget = (portfolioId != null && !portfolioId.isBlank()) ? portfolioId : "ALL";
            redisTemplate.opsForValue().set(KEY_PREFIX + userId, watchTarget, TTL);
            redisTemplate.opsForValue().set(KEY_PREFIX + userId + ":session", sessionId, TTL);
            log.debug("[Interest] Registered User: {} → Portfolio: {} (Session: {})", userId, watchTarget, sessionId);
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for registration: {}", ex.getMessage());
        }
    }

    /**
     * Refresh the TTL for a user's subscription (heartbeat renewal).
     *
     * @param userId The authenticated user ID.
     */
    public void heartbeat(String userId) {
        try {
            redisTemplate.expire(KEY_PREFIX + userId, TTL);
            redisTemplate.expire(KEY_PREFIX + userId + ":session", TTL);
            log.debug("[Interest] Heartbeat for User: {}", userId);
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for heartbeat: {}", ex.getMessage());
        }
    }

    /**
     * Deregister a user's subscription (on explicit unsubscribe or disconnect).
     *
     * @param userId The authenticated user ID.
     */
    public void deregister(String userId) {
        try {
            redisTemplate.delete(KEY_PREFIX + userId);
            redisTemplate.delete(KEY_PREFIX + userId + ":session");
            log.info("[Interest] Deregistered User: {}", userId);
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for deregistration: {}", ex.getMessage());
        }
    }

    /**
     * Get the portfolio being watched by a user. Empty = not watching or Redis unavailable.
     *
     * @param userId The authenticated user ID.
     * @return Optional portfolioId ("ALL" if watching all).
     */
    public Optional<String> getWatchedPortfolio(String userId) {
        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
            return Optional.ofNullable(value);
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for getWatchedPortfolio: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Check if any user is currently watching a specific portfolio.
     * Used by the Orchestrator to decide whether to trigger calculation.
     *
     * @param portfolioId The portfolio UUID.
     * @return true if at least one user is watching this portfolio.
     */
    public boolean hasActiveWatchers(String portfolioId) {
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) return false;
            for (String key : keys) {
                if (key.endsWith(":session")) continue;
                String value = redisTemplate.opsForValue().get(key);
                if ("ALL".equals(value) || portfolioId.equals(value)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for hasActiveWatchers: {}", ex.getMessage());
            return false; // Degraded: skip calculation
        }
    }
}
