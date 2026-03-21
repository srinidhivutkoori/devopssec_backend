package com.whiteboard.app.controller;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.service.ElementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for element CRUD operations and lock management.
 * All endpoints are nested under /api/boards/{boardId}/elements to enforce
 * the board-element ownership hierarchy.
 */
@RestController
@RequestMapping("/api/boards/{boardId}/elements")
public class ElementController {

    private final ElementService elementService;

    /**
     * Constructor injection of the element service.
     *
     * @param elementService handles element lifecycle and locking logic
     */
    public ElementController(ElementService elementService) {
        this.elementService = elementService;
    }

    /**
     * Create a new element on the specified board.
     *
     * POST /api/boards/{boardId}/elements
     *
     * @param boardId     the board to add the element to
     * @param request     element creation parameters
     * @param userDetails the authenticated user
     * @return 201 Created with the new element's details
     */
    @PostMapping
    public ResponseEntity<ElementResponse> createElement(
            @PathVariable Long boardId,
            @Valid @RequestBody CreateElementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ElementResponse response = elementService.createElement(boardId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve all elements on a board ordered by z-index.
     *
     * GET /api/boards/{boardId}/elements
     *
     * @param boardId the board whose elements to retrieve
     * @return 200 OK with list of elements, bottom layer first
     */
    @GetMapping
    public ResponseEntity<List<ElementResponse>> getElementsByBoard(@PathVariable Long boardId) {
        List<ElementResponse> elements = elementService.getElementsByBoard(boardId);
        return ResponseEntity.ok(elements);
    }

    /**
     * Retrieve a single element by its ID.
     *
     * GET /api/boards/{boardId}/elements/{elementId}
     *
     * @param boardId   the board the element belongs to
     * @param elementId the element's database ID
     * @return 200 OK with element details, or 404 if not found
     */
    @GetMapping("/{elementId}")
    public ResponseEntity<ElementResponse> getElementById(
            @PathVariable Long boardId,
            @PathVariable Long elementId) {
        ElementResponse response = elementService.getElementById(boardId, elementId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing element's properties (partial update).
     *
     * PUT /api/boards/{boardId}/elements/{elementId}
     *
     * @param boardId     the board the element belongs to
     * @param elementId   the element to update
     * @param request     fields to update
     * @param userDetails the authenticated user
     * @return 200 OK with updated element details
     */
    @PutMapping("/{elementId}")
    public ResponseEntity<ElementResponse> updateElement(
            @PathVariable Long boardId,
            @PathVariable Long elementId,
            @Valid @RequestBody UpdateElementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ElementResponse response = elementService.updateElement(
                boardId, elementId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an element from the board.
     *
     * DELETE /api/boards/{boardId}/elements/{elementId}
     *
     * @param boardId     the board the element belongs to
     * @param elementId   the element to delete
     * @param userDetails the authenticated user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{elementId}")
    public ResponseEntity<Void> deleteElement(
            @PathVariable Long boardId,
            @PathVariable Long elementId,
            @AuthenticationPrincipal UserDetails userDetails) {
        elementService.deleteElement(boardId, elementId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Acquire an edit lock on an element.
     * Prevents other users from modifying the element while it is locked.
     *
     * PUT /api/boards/{boardId}/elements/{elementId}/lock
     *
     * @param boardId     the board the element belongs to
     * @param elementId   the element to lock
     * @param userDetails the authenticated user requesting the lock
     * @return 200 OK with the element showing its locked state
     */
    @PutMapping("/{elementId}/lock")
    public ResponseEntity<ElementResponse> lockElement(
            @PathVariable Long boardId,
            @PathVariable Long elementId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ElementResponse response = elementService.lockElement(boardId, elementId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Release the edit lock on an element.
     * Only the user holding the lock can release it.
     *
     * PUT /api/boards/{boardId}/elements/{elementId}/unlock
     *
     * @param boardId     the board the element belongs to
     * @param elementId   the element to unlock
     * @param userDetails the authenticated user releasing the lock
     * @return 200 OK with the element showing its unlocked state
     */
    @PutMapping("/{elementId}/unlock")
    public ResponseEntity<ElementResponse> unlockElement(
            @PathVariable Long boardId,
            @PathVariable Long elementId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ElementResponse response = elementService.unlockElement(boardId, elementId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
