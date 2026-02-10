package com.am.analysis.controller;

import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.dto.TopMoversResponse;
import com.am.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/{type}/{id}/allocation")
    public ResponseEntity<AllocationResponse> getAllocation(
            @RequestHeader("Authorization") String token,
            @PathVariable String type,
            @PathVariable String id) {
        try {
            String userId = com.am.security.util.TokenExtractor.extractUserId(token);
            AnalysisEntityType entityType = AnalysisEntityType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(analysisService.getAllocation(id, entityType, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{type}/{id}/performance")
    public ResponseEntity<PerformanceResponse> getPerformance(
            @RequestHeader("Authorization") String token,
            @PathVariable String type,
            @PathVariable String id,
            @RequestParam(defaultValue = "1M") String timeFrame) {
        try {
            String userId = com.am.security.util.TokenExtractor.extractUserId(token);
            AnalysisEntityType entityType = AnalysisEntityType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(analysisService.getPerformance(id, entityType, timeFrame, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{type}/top-movers")
    public ResponseEntity<TopMoversResponse> getTopMoversByCategory(
            @RequestHeader("Authorization") String token,
            @PathVariable String type,
            @RequestParam(defaultValue = "1D") String timeFrame) {
        try {
            String userId = com.am.security.util.TokenExtractor.extractUserId(token);
            AnalysisEntityType entityType = AnalysisEntityType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(analysisService.getTopMovers(null, entityType, timeFrame, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{type}/{id}/top-movers")
    public ResponseEntity<TopMoversResponse> getTopMoversByEntity(
            @RequestHeader("Authorization") String token,
            @PathVariable String type,
            @PathVariable String id,
            @RequestParam(defaultValue = "1D") String timeFrame) {
        try {
            String userId = com.am.security.util.TokenExtractor.extractUserId(token);
            AnalysisEntityType entityType = AnalysisEntityType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(analysisService.getTopMovers(id, entityType, timeFrame, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
