package com.am.analysis.adapter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Durable dashboard widget snapshot (MongoDB) with Redis hot cache.
 * <p>
 * Contract per widget — see {@link DashboardWidgetType} and {@link DashboardSnapshotContract}:
 * <ul>
 *   <li>{@code widget} — which card (SUMMARY, ACTIVITY, …)</li>
 *   <li>{@code payloadJson} — serialized DTO JSON ({@link DashboardSnapshotContract#payloadTypeName})</li>
 *   <li>{@code schemaVersion} — payload schema generation ({@link DashboardSnapshotContract#CURRENT_SCHEMA_VERSION})</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("dashboard_snapshots")
public class DashboardSnapshot {

    /** Composite key {@code userId:WIDGET} — see {@link DashboardWidgetType#documentId(String)}. */
    @Id
    private String id;

    private String userId;

    /** Widget discriminator; stored as enum name in MongoDB (SUMMARY, ACTIVITY, …). */
    private DashboardWidgetType widget;

    /**
     * JSON body of the widget DTO. Type is determined by {@link #widget}.
     * Mongo field kept as {@code dataJson} for backward compatibility.
     */
    @Field("dataJson")
    private String payloadJson;

    /** v1 always false on write; v2 may flag pending recalc after ingest. */
    @Builder.Default
    private boolean isStale = false;

    @Builder.Default
    private int schemaVersion = DashboardSnapshotContract.CURRENT_SCHEMA_VERSION;

    private LocalDateTime calculatedAt;

    public static DashboardSnapshot of(String userId, DashboardWidgetType widget, String payloadJson) {
        return DashboardSnapshot.builder()
                .id(widget.documentId(userId))
                .userId(userId)
                .widget(widget)
                .payloadJson(payloadJson)
                .schemaVersion(DashboardSnapshotContract.CURRENT_SCHEMA_VERSION)
                .isStale(false)
                .calculatedAt(LocalDateTime.now())
                .build();
    }
}
