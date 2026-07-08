package com.am.scheduler.config;

import com.am.scheduler.aspect.SchedulerTrackingAndLockingAspect;
import com.am.scheduler.repository.SchedulerExecutionAuditRepository;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Spring configuration class that sets up the database lock provider (ShedLock MongoDB)
 * and configures the tracking/locking Aspect.
 */
@Configuration
@EnableMongoRepositories(basePackageClasses = SchedulerExecutionAuditRepository.class)
public class SchedulerTrackingAndLockingAutoConfiguration {

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
