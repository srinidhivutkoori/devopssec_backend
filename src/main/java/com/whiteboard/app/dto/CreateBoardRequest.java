package com.whiteboard.app.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for creating a new whiteboard.
 * Canvas dimensions are validated to be between 100x100 and 10000x10000 pixels.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBoardRequest {

    /**
     * Display name for the board.
     */
    @NotBlank(message = "Board name is required")
    @Size(min = 1, max = 200, message = "Board name must be between 1 and 200 characters")
    private String name;

    /**
     * Canvas width in pixels. Minimum 100, maximum 10000.
     */
    @NotNull(message = "Width is required")
    @Min(value = 100, message = "Board width must be at least 100 pixels")
    @Max(value = 10000, message = "Board width must not exceed 10000 pixels")
    private Integer width;

    /**
     * Canvas height in pixels. Minimum 100, maximum 10000.
     */
    @NotNull(message = "Height is required")
    @Min(value = 100, message = "Board height must be at least 100 pixels")
    @Max(value = 10000, message = "Board height must not exceed 10000 pixels")
    private Integer height;

    /**
     * Background color as a hex color string (e.g., "#FFFFFF").
     * Defaults to white if not provided.
     */
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
             message = "Background color must be a valid hex color code (e.g., #FFFFFF)")
    private String backgroundColor;
}
