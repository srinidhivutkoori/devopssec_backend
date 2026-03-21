package com.whiteboard.app.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for creating a new version snapshot of a board.
 * The snapshot captures the complete current state of all board elements.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSnapshotRequest {

    /**
     * Optional description of what this snapshot represents.
     * Examples: "Before major redesign", "Sprint 3 final layout".
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
