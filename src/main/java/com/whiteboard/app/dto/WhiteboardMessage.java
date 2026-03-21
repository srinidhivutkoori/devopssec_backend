package com.whiteboard.app.dto;

import lombok.*;

/**
 * DTO representing a real-time WebSocket message for whiteboard collaboration.
 * Sent by clients when they create, update, delete, lock, or unlock elements.
 * The server validates the action, applies it, and broadcasts the result
 * to all connected clients subscribed to the same board topic.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhiteboardMessage {

    /**
     * The type of action being performed on the element.
     * Possible values: CREATE, UPDATE, DELETE, LOCK, UNLOCK, CURSOR_MOVE
     */
    private String action;

    /**
     * The ID of the board this action applies to.
     * Used to route the broadcast to the correct board topic.
     */
    private Long boardId;

    /**
     * The ID of the element being acted upon.
     * Null for CREATE actions (element doesn't exist yet).
     */
    private Long elementId;

    /**
     * The username of the user performing the action.
     * Used for attribution and lock holder display.
     */
    private String username;

    /**
     * The element data payload for CREATE and UPDATE actions.
     * Serialized as JSON to be applied directly to the canvas.
     */
    private ElementResponse elementData;

    /**
     * For CURSOR_MOVE actions: the X position of the user's cursor.
     */
    private Double cursorX;

    /**
     * For CURSOR_MOVE actions: the Y position of the user's cursor.
     */
    private Double cursorY;

    /**
     * Optional message or error description for error broadcasts.
     */
    private String message;
}
