package com.whiteboard.app.service;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.exception.ResourceNotFoundException;
import com.whiteboard.app.model.Board;
import com.whiteboard.app.model.Element;
import com.whiteboard.app.model.PermissionLevel;
import com.whiteboard.app.model.User;
import com.whiteboard.app.repository.ElementRepository;
import com.whiteboard.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling element CRUD operations and lock management.
 * Elements are the visual building blocks placed on a whiteboard canvas.
 * Locking prevents concurrent edit conflicts during real-time collaboration.
 *
 * Lock semantics:
 * - A user must lock an element before editing it
 * - Only the lock holder can unlock or modify a locked element
 * - Locks are released when the user unlocks explicitly or disconnects
 */
@Service
@Slf4j
public class ElementService {

    private final ElementRepository elementRepository;
    private final UserRepository userRepository;
    private final BoardService boardService;
    private final UserService userService;
    private final AccessPermissionService accessPermissionService;

    public ElementService(ElementRepository elementRepository,
                          UserRepository userRepository,
                          BoardService boardService,
                          UserService userService,
                          AccessPermissionService accessPermissionService) {
        this.elementRepository = elementRepository;
        this.userRepository = userRepository;
        this.boardService = boardService;
        this.userService = userService;
        this.accessPermissionService = accessPermissionService;
    }

    /**
     * Create a new element on the specified board.
     * If zIndex is not provided, the element is placed on top of all existing elements.
     *
     * @param boardId  the board to add the element to
     * @param request  element creation parameters
     * @param username the authenticated user creating the element
     * @return the created element as a response DTO
     */
    @Transactional
    public ElementResponse createElement(Long boardId, CreateElementRequest request, String username) {
        Board board = boardService.findBoardOrThrow(boardId);
        assertEditPermission(board, username);

        // Auto-assign zIndex if not specified: place on top of all existing elements
        int zIndex = request.getZIndex() != null
                ? request.getZIndex()
                : getNextZIndex(boardId);

        Element element = Element.builder()
                .board(board)
                .type(request.getType())
                .x(request.getX())
                .y(request.getY())
                .width(request.getWidth())
                .height(request.getHeight())
                .content(request.getContent())
                .style(request.getStyle())
                .zIndex(zIndex)
                .locked(false)
                .build();

        Element saved = elementRepository.save(element);
        log.info("Element created: id={} type={} on board={} by user='{}'",
                saved.getId(), saved.getType(), boardId, username);
        return mapToResponse(saved);
    }

