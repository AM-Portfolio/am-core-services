package com.am.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Manages WebSocket subscriptions for portfolio updates and triggers
 * periodic portfolio calculations only when users are actively subscribed.
 * 
 * Flow:
 * 1. User subscribes to /user/queue/portfolio -> Start 10-second scheduler
 * 2. Every 10 seconds -> Send calculation trigger to Kafka
 * 3. User unsubscribes/disconnects -> Stop scheduler
 */
@Service
@Slf4j
public class PortfolioSubscriptionManager {

    private final TaskScheduler taskScheduler;
    private final GatewayKafkaProducer gatewayKafkaProducer;

    public PortfolioSubscriptionManager(
            @Qualifier("portfolioTaskScheduler") TaskScheduler taskScheduler,
            GatewayKafkaProducer gatewayKafkaProducer) {
        this.taskScheduler = taskScheduler;
        this.gatewayKafkaProducer = gatewayKafkaProducer;
    }

    @Value("${portfolio.calculation.interval-seconds:10}")
    private int calculationIntervalSeconds;

    @Value("${portfolio.calculation.max-concurrent-users:100}")
    private int maxConcurrentUsers;

    // Track active schedulers: userId -> ScheduledFuture
    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeSchedulers = new ConcurrentHashMap<>();

    // Track selected portfolio per user: userId -> portfolioId
    private final ConcurrentHashMap<String, String> userPortfolios = new ConcurrentHashMap<>();

    /**
     * Handle user subscription to portfolio updates.
     * Starts a periodic scheduler if subscribing to /user/queue/portfolio.
     */
    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headers.getDestination();
        Principal user = headers.getUser();

        if (destination == null || user == null) {
            return;
        }

        // Only handle portfolio queue subscriptions
        if (destination.contains("/queue/portfolio")) {
            String userId = user.getName();

            // Check if we've hit the concurrent user limit
            if (activeSchedulers.size() >= maxConcurrentUsers && !activeSchedulers.containsKey(userId)) {
                log.warn("Max concurrent users ({}) reached. Cannot start scheduler for user: {}",
                        maxConcurrentUsers, userId);
                return;
            }

            startSchedulerForUser(userId);
        }
    }

    /**
     * Handle user unsubscription from portfolio updates.
     * Stops the periodic scheduler if it exists.
     */
    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headers.getUser();

        if (user != null) {
            String userId = user.getName();
            stopSchedulerForUser(userId);
        }
    }

    /**
     * Handle WebSocket session disconnect.
     * Ensures cleanup of any active schedulers for the disconnected user.
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headers.getUser();

        if (user != null) {
            String userId = user.getName();
            stopSchedulerForUser(userId);
        }
    }

    /**
     * Start a periodic scheduler for the given user.
     * The scheduler sends calculation trigger events to Kafka every N seconds.
     */
    private void startSchedulerForUser(String userId) {
        // If scheduler already exists, don't create a duplicate
        if (activeSchedulers.containsKey(userId)) {
            log.debug("Scheduler already active for user: {}", userId);
            return;
        }

        String currentPortfolioId = userPortfolios.get(userId);
        log.info("🚀 Starting portfolio calculation scheduler for user: {} (Interval: {}s, Current Portfolio: {})",
                userId, calculationIntervalSeconds, currentPortfolioId != null ? currentPortfolioId : "ALL");

        // Create the scheduled task
        ScheduledFuture<?> scheduledFuture = taskScheduler.scheduleAtFixedRate(
                () -> triggerCalculation(userId),
                Duration.ofSeconds(calculationIntervalSeconds));

        // Store the future for later cancellation
        activeSchedulers.put(userId, scheduledFuture);

        log.info("✅ Scheduler started successfully for user: {}. Total active schedulers: {}",
                userId, activeSchedulers.size());
    }

    /**
     * Stop the periodic scheduler for the given user.
     */
    private void stopSchedulerForUser(String userId) {
        ScheduledFuture<?> scheduledFuture = activeSchedulers.remove(userId);

        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            log.info("🛑 Stopped portfolio calculation scheduler for user: {}. Total active schedulers: {}",
                    userId, activeSchedulers.size());
        } else {
            log.debug("No active scheduler found for user: {}", userId);
        }
    }

    /**
     * Trigger a portfolio calculation by sending a message to Kafka.
     * Uses the user's selected portfolioId if available.
     */
    private void triggerCalculation(String userId) {
        try {
            String portfolioId = userPortfolios.get(userId);
            gatewayKafkaProducer.sendCalculationTrigger(userId, portfolioId, "AUTOMATED_SCHEDULER");
        } catch (Exception e) {
            log.error("❌ Failed to trigger calculation for user: {}", userId, e);
            // Don't crash the scheduler - just log and continue
        }
    }

    /**
     * Update the selected portfolio for a user.
     * Called externally when user explicitly selects a portfolio.
     */
    public void setUserPortfolio(String userId, String portfolioId) {
        if (portfolioId != null && !portfolioId.isEmpty()) {
            userPortfolios.put(userId, portfolioId);
            log.info("💾 Portfolio Selection Updated → User: {} | PortfolioID: {}", userId, portfolioId);
        } else {
            userPortfolios.remove(userId);
            log.info("🗑️ Portfolio Selection Cleared → User: {} (will trigger ALL portfolios)", userId);
        }
    }

    /**
     * Get the count of currently active schedulers.
     * Useful for monitoring and metrics.
     */
    public int getActiveSchedulerCount() {
        return activeSchedulers.size();
    }
}
