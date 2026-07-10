package com.am.scheduler.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB Document representation for storing the logbook history/audit of
 * scheduler runs.
 * This class is written in standard Java (without Lombok) to ensure
 * compatibility
 * and compile-time safety across different modules during builds.
 */
@Document(collection = "scheduler_execution_audit")
public class SchedulerExecutionAuditRecord {

    @Id
    private String id; // Unique database identifier for this specific execution run

    private String jobName; // The unique name of the scheduler task (matching the annotation name)

    private String status; // Current status of the execution: "RUNNING", "SUCCESS", "FAILED", or "SKIPPED"

    @org.springframework.data.mongodb.core.index.Indexed(expireAfter = "14d")
    private Instant startTime; // The exact timestamp when this execution started

    private Instant endTime; // The exact timestamp when this execution completed or failed

    private Long durationMs; // Total duration of the scheduler run in milliseconds

    private String executedBy; // The pod ID / hostname of the server replica that executed this scheduler task

    private String errorMessage; // The error details/message in case the execution throws an exception (FAILED
                                 // status)

    private String stackTrace; // Full stack trace recorded in case of failures for easy debugging without
                               // checking raw log files

    // Default constructor required by Spring Data MongoDB for deserialization
    public SchedulerExecutionAuditRecord() {
    }

    // Full constructor to support builder patterns and manual instantiation
    public SchedulerExecutionAuditRecord(String id, String jobName, String status, Instant startTime,
            Instant endTime, Long durationMs, String executedBy,
            String errorMessage, String stackTrace) {
        this.id = id;
        this.jobName = jobName;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMs = durationMs;
        this.executedBy = executedBy;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
    }

    // Standard static method to initiate the custom Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent Builder helper class to construct SchedulerExecutionAuditRecord
     * objects easily.
     */
    public static class Builder {
        private String id;
        private String jobName;
        private String status;
        private Instant startTime;
        private Instant endTime;
        private Long durationMs;
        private String executedBy;
        private String errorMessage;
        private String stackTrace;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder jobName(String jobName) {
            this.jobName = jobName;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder durationMs(Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder executedBy(String executedBy) {
            this.executedBy = executedBy;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public SchedulerExecutionAuditRecord build() {
            return new SchedulerExecutionAuditRecord(id, jobName, status, startTime, endTime, durationMs, executedBy,
                    errorMessage, stackTrace);
        }
    }

    // --- GETTERS AND SETTERS ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
}

// comment