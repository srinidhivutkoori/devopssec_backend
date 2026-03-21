package com.whiteboard.app.dto;

import com.whiteboard.app.model.ElementType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO returned when an element is fetched from the API.
 * Includes all element properties and lock status information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElementResponse {

    /** Unique database identifier for the element. */
    private Long id;

    /** The ID of the board this element belongs to. */
    private Long boardId;

    /** The type of this element. */
    private ElementType type;

    /** X-coordinate on the canvas. */
    private Double x;

    /** Y-coordinate on the canvas. */
    private Double y;

    /** Width of the element's bounding box. */
    private Double width;

    /** Height of the element's bounding box. */
    private Double height;

    /** Text or path content of the element. */
    private String content;

    /** JSON style string with visual properties. */
    private String style;

    /** Stacking order - higher values render on top. */
    private Integer zIndex;

    /** Whether this element is currently locked for editing by another user. */
    private Boolean locked;

    /** Basic information about the user holding the lock, or null if unlocked. */
    private UserResponse lockedBy;

    /** When this element was first created. */
    private LocalDateTime createdAt;

    /** When this element was last modified. */
    private LocalDateTime updatedAt;
}
