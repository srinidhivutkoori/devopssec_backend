package com.whiteboard.app.controller;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.service.VersionSnapshotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for version snapshot operations.
 * Snapshots capture the complete state of a board at a point in time,
 * enabling version history and board state replay.
 *
 * All endpoints are nested under /api/boards/{boardId}/snapshots.
 */
@RestController
@RequestMapping("/api/boards/{boardId}/snapshots")
public class VersionSnapshotController {

    private final VersionSnapshotService snapshotService;

    /**
     * Constructor injection of the snapshot service.
     *
     * @param snapshotService handles snapshot creation and retrieval
     */
    public VersionSnapshotController(VersionSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    /**
     * Create a new snapshot of the board's current state.
     * Captures all elements and their properties at the current moment.
     *
     * POST /api/boards/{boardId}/snapshots
     *
     * @param boardId     the board to snapshot
     * @param request     optional snapshot description
     * @param userDetails the authenticated user creating the snapshot
     * @return 201 Created with the new snapshot metadata
     */
    @PostMapping
    public ResponseEntity<SnapshotResponse> createSnapshot(
            @PathVariable Long boardId,
            @Valid @RequestBody(required = false) CreateSnapshotRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SnapshotResponse response = snapshotService.createSnapshot(
                boardId,
                request != null ? request : new CreateSnapshotRequest(),
                userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve the complete version history for a board.
     * Returns snapshots ordered from newest to oldest.
     *
     * GET /api/boards/{boardId}/snapshots
     *
     * @param boardId the board whose history to retrieve
     * @return 200 OK with list of snapshots, newest first
     */
    @GetMapping
    public ResponseEntity<List<SnapshotResponse>> getSnapshots(@PathVariable Long boardId) {
        List<SnapshotResponse> snapshots = snapshotService.getSnapshotsByBoard(boardId);
        return ResponseEntity.ok(snapshots);
    }

    /**
     * Retrieve a single snapshot by its ID for board state replay.
     * The snapshotData field contains the full JSON state of all elements.
     *
     * GET /api/boards/{boardId}/snapshots/{snapshotId}
     *
     * @param boardId    the board the snapshot belongs to
     * @param snapshotId the snapshot's database ID
     * @return 200 OK with snapshot details including full element state JSON
     */
    @GetMapping("/{snapshotId}")
    public ResponseEntity<SnapshotResponse> getSnapshotById(
            @PathVariable Long boardId,
            @PathVariable Long snapshotId) {
        SnapshotResponse response = snapshotService.getSnapshotById(boardId, snapshotId);
        return ResponseEntity.ok(response);
    }
}
