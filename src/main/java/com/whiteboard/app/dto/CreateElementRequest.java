package com.whiteboard.app.dto;

import com.whiteboard.app.model.ElementType;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for creating a new element on a whiteboard canvas.
 * Coordinates and dimensions are validated to be positive values.
 * The x/y coordinates represent the element's top-left corner position.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateElementRequest {

    /**
     * The type of element to create (SHAPE, TEXT, STICKY_NOTE, IMAGE, FREEHAND).
     */
    @NotNull(message = "Element type is required")
    private ElementType type;

    /**
     * X-coordinate of the element's top-left corner on the canvas.
     * Must be a non-negative value (canvas starts at 0,0).
     */
    @NotNull(message = "X coordinate is required")
    @DecimalMin(value = "0.0", message = "X coordinate must be non-negative")
    private Double x;

    /**
     * Y-coordinate of the element's top-left corner on the canvas.
     */
    @NotNull(message = "Y coordinate is required")
    @DecimalMin(value = "0.0", message = "Y coordinate must be non-negative")
    private Double y;

    /**
     * Width of the element's bounding box.
     * FREEHAND and TEXT elements may have zero width (path data is in content).
     */
    @NotNull(message = "Width is required")
    @DecimalMin(value = "0.0", message = "Element width must be non-negative")
    private Double width;

    /**
     * Height of the element's bounding box.
     * FREEHAND and TEXT elements may have zero height (path data is in content).
     */
    @NotNull(message = "Height is required")
    @DecimalMin(value = "0.0", message = "Element height must be non-negative")
    private Double height;

    /**
     * Text content for TEXT and STICKY_NOTE types.
     * For FREEHAND, contains the serialized path data.
     */
    private String content;

    /**
     * JSON string containing visual style properties.
     * Expected keys: color, strokeWidth, fontSize, fontFamily, opacity, fillColor, etc.
     */
    private String style;

    /**
     * Z-index for layering. Higher values appear on top.
     * If not provided, the service will auto-assign the next available z-index.
     */
    @Min(value = 0, message = "Z-index must be non-negative")
    private Integer zIndex;
}
