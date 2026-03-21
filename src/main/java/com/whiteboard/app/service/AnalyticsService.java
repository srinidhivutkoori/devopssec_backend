package com.whiteboard.app.service;

import com.whiteboard.app.model.VersionSnapshot;
import com.whiteboard.app.repository.VersionSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service providing analytics on collaboration patterns and board usage.
 * Uses Apache Commons Math SimpleRegression to perform linear regression on
 * snapshot creation timestamps, predicting future peak collaboration times
 * and modeling board activity trends over time.
 *
 * Methodology:
 * - Snapshot creation events serve as proxies for collaboration activity
 * - Each snapshot timestamp is converted to a Unix epoch value (x-axis)
 * - Snapshot count per hour-of-day is used to identify peak collaboration hours
 * - SimpleRegression models the trend line over time to predict future usage
 */
@Service
@Slf4j
public class AnalyticsService {

    private final VersionSnapshotRepository snapshotRepository;

    /**
     * Constructor injection of the snapshot repository.
     *
     * @param snapshotRepository data access for version snapshots (activity proxy)
     */
    public AnalyticsService(VersionSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Analyze collaboration patterns across all boards.
     * Groups snapshot activity by hour-of-day to identify when users are most active.
     * Uses SimpleRegression to compute a trend line over the activity data.
     *
     * @return a map containing:
     *         - "hourlyActivity": map of hour (0-23) to snapshot count
     *         - "peakHour": the hour with the highest activity
     *         - "regression": regression slope and intercept for the trend
     *         - "summary": human-readable summary of the analysis
     */
    @Transactional(readOnly = true)
    public Map<String, Object> analyzeCollaborationPatterns() {
        List<VersionSnapshot> snapshots = snapshotRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();

        if (snapshots.isEmpty()) {
            result.put("message", "No snapshot data available for analysis");
            result.put("hourlyActivity", Collections.emptyMap());
            return result;
        }

        // Group snapshots by hour-of-day (0-23) to find peak collaboration times
        Map<Integer, Long> hourlyActivity = snapshots.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCreatedAt().getHour(),
                        Collectors.counting()
                ));

        // Identify the hour with the most activity (peak collaboration time)
        int peakHour = hourlyActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);

        // Build regression data: x = hour of day, y = snapshot count at that hour
        SimpleRegression regression = new SimpleRegression();
        hourlyActivity.forEach((hour, count) ->
                regression.addData(hour.doubleValue(), count.doubleValue())
        );

        // Compile regression metrics
        Map<String, Object> regressionMetrics = new LinkedHashMap<>();
        regressionMetrics.put("slope", regression.getSlope());
        regressionMetrics.put("intercept", regression.getIntercept());
        regressionMetrics.put("rSquared", regression.getRSquare());
        regressionMetrics.put("sampleSize", regression.getN());

        result.put("totalSnapshots", snapshots.size());
        result.put("hourlyActivity", hourlyActivity);
        result.put("peakHour", peakHour);
        result.put("peakHourLabel",
                peakHour >= 0 ? String.format("%02d:00 - %02d:59", peakHour, peakHour) : "N/A");
        result.put("regression", regressionMetrics);
        result.put("summary", buildCollaborationSummary(peakHour, snapshots.size()));

        log.debug("Collaboration analysis complete: {} snapshots, peak hour: {}", snapshots.size(), peakHour);
        return result;
    }

    /**
     * Predict board usage trends using linear regression on snapshot creation over time.
     * Models snapshot frequency over time to forecast future activity levels.
     * The x-axis is the day index since the first snapshot, the y-axis is daily count.
     *
     * @return a map containing:
     *         - "dailyActivity": map of date to snapshot count
     *         - "trendSlope": positive (increasing) or negative (decreasing) trend
     *         - "prediction7Days": predicted daily snapshot count 7 days from now
     *         - "prediction30Days": predicted daily snapshot count 30 days from now
     *         - "interpretation": human-readable trend interpretation
     */
    @Transactional(readOnly = true)
    public Map<String, Object> predictUsagePatterns() {
        List<VersionSnapshot> snapshots = snapshotRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();

        if (snapshots.isEmpty()) {
            result.put("message", "No snapshot data available for prediction");
            result.put("dailyActivity", Collections.emptyMap());
            return result;
        }

        // Group snapshot counts by calendar date
        Map<String, Long> dailyActivity = snapshots.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCreatedAt().toLocalDate().toString(),
                        Collectors.counting()
                ));

        // Sort dates chronologically for regression
        List<Map.Entry<String, Long>> sortedDailyActivity = new ArrayList<>(dailyActivity.entrySet());
        sortedDailyActivity.sort(Map.Entry.comparingByKey());

        // Build regression: x = day index (0-based), y = snapshot count that day
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < sortedDailyActivity.size(); i++) {
            regression.addData(i, sortedDailyActivity.get(i).getValue().doubleValue());
        }

        int currentDayIndex = sortedDailyActivity.size();
        double prediction7Days  = regression.predict(currentDayIndex + 7);
        double prediction30Days = regression.predict(currentDayIndex + 30);

        // Ensure predictions are not negative (activity cannot be below zero)
        prediction7Days  = Math.max(0, prediction7Days);
        prediction30Days = Math.max(0, prediction30Days);

        String interpretation = regression.getSlope() > 0.05
                ? "Board usage is trending upward - collaboration activity is increasing"
                : regression.getSlope() < -0.05
                ? "Board usage is trending downward - collaboration activity is decreasing"
                : "Board usage is relatively stable";

        result.put("totalDays", sortedDailyActivity.size());
        result.put("dailyActivity", dailyActivity);
        result.put("trendSlope", regression.getSlope());
        result.put("rSquared", regression.getRSquare());
        result.put("prediction7Days", Math.round(prediction7Days * 100.0) / 100.0);
        result.put("prediction30Days", Math.round(prediction30Days * 100.0) / 100.0);
        result.put("interpretation", interpretation);

        log.debug("Usage prediction complete: {} days of data, slope={}", currentDayIndex, regression.getSlope());
        return result;
    }

    /**
     * Build a human-readable summary of the collaboration pattern analysis.
     *
     * @param peakHour       the hour with the most activity
     * @param totalSnapshots total number of snapshots analyzed
     * @return a formatted summary string
     */
    private String buildCollaborationSummary(int peakHour, int totalSnapshots) {
        if (peakHour < 0) {
            return "Insufficient data for collaboration pattern analysis";
        }

        String timeOfDay;
        if (peakHour >= 6 && peakHour < 12) {
            timeOfDay = "morning";
        } else if (peakHour >= 12 && peakHour < 17) {
            timeOfDay = "afternoon";
        } else if (peakHour >= 17 && peakHour < 21) {
            timeOfDay = "evening";
        } else {
            timeOfDay = "night";
        }

        return String.format(
                "Analysis of %d collaboration events shows peak activity at %02d:00 (%s). " +
                "Consider scheduling team sessions during this time for maximum participation.",
                totalSnapshots, peakHour, timeOfDay
        );
    }
}
