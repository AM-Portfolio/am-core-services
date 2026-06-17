package com.am.analysis.adapter.repository;

import com.am.analysis.adapter.model.DashboardSnapshot;
import com.am.analysis.adapter.model.DashboardWidgetType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DashboardSnapshotRepository extends MongoRepository<DashboardSnapshot, String> {
    Optional<DashboardSnapshot> findByUserIdAndWidget(String userId, DashboardWidgetType widget);
}
