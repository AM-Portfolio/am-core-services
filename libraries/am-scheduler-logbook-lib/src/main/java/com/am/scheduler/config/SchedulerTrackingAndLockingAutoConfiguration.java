package com.am.scheduler.config;

import com.am.scheduler.aspect.SchedulerTrackingAndLockingAspect;
import com.am.scheduler.model.SchedulerExecutionAuditRecord;
import com.am.scheduler.repository.SchedulerExecutionAuditRepository;
import jakarta.annotation.PostConstruct;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.Duration;

/**
 * Spring configuration class that sets up the database lock provider (ShedLock MongoDB)
 * and configures the tracking/locking Aspect.
 */
@Configuration
@EnableMongoRepositories(basePackageClasses = SchedulerExecutionAuditRepository.class)
public class SchedulerTrackingAndLockingAutoConfiguration {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndices() {
        // Programmatically create TTL index to ensure older logs are cleaned up after 14 days
        try {
            mongoTemplate.indexOps(SchedulerExecutionAuditRecord.class)
                    .ensureIndex(new Index().on("startTime", Sort.Direction.ASC).expire(Duration.ofDays(14)));
        } catch (Exception e) {
            // Log warning but don't prevent application startup if index creation fails (e.g. read-only permissions)
            org.slf4j.LoggerFactory.getLogger(SchedulerTrackingAndLockingAutoConfiguration.class)
                    .warn("Failed to programmatically ensure TTL index on SchedulerExecutionAuditRecord: {}", e.getMessage());
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public LockProvider lockProvider(MongoTemplate mongoTemplate) {
        // Configures ShedLock to use MongoDB with the default 'shedLock' collection
        return new MongoLockProvider(mongoTemplate.getCollection("shedLock"));
    }

    @Bean
    @ConditionalOnMissingBean
    public SchedulerTrackingAndLockingAspect schedulerTrackingAndLockingAspect(
            SchedulerExecutionAuditRepository auditRepository,
            LockProvider lockProvider) {
        return new SchedulerTrackingAndLockingAspect(auditRepository, lockProvider);
    }
}
