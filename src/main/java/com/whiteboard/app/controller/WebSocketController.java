package com.whiteboard.app.controller;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.service.ElementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket controller for real-time collaborative whiteboard interactions.
 * Handles STOMP messages from clients and broadcasts element change events
 * to all subscribers watching the same board.
 *
 * Message flow:
 *   1. Client sends to: /app/board/{boardId}/action
 *   2. This controller processes the action via ElementService
 *   3. Result is broadcast to: /topic/board/{boardId}
 *   4. All connected clients on that board receive the update
 *
 * Supported actions: CREATE, UPDATE, DELETE, LOCK, UNLOCK, CURSOR_MOVE
 */
@Controller
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ElementService elementService;

    /**
     * Constructor injection of the messaging template and element service.
     *
     * @param messagingTemplate used to broadcast messages to topic subscribers
     * @param elementService    applies element operations (update, lock, unlock)
     */
    public WebSocketController(SimpMessagingTemplate messagingTemplate,
                                ElementService elementService) {
        this.messagingTemplate = messagingTemplate;
        this.elementService = elementService;
    }

    /**
     * Handle real-time element actions from connected clients.
     * Routes the action to the appropriate service method based on the action type,
     * then broadcasts the result to all subscribers on the board's topic channel.
     *
     * Clients send to: /app/board/{boardId}/action
     * Result is broadcast to: /topic/board/{boardId}
     *
     * @param boardId   the board this action applies to (from the destination URL)
     * @param message   the whiteboard message payload containing action type and data
     * @param principal the authenticated WebSocket user (derived from JWT handshake)
     */
    @MessageMapping("/board/{boardId}/action")
    public void handleBoardAction(
            @DestinationVariable Long boardId,
            @Payload WhiteboardMessage message,
            Principal principal) {

        // Use the authenticated principal username, not the client-supplied one (security)
        String username = principal != null ? principal.getName() : message.getUsername();
        message.setUsername(username);
        message.setBoardId(boardId);

        log.debug("WebSocket action: {} on board={} by user='{}'",
                message.getAction(), boardId, username);

        try {
            // Route the message to the correct service operation based on action type
            switch (message.getAction().toUpperCase()) {
                case "CREATE" -> {
                    // Element creation is handled via REST; this broadcasts an existing element
                    broadcastToBoard(boardId, message);
                }
                case "UPDATE" -> {
                    // Apply the update via the service to ensure validation and persistence
                    if (message.getElementId() != null && message.getElementData() != null) {
                        UpdateElementRequest updateReq = buildUpdateRequest(message.getElementData());
                        ElementResponse updated = elementService.updateElement(
                                boardId, message.getElementId(), updateReq, username);
                        message.setElementData(updated);
                    }
                    broadcastToBoard(boardId, message);
                }
                case "DELETE" -> {
                    if (message.getElementId() != null) {
                        elementService.deleteElement(boardId, message.getElementId(), username);
                    }
                    broadcastToBoard(boardId, message);
                }
                case "LOCK" -> {
                    if (message.getElementId() != null) {
                        ElementResponse locked = elementService.lockElement(
                                boardId, message.getElementId(), username);
                        message.setElementData(locked);
                    }
                    broadcastToBoard(boardId, message);
                }
                case "UNLOCK" -> {
                    if (message.getElementId() != null) {
                        ElementResponse unlocked = elementService.unlockElement(
                                boardId, message.getElementId(), username);
                        message.setElementData(unlocked);
                    }
                    broadcastToBoard(boardId, message);
                }
                case "CURSOR_MOVE" -> {
                    // Cursor positions are not persisted - just broadcast to other clients
                    broadcastToBoard(boardId, message);
                }
                default -> {
                    log.warn("Unknown WebSocket action '{}' from user='{}' on board={}",
                            message.getAction(), username, boardId);
                    sendErrorToUser(username, boardId, "Unknown action: " + message.getAction());
                }
            }
        } catch (Exception e) {
            // Broadcast an error back to the sender so the UI can show feedback
            log.error("Error processing WebSocket action '{}' on board={}: {}",
                    message.getAction(), boardId, e.getMessage());
            sendErrorToUser(username, boardId, e.getMessage());
        }
    }

    /**
     * Broadcast a whiteboard message to all clients subscribed to a board's topic.
     * The destination follows the pattern /topic/board/{boardId}.
     *
     * @param boardId the board whose subscribers should receive the message
     * @param message the message to broadcast
     */
    private void broadcastToBoard(Long boardId, WhiteboardMessage message) {
        String destination = "/topic/board/" + boardId;
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Broadcast {} to {}", message.getAction(), destination);
    }

    /**
     * Send an error message to a specific user's queue.
     * Used to notify only the sender when their action fails.
     *
     * @param username the recipient's username
     * @param boardId  the board context for the error
     * @param errorMsg the error description
     */
    private void sendErrorToUser(String username, Long boardId, String errorMsg) {
        WhiteboardMessage errorMessage = WhiteboardMessage.builder()
                .action("ERROR")
                .boardId(boardId)
                .username(username)
                .message(errorMsg)
                .build();
        messagingTemplate.convertAndSendToUser(username, "/queue/errors", errorMessage);
    }

    /**
     * Build an UpdateElementRequest from an ElementResponse payload.
     * Only non-null fields are included to preserve partial update semantics.
     *
     * @param elementData the element response from the client
     * @return an UpdateElementRequest with populated fields
     */
    private UpdateElementRequest buildUpdateRequest(ElementResponse elementData) {
        return UpdateElementRequest.builder()
                .x(elementData.getX())
                .y(elementData.getY())
                .width(elementData.getWidth())
                .height(elementData.getHeight())
                .content(elementData.getContent())
                .style(elementData.getStyle())
                .zIndex(elementData.getZIndex())
                .build();
    }
}
