package com.whiteboard.app.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO returned when a version snapshot is fetched from the API.
 * The snapshotData field contains the full JSON representation of the board state
 * at the time the snapshot was taken, enabling board replay and restore operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotResponse {

    /** Unique database identifier for the snapshot. */
    private Long id;

    /** The ID of the board this snapshot belongs to. */
    private Long boardId;

    /**
     * Full JSON serialization of all board elements at snapshot time.
     * This field can be large for boards with many elements.
     */
    private String snapshotData;

    /** The user who created this snapshot. */
    private UserResponse createdBy;

    /** When this snapshot was captured. */
    private LocalDateTime createdAt;

    /** Human-readable description of the snapshot. */
    private String description;
}
