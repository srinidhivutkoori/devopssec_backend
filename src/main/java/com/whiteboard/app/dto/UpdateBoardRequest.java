package com.whiteboard.app.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for updating an existing board's metadata.
 * All fields are optional - only provided fields will be updated.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBoardRequest {

    /**
     * New display name for the board. Optional.
     */
    @Size(min = 1, max = 200, message = "Board name must be between 1 and 200 characters")
    private String name;

    /**
     * Updated canvas width. If provided, must be within valid range.
     */
    @Min(value = 100, message = "Board width must be at least 100 pixels")
    @Max(value = 10000, message = "Board width must not exceed 10000 pixels")
    private Integer width;

    /**
     * Updated canvas height. If provided, must be within valid range.
     */
    @Min(value = 100, message = "Board height must be at least 100 pixels")
    @Max(value = 10000, message = "Board height must not exceed 10000 pixels")
    private Integer height;

    /**
     * Updated background color as a hex color string.
     */
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
             message = "Background color must be a valid hex color code (e.g., #FFFFFF)")
    private String backgroundColor;
}
