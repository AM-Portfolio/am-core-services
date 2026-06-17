package com.am.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Shared Redis-based Interest Registry.
 * Lives in am-kafka-lib so it can be used by both am-gateway and am-analysis without circular deps.
 *
 * Design:
 *   - Key: "interest:{userId}"         → Value: portfolioId (or "ALL" for all portfolios)
 *   - Key: "interest:{userId}:session" → Value: sessionId
 *   - TTL: 35 seconds (refreshed every 30s via heartbeat; auto-expires ghost users)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterestRegistryService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "interest:";
    private static final Duration TTL = Duration.ofSeconds(35);

    public void register(String userId, String portfolioId, String sessionId) {
        try {
            String watchTarget = (portfolioId != null && !portfolioId.isBlank()) ? portfolioId : "ALL";
            redisTemplate.opsForValue().set(KEY_PREFIX + userId, watchTarget, TTL);
            redisTemplate.opsForValue().set(KEY_PREFIX + userId + ":session", sessionId, TTL);
            log.debug("[Interest] Registered User: {} → Portfolio: {}", userId, watchTarget);
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for registration: {}", ex.getMessage());
        }
    }

    public void heartbeat(String userId) {
        try {
            redisTemplate.expire(KEY_PREFIX + userId, TTL);
            redisTemplate.expire(KEY_PREFIX + userId + ":session", TTL);
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for heartbeat: {}", ex.getMessage());
        }
    }

    public void deregister(String userId) {
        try {
            redisTemplate.delete(KEY_PREFIX + userId);
            redisTemplate.delete(KEY_PREFIX + userId + ":session");
            log.info("[Interest] Deregistered User: {}", userId);
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for deregistration: {}", ex.getMessage());
        }
    }

    public Optional<String> getWatchedPortfolio(String userId) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + userId));
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for getWatchedPortfolio: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean hasActiveWatchers(String portfolioId) {
        try {
            // Optimization: In a production environment, we should use a Redis Set (SADD/SMEMBERS)
            // for portfolio watchers to avoid O(N) keys() scanning.
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) return false;
            for (String key : keys) {
                if (key.endsWith(":session")) continue;
                String value = redisTemplate.opsForValue().get(key);
                if ("ALL".equals(value) || portfolioId.equals(value)) return true;
            }
            return false;
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for hasActiveWatchers: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Retrieve all user IDs currently marked as active in the registry.
     * Used by the Orchestrator for proactive fan-out of dashboard updates.
     *
     * @return Set of active user IDs.
     */
    public Set<String> getAllActiveUsers() {
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) return java.util.Collections.emptySet();
            
            return keys.stream()
                .filter(key -> !key.endsWith(":session"))
                .map(key -> key.substring(KEY_PREFIX.length()))
                .collect(java.util.stream.Collectors.toSet());
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for getAllActiveUsers: {}", ex.getMessage());
            return java.util.Collections.emptySet();
        }
    }
    
    // Trigger SDK publish to deploy am-kafka-lib
}
