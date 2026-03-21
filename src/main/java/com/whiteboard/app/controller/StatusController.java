package com.whiteboard.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for application status endpoint.
 * Provides a simple system status check that returns
 * the application name, version and current server timestamp.
 * This endpoint is publicly accessible (no JWT required).
 */
@RestController
@RequestMapping("/api/status")
public class StatusController {

    /**
     * Returns current application status information.
     *
     * @return JSON with application name, version and server time
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("application", "Collaborative Whiteboard");
        status.put("version", "1.0.0");
        status.put("status", "running");
        status.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }
}
