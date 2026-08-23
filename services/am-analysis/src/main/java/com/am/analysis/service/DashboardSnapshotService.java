package com.am.analysis.service;

import com.am.analysis.adapter.model.DashboardSnapshot;
import com.am.analysis.adapter.model.DashboardWidgetType;
import com.am.analysis.adapter.repository.DashboardSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardSnapshotService {

    private final DashboardSnapshotRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration REDIS_TTL = Duration.ofMinutes(5);

    public void persist(String userId, DashboardWidgetType widget, Object payload) {
        if (userId == null || widget == null || payload == null) {
            return;
        }
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            String redisKey = widget.redisKey(userId);
            redisTemplate.opsForValue().set(redisKey, payloadJson, REDIS_TTL);

            repository.save(DashboardSnapshot.of(userId, widget, payloadJson));
            log.debug("[Snapshot] Saved {} for user {}", widget, userId);
        } catch (Exception ex) {
            log.error("[Snapshot] Failed to persist {} for user {}", widget, userId, ex);
        }
    }

    public <T> Optional<T> load(String userId, DashboardWidgetType widget, Class<T> payloadType) {
        if (userId == null || widget == null) {
            return Optional.empty();
        }
        try {
            String redisKey = widget.redisKey(userId);
            String cachedJson = redisTemplate.opsForValue().get(redisKey);
            if (cachedJson != null) {
                log.debug("[Snapshot] Redis HIT {} user {}", widget, userId);
                return Optional.of(objectMapper.readValue(cachedJson, payloadType));
            }

            log.debug("[Snapshot] Redis MISS {} user {} — loading MongoDB", widget, userId);
            Optional<DashboardSnapshot> dbSnapshotOpt = repository.findByUserIdAndWidget(userId, widget);
            if (dbSnapshotOpt.isPresent()) {
                DashboardSnapshot dbSnapshot = dbSnapshotOpt.get();
                if (isExpired(dbSnapshot.getCalculatedAt())) {
                    log.info("[Snapshot] Mongo snapshot expired {} user {} calculatedAt={}",
                            widget, userId, dbSnapshot.getCalculatedAt());
                    return Optional.empty();
                }
                String payloadJson = dbSnapshot.getPayloadJson();
                T data = objectMapper.readValue(payloadJson, payloadType);
                redisTemplate.opsForValue().set(redisKey, payloadJson, REDIS_TTL);
                return Optional.of(data);
            }
        } catch (Exception ex) {
            log.error("[Snapshot] Failed to load {} for user {}", widget, userId, ex);
        }
        return Optional.empty();
    }

    static boolean isExpired(java.time.LocalDateTime calculatedAt) {
        if (calculatedAt == null) {
            return true;
        }
        return calculatedAt.isBefore(java.time.LocalDateTime.now().minus(REDIS_TTL));
    }
}
