package com.am.analysis.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardSnapshotServiceTest {

    @Test
    void isExpired_treatsNullAndOldSnapshotsAsExpired() {
        assertTrue(DashboardSnapshotService.isExpired(null));
        assertTrue(DashboardSnapshotService.isExpired(LocalDateTime.now().minusHours(2)));
        assertFalse(DashboardSnapshotService.isExpired(LocalDateTime.now().minusMinutes(1)));
    }
}
