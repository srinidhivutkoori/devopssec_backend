package com.whiteboard.app.controller;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.service.AccessPermissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing board access permissions.
 * Permissions are nested under /api/boards/{boardId}/permissions
 * to keep them scoped to their parent board.
 * All endpoints require JWT authentication.
 */
@RestController
@RequestMapping("/api/boards/{boardId}/permissions")
public class AccessPermissionController {

    private final AccessPermissionService permissionService;

    /**
     * Constructor injection of the permission service.
     *
     * @param permissionService handles permission grant, revoke, and check logic
     */
    public AccessPermissionController(AccessPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Grant a new permission on a board to a user or team.
     *
     * POST /api/boards/{boardId}/permissions
     *
     * @param boardId the board to grant permission on
     * @param request permission details (userId or teamId + permissionLevel)
     * @return 201 Created with the new permission record
     */
    @PostMapping
    public ResponseEntity<PermissionResponse> createPermission(
            @PathVariable Long boardId,
            @Valid @RequestBody CreatePermissionRequest request) {
        PermissionResponse response = permissionService.createPermission(boardId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve all permissions for a specific board.
     *
     * GET /api/boards/{boardId}/permissions
     *
     * @param boardId the board whose permissions to list
     * @return 200 OK with list of all permission records
     */
    @GetMapping
    public ResponseEntity<List<PermissionResponse>> getPermissionsByBoard(@PathVariable Long boardId) {
        List<PermissionResponse> permissions = permissionService.getPermissionsByBoard(boardId);
        return ResponseEntity.ok(permissions);
    }

    /**
     * Retrieve a single permission record by its ID.
     *
     * GET /api/boards/{boardId}/permissions/{permissionId}
     *
     * @param boardId      the board the permission belongs to
     * @param permissionId the permission record's database ID
     * @return 200 OK with the permission details
     */
    @GetMapping("/{permissionId}")
    public ResponseEntity<PermissionResponse> getPermissionById(
            @PathVariable Long boardId,
            @PathVariable Long permissionId) {
        PermissionResponse response = permissionService.getPermissionById(permissionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update the permission level of an existing permission record.
     *
     * PUT /api/boards/{boardId}/permissions/{permissionId}
     *
     * @param boardId      the board the permission belongs to
     * @param permissionId the permission to update
     * @param request      the new permission level
     * @return 200 OK with updated permission details
     */
    @PutMapping("/{permissionId}")
    public ResponseEntity<PermissionResponse> updatePermission(
            @PathVariable Long boardId,
            @PathVariable Long permissionId,
            @Valid @RequestBody UpdatePermissionRequest request) {
        PermissionResponse response = permissionService.updatePermission(permissionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke a permission record.
     *
     * DELETE /api/boards/{boardId}/permissions/{permissionId}
     *
     * @param boardId      the board the permission belongs to
     * @param permissionId the permission to revoke
     * @return 204 No Content on success
     */
    @DeleteMapping("/{permissionId}")
    public ResponseEntity<Void> deletePermission(
            @PathVariable Long boardId,
            @PathVariable Long permissionId) {
        permissionService.deletePermission(permissionId);
        return ResponseEntity.noContent().build();
    }
}
