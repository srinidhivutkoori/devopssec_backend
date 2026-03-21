package com.whiteboard.app.service;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.exception.ResourceNotFoundException;
import com.whiteboard.app.model.Board;
import com.whiteboard.app.model.PermissionLevel;
import com.whiteboard.app.model.User;
import com.whiteboard.app.repository.AccessPermissionRepository;
import com.whiteboard.app.repository.BoardRepository;
import com.whiteboard.app.repository.ElementRepository;
import com.whiteboard.app.repository.UserRepository;
import com.whiteboard.app.repository.VersionSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling all board lifecycle operations.
 * Boards are the top-level canvas containers owned by a user.
 * Access control checks are performed before any mutating operation.
 */
@Service
@Slf4j
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ElementRepository elementRepository;
    private final AccessPermissionRepository permissionRepository;
    private final VersionSnapshotRepository snapshotRepository;
    private final ActivityLogService activityLogService;

    public BoardService(BoardRepository boardRepository,
                        UserRepository userRepository,
                        UserService userService,
                        ElementRepository elementRepository,
                        AccessPermissionRepository permissionRepository,
                        VersionSnapshotRepository snapshotRepository,
                        ActivityLogService activityLogService) {
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.elementRepository = elementRepository;
        this.permissionRepository = permissionRepository;
        this.snapshotRepository = snapshotRepository;
        this.activityLogService = activityLogService;
    }

    /**
     * Create a new board owned by the given user.
     * Default background color is white (#FFFFFF) if not specified.
     *
     * @param request   board creation parameters
     * @param ownerUsername the authenticated user who will own the board
     * @return the created board as a response DTO
     */
    @Transactional
    public BoardResponse createBoard(CreateBoardRequest request, String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", ownerUsername));

        // Default background color to white when not provided
        String bgColor = request.getBackgroundColor() != null
                ? request.getBackgroundColor()
                : "#FFFFFF";

        Board board = Board.builder()
                .name(request.getName())
                .width(request.getWidth())
                .height(request.getHeight())
                .backgroundColor(bgColor)
                .owner(owner)
                .build();

        Board saved = boardRepository.save(board);
        log.info("Board created: id={} name='{}' by user='{}'", saved.getId(), saved.getName(), ownerUsername);
        activityLogService.logActivity("BOARD_CREATED",
                "Created board '" + saved.getName() + "'", owner, saved);
        return mapToResponse(saved);
    }

    /**
     * Retrieve a single board by its ID, including the requesting user's permission level.
     *
     * @param boardId  the board's database ID
     * @param username the authenticated user requesting the board
     * @return the board as a response DTO with permissionLevel set
     * @throws ResourceNotFoundException if no board with that ID exists
     */
    @Transactional(readOnly = true)
    public BoardResponse getBoardById(Long boardId, String username) {
        Board board = findBoardOrThrow(boardId);
        BoardResponse response = mapToResponse(board);
        response.setPermissionLevel(resolvePermissionLevel(board, username));
        return response;
    }

    /**
     * Retrieve all boards owned by a specific user.
     *
     * @param username the owner's username
     * @return list of boards owned by the user, newest first
     */
    @Transactional(readOnly = true)
    public List<BoardResponse> getBoardsByOwner(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return boardRepository.findByOwnerOrderByCreatedAtDesc(owner)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all boards accessible to a user (owned + shared via permissions).
     *
     * @param userId the user's database ID
     * @return combined list of owned and shared boards
     */
    @Transactional(readOnly = true)
    public List<BoardResponse> getAllAccessibleBoards(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Long userId = owner.getId();

        // Fetch boards the user owns directly
        List<Board> ownedBoards = boardRepository.findByOwnerOrderByCreatedAtDesc(owner);

        // Fetch boards accessible via direct user permissions
        List<Board> permittedBoards = boardRepository.findBoardsAccessibleByUser(userId);

        // Fetch boards accessible via team membership
        List<Board> teamBoards = boardRepository.findBoardsAccessibleByTeamMember(userId);

        // Merge all boards, eliminating duplicates by ID
        return java.util.stream.Stream.of(ownedBoards, permittedBoards, teamBoards)
                .flatMap(List::stream)
                .collect(Collectors.toMap(Board::getId, b -> b, (a, b) -> a))
                .values()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update a board's metadata. Only the board owner may update it.
     * Only non-null fields in the request are applied (partial update).
     *
     * @param boardId       the board to update
     * @param request       fields to update
     * @param currentUsername the authenticated user requesting the update
     * @return the updated board as a response DTO
     * @throws IllegalArgumentException  if the user is not the board owner
     */
    @Transactional
    public BoardResponse updateBoard(Long boardId, UpdateBoardRequest request, String currentUsername) {
        Board board = findBoardOrThrow(boardId);
        assertOwner(board, currentUsername);

        // Apply only the fields that were provided in the request
        if (request.getName() != null) {
            board.setName(request.getName());
        }
        if (request.getWidth() != null) {
            board.setWidth(request.getWidth());
        }
        if (request.getHeight() != null) {
            board.setHeight(request.getHeight());
        }
        if (request.getBackgroundColor() != null) {
            board.setBackgroundColor(request.getBackgroundColor());
        }

        Board updated = boardRepository.save(board);
        log.info("Board updated: id={} by user='{}'", boardId, currentUsername);
        activityLogService.logActivity("BOARD_UPDATED",
                "Updated board '" + updated.getName() + "'", board.getOwner(), updated);
        return mapToResponse(updated);
    }

    /**
     * Delete a board and all its associated data.
     * Only the board owner may delete it.
     *
     * @param boardId         the board to delete
     * @param currentUsername the authenticated user requesting deletion
     * @throws IllegalArgumentException if the user is not the board owner
     */
    @Transactional
    public void deleteBoard(Long boardId, String currentUsername) {
        Board board = findBoardOrThrow(boardId);
        assertOwner(board, currentUsername);
        // Delete child records before the board to avoid FK constraint violations
        elementRepository.deleteAllByBoardId(boardId);
        permissionRepository.deleteAllByBoardId(boardId);
        snapshotRepository.deleteAllByBoardId(boardId);
        activityLogService.logActivity("BOARD_DELETED",
                "Deleted board '" + board.getName() + "'", board.getOwner(), null);
        boardRepository.delete(board);
        log.info("Board deleted: id={} by user='{}'", boardId, currentUsername);
    }

    /**
     * Fetch a board entity or throw ResourceNotFoundException.
     *
     * @param boardId the board ID to look up
     * @return the board entity
     */
    public Board findBoardOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", "id", boardId));
    }

    /**
     * Assert that the current user is the owner of the board.
     *
     * @param board           the board entity
     * @param currentUsername the authenticated user's username
     * @throws IllegalArgumentException if the user is not the owner
     */
    private void assertOwner(Board board, String currentUsername) {
        if (!board.getOwner().getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("Only the board owner can perform this action");
        }
    }

    /**
     * Resolve the effective permission level a user has on a board.
     * Owner gets "OWNER", otherwise checks direct + team permissions for the highest level.
     *
     * @param board    the board entity
     * @param username the user whose permission to resolve
     * @return "OWNER", "ADMIN", "EDIT", or "VIEW"
     */
    public String resolvePermissionLevel(Board board, String username) {
        if (board.getOwner().getUsername().equals(username)) {
            return "OWNER";
        }
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "VIEW";

        // Check for the highest permission: ADMIN > EDIT > VIEW
        Long userId = user.getId();
        for (PermissionLevel level : List.of(PermissionLevel.ADMIN, PermissionLevel.EDIT, PermissionLevel.VIEW)) {
            boolean hasDirect = permissionRepository.hasUserPermission(board.getId(), userId, List.of(level));
            boolean hasTeam = permissionRepository.hasTeamPermission(board.getId(), userId, List.of(level));
            if (hasDirect || hasTeam) {
                return level.name();
            }
        }
        return "VIEW";
    }

    /**
     * Map a Board entity to a BoardResponse DTO.
     *
     * @param board the entity to map
     * @return the response DTO
     */
    public BoardResponse mapToResponse(Board board) {
        return BoardResponse.builder()
                .id(board.getId())
                .name(board.getName())
                .width(board.getWidth())
                .height(board.getHeight())
                .backgroundColor(board.getBackgroundColor())
                .owner(userService.mapToResponse(board.getOwner()))
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }
}
