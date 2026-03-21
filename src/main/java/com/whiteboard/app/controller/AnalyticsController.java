package com.whiteboard.app.controller;

import com.whiteboard.app.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller exposing analytics and usage prediction endpoints.
 * Uses Apache Commons Math regression analysis on snapshot activity data
 * to identify collaboration patterns and forecast future usage trends.
 *
 * All endpoints require JWT authentication.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Constructor injection of the analytics service.
     *
     * @param analyticsService performs regression analysis and pattern detection
     */
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Analyze collaboration patterns by examining snapshot activity over time.
     * Identifies peak hours when users are most active and computes an
     * hourly activity distribution using SimpleRegression.
     *
     * GET /api/analytics/collaboration-patterns
     *
     * @return 200 OK with hourly activity map, peak hour, and regression metrics
     */
    @GetMapping("/collaboration-patterns")
    public ResponseEntity<Map<String, Object>> getCollaborationPatterns() {
        Map<String, Object> analysis = analyticsService.analyzeCollaborationPatterns();
        return ResponseEntity.ok(analysis);
    }

    /**
     * Predict future board usage trends using linear regression on daily snapshot counts.
     * Returns 7-day and 30-day predictions along with the regression slope
     * to indicate whether usage is growing, stable, or declining.
     *
     * GET /api/analytics/usage-prediction
     *
     * @return 200 OK with daily activity data, trend slope, and usage predictions
     */
    @GetMapping("/usage-prediction")
    public ResponseEntity<Map<String, Object>> getUsagePrediction() {
        Map<String, Object> prediction = analyticsService.predictUsagePatterns();
        return ResponseEntity.ok(prediction);
    }
}
