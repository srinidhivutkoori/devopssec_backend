package com.whiteboard.app.controller;

import com.whiteboard.app.dto.ActivityLogResponse;
import com.whiteboard.app.service.ActivityLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the activity feed.
 * Provides endpoints to retrieve recent activity for boards the user can access.
 */
@RestController
@RequestMapping("/api/activity")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    /**
     * Get recent activity for boards the authenticated user can access.
     * GET /api/activity?limit=50
     */
    @GetMapping
    public ResponseEntity<List<ActivityLogResponse>> getRecentActivity(
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(activityLogService.getRecentActivity(Math.min(limit, 100), userDetails.getUsername()));
    }

    /**
     * Get recent activity for a specific board.
     * GET /api/activity/board/{boardId}?limit=30
     */
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<ActivityLogResponse>> getBoardActivity(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(activityLogService.getBoardActivity(boardId, Math.min(limit, 100)));
    }
}
