package com.am.scheduler.aspect;

import com.am.scheduler.annotation.TrackedAndLockedScheduler;
import com.am.scheduler.model.SchedulerExecutionAuditRecord;
import com.am.scheduler.repository.SchedulerExecutionAuditRepository;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.convert.DurationStyle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;

/**
 * Aspect to intercept scheduler executions, manage locking via ShedLock,
 * and save audit execution records in MongoDB.
 * 
 * Works automatically for any method annotated with {@link TrackedAndLockedScheduler}.
 */
@Aspect
public class SchedulerTrackingAndLockingAspect {

    private static final Logger log = LoggerFactory.getLogger(SchedulerTrackingAndLockingAspect.class);

    private final SchedulerExecutionAuditRepository auditRepository;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final String hostname;

    /**
     * Constructor setup that retrieves the container/pod hostname automatically.
     * Hostname is stored in MongoDB records to track which replica executed the scheduler.
     */
    public SchedulerTrackingAndLockingAspect(SchedulerExecutionAuditRepository auditRepository, LockProvider lockProvider) {
        this.auditRepository = auditRepository;
        // ShedLock's DefaultLockingTaskExecutor provides programmatic locking
        this.lockingTaskExecutor = new DefaultLockingTaskExecutor(lockProvider);

        // Fetch Pod ID/Name from Kubernetes env variables, fall back to localhost name
        String host = System.getenv("HOSTNAME");
        if (host == null || host.isEmpty()) {
            try {
                host = java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                host = "unknown-host";
            }
        }
        this.hostname = host;
    }

    /**
     * Intercepts execution of methods annotated with @TrackedAndLockedScheduler.
     * Uses AOP @Around advice to intercept before, during, and after the method call.
     */
    @Around("@annotation(annotation)")
    public Object traceAndLock(ProceedingJoinPoint joinPoint, TrackedAndLockedScheduler annotation) throws Throwable {
        String jobName = annotation.name();
        
        // Parse Spring-style durations (e.g., "15m" -> Duration of 15 Minutes)
        Duration lockAtMostFor = DurationStyle.detectAndParse(annotation.lockAtMostFor());
        Duration lockAtLeastFor = DurationStyle.detectAndParse(annotation.lockAtLeastFor());

        // Prepare the lock key settings for ShedLock
        LockConfiguration lockConfiguration = new LockConfiguration(
                Instant.now(),
                jobName,
                lockAtMostFor,
                lockAtLeastFor
        );

        log.debug("Attempting to run scheduler job [{}] with lock configurations: lockAtMostFor={}, lockAtLeastFor={}",
                jobName, lockAtMostFor, lockAtLeastFor);

        // Step 1: Request lock and execute the underlying scheduler code only if lock is acquired
        LockingTaskExecutor.TaskResult<Object> result = lockingTaskExecutor.executeWithLock(
                () -> executeAndAudit(joinPoint, jobName),
                lockConfiguration
        );

        // Step 2: Check if the lock was acquired and the job was executed
        if (result.wasExecuted()) {
            // Task ran successfully, return result if any
            return result.getResult();
        } else {
            // Task was skipped because another replica is already running it
            log.info("Scheduler job [{}] execution skipped because lock is held by another instance.", jobName);

            // Log skipped execution in MongoDB for visibility in our audit logs
            SchedulerExecutionAuditRecord skippedRecord = SchedulerExecutionAuditRecord.builder()
                    .jobName(jobName)
                    .status("SKIPPED")
                    .startTime(Instant.now())
                    .endTime(Instant.now())
                    .durationMs(0L)
                    .executedBy(hostname)
                    .build();
            auditRepository.save(skippedRecord);

            return null;
        }
    }

    /**
     * Inner helper to record the execution lifecycle (RUNNING, SUCCESS, FAILED)
     * and measure total execution duration.
     */
    private Object executeAndAudit(ProceedingJoinPoint joinPoint, String jobName) throws Throwable {
        Instant startTime = Instant.now();

        // 1. Create a "RUNNING" entry in MongoDB logbook
        SchedulerExecutionAuditRecord record = SchedulerExecutionAuditRecord.builder()
                .jobName(jobName)
                .status("RUNNING")
                .startTime(startTime)
                .executedBy(hostname)
                .build();

        record = auditRepository.save(record);

        try {
            // 2. Run the actual business logic of the scheduled method
            Object output = joinPoint.proceed();

            // 3. Mark the log as "SUCCESS" and calculate total runtime
            Instant endTime = Instant.now();
            record.setEndTime(endTime);
            record.setDurationMs(Duration.between(startTime, endTime).toMillis());
            record.setStatus("SUCCESS");
            auditRepository.save(record);

            return output;
        } catch (Throwable t) {
            // 4. Mark the log as "FAILED" on exceptions, record error details & stack trace, then rethrow
            Instant endTime = Instant.now();
            record.setEndTime(endTime);
            record.setDurationMs(Duration.between(startTime, endTime).toMillis());
            record.setStatus("FAILED");
            record.setErrorMessage(t.getMessage());

            // Convert stack trace to String for database storage
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            record.setStackTrace(sw.toString());

            auditRepository.save(record);
            
            // Rethrow the exception so normal application logging / alerting remains active
            throw t;
        }
    }
}
