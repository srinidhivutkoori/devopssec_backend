package com.whiteboard.app.controller;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for whiteboard CRUD operations.
 * All endpoints require JWT authentication.
 * The authenticated user is extracted from the security context for ownership checks.
 */
@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    /**
     * Constructor injection of the board service.
     *
     * @param boardService handles board lifecycle logic
     */
    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /**
     * Create a new whiteboard owned by the authenticated user.
     *
     * POST /api/boards
     *
     * @param request      board creation parameters
     * @param userDetails  the authenticated user (injected by Spring Security)
     * @return 201 Created with the new board's details
     */
    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(
            @Valid @RequestBody CreateBoardRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        BoardResponse response = boardService.createBoard(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve all boards accessible to the authenticated user
     * (owned + shared via direct permissions + shared via team membership).
     *
     * GET /api/boards
     *
     * @param userDetails the authenticated user
     * @return 200 OK with list of accessible boards
     */
    @GetMapping
    public ResponseEntity<List<BoardResponse>> getMyBoards(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<BoardResponse> boards = boardService.getAllAccessibleBoards(userDetails.getUsername());
        return ResponseEntity.ok(boards);
    }

    /**
     * Retrieve a single board by its ID.
     *
     * GET /api/boards/{boardId}
     *
     * @param boardId the board's database ID
     * @return 200 OK with the board details, or 404 if not found
     */
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoardById(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        BoardResponse response = boardService.getBoardById(boardId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Update a board's metadata.
     * Only the board owner can perform this operation.
     *
     * PUT /api/boards/{boardId}
     *
     * @param boardId     the board to update
     * @param request     fields to update (all optional)
     * @param userDetails the authenticated user
     * @return 200 OK with updated board details
     */
    @PutMapping("/{boardId}")
    public ResponseEntity<BoardResponse> updateBoard(
            @PathVariable Long boardId,
            @Valid @RequestBody UpdateBoardRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        BoardResponse response = boardService.updateBoard(boardId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a board and all associated data.
     * Only the board owner can delete it.
     *
     * DELETE /api/boards/{boardId}
     *
     * @param boardId     the board to delete
     * @param userDetails the authenticated user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        boardService.deleteBoard(boardId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
