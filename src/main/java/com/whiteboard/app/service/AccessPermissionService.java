package com.whiteboard.app.service;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.exception.ResourceNotFoundException;
import com.whiteboard.app.model.*;
import com.whiteboard.app.repository.AccessPermissionRepository;
import com.whiteboard.app.repository.TeamRepository;
import com.whiteboard.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing access permissions on whiteboards.
 * Permissions can be granted to individual users or entire teams.
 * The three permission levels are: VIEW (read-only), EDIT (modify elements), ADMIN (full control).
 *
 * The board owner always has implicit ADMIN access regardless of permission records.
 */
@Service
@Slf4j
public class AccessPermissionService {

    private final AccessPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final BoardService boardService;
    private final UserService userService;
    private final TeamService teamService;

    /**
     * Constructor injection of all required dependencies.
     *
     * @param permissionRepository data access for permissions
     * @param userRepository       data access for users
     * @param teamRepository       data access for teams
     * @param boardService         validates and fetches board entities
     * @param userService          maps User entities to DTOs
     * @param teamService          maps Team entities to DTOs
     */
    public AccessPermissionService(AccessPermissionRepository permissionRepository,
                                   UserRepository userRepository,
                                   TeamRepository teamRepository,
                                   BoardService boardService,
                                   UserService userService,
                                   TeamService teamService) {
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.boardService = boardService;
        this.userService = userService;
        this.teamService = teamService;
    }

    /**
     * Grant a new permission on a board to a user or team.
     * Either userId or teamId must be provided in the request.
     *
     * @param boardId the board to grant permission on
     * @param request permission details (target user/team and level)
     * @return the created permission as a response DTO
     * @throws IllegalArgumentException if neither userId nor teamId is provided,
     *                                   or if a permission already exists for this target
     */
    @Transactional
    public PermissionResponse createPermission(Long boardId, CreatePermissionRequest request) {
        Board board = boardService.findBoardOrThrow(boardId);

        // Resolve username to userId if provided
        if (request.getUsername() != null && !request.getUsername().isBlank() && request.getUserId() == null) {
            User resolvedUser = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));
            request.setUserId(resolvedUser.getId());
        }

        // Validate that exactly one of userId or teamId is provided
        if (request.getUserId() == null && request.getTeamId() == null) {
            throw new IllegalArgumentException("Either userId or teamId must be provided");
        }

        AccessPermission permission = AccessPermission.builder()
                .board(board)
                .permissionLevel(request.getPermissionLevel())
                .build();

        if (request.getUserId() != null) {
            // Check for duplicate user permission
            if (permissionRepository.findByBoardIdAndUserId(boardId, request.getUserId()).isPresent()) {
                throw new IllegalArgumentException(
                        "User already has a permission on this board. Use update to change it.");
            }
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
            permission.setUser(user);
        } else {
            // Check for duplicate team permission
            if (permissionRepository.findByBoardIdAndTeamId(boardId, request.getTeamId()).isPresent()) {
                throw new IllegalArgumentException(
                        "Team already has a permission on this board. Use update to change it.");
            }
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getTeamId()));
            permission.setTeam(team);
        }

        AccessPermission saved = permissionRepository.save(permission);
        log.info("Permission granted: board={} level={} user={} team={}",
                boardId, request.getPermissionLevel(), request.getUserId(), request.getTeamId());
        return mapToResponse(saved);
    }

    /**
     * Retrieve all permissions for a specific board.
     *
     * @param boardId the board whose permissions to list
     * @return list of all permission records on the board
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissionsByBoard(Long boardId) {
        boardService.findBoardOrThrow(boardId); // Validate board exists
        return permissionRepository.findByBoardId(boardId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a single permission record by its ID.
     *
     * @param permissionId the permission record ID
     * @return the permission as a response DTO
     */
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(Long permissionId) {
        AccessPermission permission = findPermissionOrThrow(permissionId);
        return mapToResponse(permission);
    }

    /**
     * Update the permission level for an existing permission record.
     *
     * @param permissionId the permission to update
     * @param request      the new permission level
     * @return the updated permission as a response DTO
     */
    @Transactional
    public PermissionResponse updatePermission(Long permissionId, UpdatePermissionRequest request) {
        AccessPermission permission = findPermissionOrThrow(permissionId);
        permission.setPermissionLevel(request.getPermissionLevel());
        AccessPermission updated = permissionRepository.save(permission);
        log.info("Permission updated: id={} new level={}", permissionId, request.getPermissionLevel());
        return mapToResponse(updated);
    }

    /**
     * Revoke (delete) a permission record.
     *
     * @param permissionId the permission to revoke
     */
    @Transactional
    public void deletePermission(Long permissionId) {
        AccessPermission permission = findPermissionOrThrow(permissionId);
        permissionRepository.delete(permission);
        log.info("Permission revoked: id={}", permissionId);
    }

    /**
     * Check whether a user has at least the given permission level on a board.
     * Checks both direct user permissions and team membership permissions.
     * The board owner always has ADMIN access.
     *
     * @param boardId         the board to check
     * @param userId          the user to check
     * @param permissionLevel the minimum required level
     * @return true if the user has sufficient access
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(Long boardId, Long userId, PermissionLevel permissionLevel) {
        // Build a list of acceptable permission levels (the requested level and higher)
        List<PermissionLevel> acceptedLevels = getAcceptedLevels(permissionLevel);

        // Check direct user permission
        boolean hasUserPerm = permissionRepository.hasUserPermission(boardId, userId, acceptedLevels);

        // Check team membership permission
        boolean hasTeamPerm = permissionRepository.hasTeamPermission(boardId, userId, acceptedLevels);

        return hasUserPerm || hasTeamPerm;
    }

    /**
     * Get the hierarchy of permission levels at or above the given level.
     * E.g., requesting VIEW returns [VIEW, EDIT, ADMIN] since higher levels include lower access.
     *
     * @param minLevel the minimum required level
     * @return list of levels that satisfy the minimum requirement
     */
    private List<PermissionLevel> getAcceptedLevels(PermissionLevel minLevel) {
        return switch (minLevel) {
            case VIEW -> Arrays.asList(PermissionLevel.VIEW, PermissionLevel.EDIT, PermissionLevel.ADMIN);
            case EDIT -> Arrays.asList(PermissionLevel.EDIT, PermissionLevel.ADMIN);
            case ADMIN -> List.of(PermissionLevel.ADMIN);
        };
    }

    /**
     * Fetch a permission entity or throw ResourceNotFoundException.
     *
     * @param permissionId the permission ID to look up
     * @return the permission entity
     */
    private AccessPermission findPermissionOrThrow(Long permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("AccessPermission", "id", permissionId));
    }

    /**
     * Map an AccessPermission entity to its response DTO.
     *
     * @param permission the entity to map
     * @return the response DTO
     */
    public PermissionResponse mapToResponse(AccessPermission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .boardId(permission.getBoard().getId())
                .user(permission.getUser() != null
                        ? userService.mapToResponse(permission.getUser())
                        : null)
                .team(permission.getTeam() != null
                        ? teamService.mapToResponse(permission.getTeam())
                        : null)
                .permissionLevel(permission.getPermissionLevel())
                .createdAt(permission.getCreatedAt())
                .build();
    }
}
