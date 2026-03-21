package com.whiteboard.app.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for updating an existing canvas element.
 * All fields are optional to support partial updates (PATCH-style semantics).
 * Only non-null fields will be applied to the element.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateElementRequest {

    /**
     * Updated X-coordinate. Must be non-negative if provided.
     */
    @DecimalMin(value = "0.0", message = "X coordinate must be non-negative")
    private Double x;

    /**
     * Updated Y-coordinate. Must be non-negative if provided.
     */
    @DecimalMin(value = "0.0", message = "Y coordinate must be non-negative")
    private Double y;

    /**
     * Updated element width. Must be non-negative if provided.
     */
    @DecimalMin(value = "0.0", message = "Element width must be non-negative")
    private Double width;

    /**
     * Updated element height. Must be non-negative if provided.
     */
    @DecimalMin(value = "0.0", message = "Element height must be non-negative")
    private Double height;

    /**
     * Updated text or path content.
     */
    private String content;

    /**
     * Updated JSON style string.
     */
    private String style;

    /**
     * Updated z-index for layer reordering.
     */
    @Min(value = 0, message = "Z-index must be non-negative")
    private Integer zIndex;
}