    /**
     * Retrieve all elements belonging to a board, ordered by z-index.
     *
     * @param boardId the board's database ID
     * @return list of elements sorted bottom-to-top by z-index
     */
    @Transactional(readOnly = true)
    public List<ElementResponse> getElementsByBoard(Long boardId) {
        // Verify the board exists before querying elements
        boardService.findBoardOrThrow(boardId);
        return elementRepository.findByBoardIdOrderByZIndexAsc(boardId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a single element by its ID.
     *
     * @param boardId   the board the element belongs to (used for validation)
     * @param elementId the element's database ID
     * @return the element as a response DTO
     */
    @Transactional(readOnly = true)
    public ElementResponse getElementById(Long boardId, Long elementId) {
        Element element = findElementOrThrow(elementId);
        validateElementBelongsToBoard(element, boardId);
        return mapToResponse(element);
    }

    /**
     * Update an existing element's properties.
     * Only non-null fields in the request are applied.
     * The element must not be locked by another user.
     *
     * @param boardId   the board the element belongs to
     * @param elementId the element to update
     * @param request   fields to update
     * @param username  the authenticated user making the update
     * @return the updated element as a response DTO
     * @throws IllegalStateException if the element is locked by another user
     */
    @Transactional
    public ElementResponse updateElement(Long boardId, Long elementId,
                                         UpdateElementRequest request, String username) {
        Element element = findElementOrThrow(elementId);
        validateElementBelongsToBoard(element, boardId);
        assertEditPermission(element.getBoard(), username);
        assertNotLockedByOther(element, username);

        // Apply only provided (non-null) fields
        if (request.getX() != null)       element.setX(request.getX());
        if (request.getY() != null)       element.setY(request.getY());
        if (request.getWidth() != null)   element.setWidth(request.getWidth());
        if (request.getHeight() != null)  element.setHeight(request.getHeight());
        if (request.getContent() != null) element.setContent(request.getContent());
        if (request.getStyle() != null)   element.setStyle(request.getStyle());
        if (request.getZIndex() != null)  element.setZIndex(request.getZIndex());

        Element updated = elementRepository.save(element);
        log.debug("Element updated: id={} on board={} by user='{}'", elementId, boardId, username);
        return mapToResponse(updated);
    }

    /**
     * Delete an element from the board.
     * The element must not be locked by another user.
     *
     * @param boardId   the board the element belongs to
     * @param elementId the element to delete
     * @param username  the authenticated user requesting deletion
     */
    @Transactional
    public void deleteElement(Long boardId, Long elementId, String username) {
        Element element = findElementOrThrow(elementId);
        validateElementBelongsToBoard(element, boardId);
        assertEditPermission(element.getBoard(), username);
        assertNotLockedByOther(element, username);
        elementRepository.delete(element);
        log.info("Element deleted: id={} from board={} by user='{}'", elementId, boardId, username);
    }

    /**
     * Acquire a lock on an element for the requesting user.
     * Locks prevent other users from modifying the element concurrently.
     *
     * @param boardId   the board the element belongs to
     * @param elementId the element to lock
     * @param username  the user requesting the lock
     * @return the updated element showing the lock state
     * @throws IllegalStateException if the element is already locked by another user
     */
    @Transactional
    public ElementResponse lockElement(Long boardId, Long elementId, String username) {
        Element element = findElementOrThrow(elementId);
        validateElementBelongsToBoard(element, boardId);
        assertEditPermission(element.getBoard(), username);

        // Check if another user holds the lock
        if (Boolean.TRUE.equals(element.getLocked()) &&
                !element.getLockedBy().getUsername().equals(username)) {
            throw new IllegalStateException(
                    "Element " + elementId + " is already locked by user: " +
                    element.getLockedBy().getUsername());
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        element.setLocked(true);
        element.setLockedBy(user);

        Element saved = elementRepository.save(element);
        log.debug("Element locked: id={} by user='{}'", elementId, username);
        return mapToResponse(saved);
    }

    /**
     * Release the lock on an element.
     * Only the user currently holding the lock may release it.
     *
     * @param boardId   the board the element belongs to
     * @param elementId the element to unlock
     * @param username  the user releasing the lock
     * @return the updated element showing the unlocked state
     * @throws IllegalStateException if the element is not locked or locked by a different user
     */
    @Transactional
    public ElementResponse unlockElement(Long boardId, Long elementId, String username) {
        Element element = findElementOrThrow(elementId);
        validateElementBelongsToBoard(element, boardId);

        // Only the lock holder can release the lock
        if (Boolean.TRUE.equals(element.getLocked()) &&
                !element.getLockedBy().getUsername().equals(username)) {
            throw new IllegalStateException(
                    "Only the user who locked this element can unlock it");
        }

        element.setLocked(false);
        element.setLockedBy(null);

        Element saved = elementRepository.save(element);
        log.debug("Element unlocked: id={} by user='{}'", elementId, username);
        return mapToResponse(saved);
    }

    /**
     * Calculate the next available z-index for a new element on the board.
     * Places the new element on top of all existing elements.
     *
     * @param boardId the board to query
     * @return the next z-index value (max + 1, or 0 if no elements exist)
     */
    private int getNextZIndex(Long boardId) {
        Integer maxZIndex = elementRepository.findMaxZIndexByBoardId(boardId);
        return maxZIndex != null ? maxZIndex + 1 : 0;
    }

    /**
     * Fetch an element entity or throw ResourceNotFoundException.
     *
     * @param elementId the element ID to look up
     * @return the element entity
     */
    private Element findElementOrThrow(Long elementId) {
        return elementRepository.findById(elementId)
                .orElseThrow(() -> new ResourceNotFoundException("Element", "id", elementId));
    }

    /**
     * Validate that the element actually belongs to the given board.
     * Prevents cross-board element access via URL manipulation.
     *
     * @param element the element to check
     * @param boardId the expected board ID
     * @throws IllegalArgumentException if the element belongs to a different board
     */
    private void validateElementBelongsToBoard(Element element, Long boardId) {
        if (!element.getBoard().getId().equals(boardId)) {
            throw new IllegalArgumentException(
                    "Element " + element.getId() + " does not belong to board " + boardId);
        }
    }

    /**
     * Assert that the element is not currently locked by a different user.
     *
     * @param element  the element to check
     * @param username the current user's username
     * @throws IllegalStateException if the element is locked by someone else
     */
    /**
     * Assert that the user has at least EDIT permission on the board.
     * Board owners always have edit access.
     */
    private void assertEditPermission(Board board, String username) {
        // Board owner always has full access
        if (board.getOwner().getUsername().equals(username)) {
            return;
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        if (!accessPermissionService.hasPermission(board.getId(), user.getId(), PermissionLevel.EDIT)) {
            throw new IllegalStateException(
                    "You have view-only access to this board and cannot modify elements");
        }
    }

    private void assertNotLockedByOther(Element element, String username) {
        if (Boolean.TRUE.equals(element.getLocked()) &&
                !element.getLockedBy().getUsername().equals(username)) {
            throw new IllegalStateException(
                    "Element is locked by user: " + element.getLockedBy().getUsername());
        }
    }

    /**
     * Map an Element entity to its response DTO.
     *
     * @param element the entity to map
     * @return the response DTO
     */
    public ElementResponse mapToResponse(Element element) {
        return ElementResponse.builder()
                .id(element.getId())
                .boardId(element.getBoard().getId())
                .type(element.getType())
                .x(element.getX())
                .y(element.getY())
                .width(element.getWidth())
                .height(element.getHeight())
                .content(element.getContent())
                .style(element.getStyle())
                .zIndex(element.getZIndex())
                .locked(element.getLocked())
                .lockedBy(element.getLockedBy() != null
                        ? userService.mapToResponse(element.getLockedBy())
                        : null)
                .createdAt(element.getCreatedAt())
                .updatedAt(element.getUpdatedAt())
                .build();
    }
}
