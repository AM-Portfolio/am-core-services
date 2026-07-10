package com.am.scheduler.repository;

import com.am.scheduler.model.SchedulerExecutionAuditRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Repository for saving and retrieving SchedulerExecutionAuditRecord entities in MongoDB.
 */
@Repository
public interface SchedulerExecutionAuditRepository extends MongoRepository<SchedulerExecutionAuditRecord, String> {
}
