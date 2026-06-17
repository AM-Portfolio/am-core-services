package com.am.kafka.service;

import com.am.kafka.config.InterestRegistryKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared Redis-based Interest Registry.
 * Lives in am-kafka-lib so it can be used by both am-gateway and am-analysis without circular deps.
 *
 * Design:
 *   - Key: "interest:registry"         → Hash: {userId: watchTarget}
 *   - Key: "interest:sessions"         → Hash: {userId: sessionId}
 *   - TTL: 35 seconds (refreshed every 30s via heartbeat; auto-expires ghost users)
 *
 * Watch targets: real {@code portfolioId} or {@link InterestRegistryKeys#CHANNEL_DASHBOARD_MAIN}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterestRegistryService {

    private final StringRedisTemplate redisTemplate;

    private static final String REGISTRY_KEY = "interest:registry";
    private static final String SESSIONS_KEY = "interest:sessions";
    private static final Duration TTL = Duration.ofSeconds(35);

    public void register(String userId, String portfolioId, String sessionId) {
        try {
            String watchTarget = resolveWatchTarget(portfolioId);
            redisTemplate.opsForHash().put(REGISTRY_KEY, userId, watchTarget);
            redisTemplate.opsForHash().put(SESSIONS_KEY, userId, sessionId);
            redisTemplate.expire(REGISTRY_KEY, TTL);
            redisTemplate.expire(SESSIONS_KEY, TTL);
            log.debug("[Interest] Registered User: {} → Watch: {}", userId, watchTarget);
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for registration: {}", ex.getMessage());
        }
    }

    private static String resolveWatchTarget(String portfolioId) {
        if (InterestRegistryKeys.isDashboardChannel(portfolioId)) {
            return InterestRegistryKeys.CHANNEL_DASHBOARD_MAIN;
        }
        if (portfolioId != null && !portfolioId.isBlank()) {
            return portfolioId;
        }
        return portfolioId != null ? portfolioId : "";
    }

    public void heartbeat(String userId) {
        try {
            redisTemplate.expire(REGISTRY_KEY, TTL);
            redisTemplate.expire(SESSIONS_KEY, TTL);
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for heartbeat: {}", ex.getMessage());
        }
    }

    public void deregister(String userId) {
        try {
            redisTemplate.opsForHash().delete(REGISTRY_KEY, userId);
            redisTemplate.opsForHash().delete(SESSIONS_KEY, userId);
            log.info("[Interest] Deregistered User: {}", userId);
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for deregistration: {}", ex.getMessage());
        }
    }

    public Optional<String> getWatchedPortfolio(String userId) {
        try {
            return Optional.ofNullable((String) redisTemplate.opsForHash().get(REGISTRY_KEY, userId));
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for getWatchedPortfolio: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean hasActiveWatchers(String portfolioId) {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(REGISTRY_KEY);
            return entries.values().stream()
                    .map(Object::toString)
                    .anyMatch(val -> portfolioId.equals(val)
                            && !InterestRegistryKeys.isDashboardChannel(val));
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for hasActiveWatchers: {}", ex.getMessage());
            return false;
        }
    }

    public boolean isUserOnDashboardChannel(String userId) {
        return getWatchedPortfolio(userId)
                .map(InterestRegistryKeys::isDashboardChannel)
                .orElse(false);
    }

    public Set<String> getAllActiveUserIds() {
        try {
            return redisTemplate.opsForHash().keys(REGISTRY_KEY).stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for getAllActiveUserIds: {}", ex.getMessage());
            return Set.of();
        }
    }

    public Map<String, String> getAllWatchers() {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(REGISTRY_KEY);
            return entries.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        } catch (Exception ex) {
            log.warn("[Interest] Redis unavailable for getAllWatchers: {}", ex.getMessage());
            return Map.of();
        }
    }
}
