package com.whiteboard.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a point-in-time snapshot of an entire whiteboard's state.
 * Snapshots enable version history and board state replay functionality.
 * The snapshotData field stores the complete serialized JSON of all board elements
 * at the time the snapshot was created.
 */
@Entity
@Table(name = "version_snapshots", indexes = {
        @Index(name = "idx_snapshot_board", columnList = "board_id"),
        @Index(name = "idx_snapshot_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionSnapshot {

    /**
     * Primary key - auto-incremented by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The board this snapshot belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    /**
     * Full JSON serialization of the board state at snapshot time.
     * Includes all elements with their properties, styles, and positions.
     * Stored as TEXT to accommodate large boards with many elements.
     */
    @Column(name = "snapshot_data", columnDefinition = "TEXT", nullable = false)
    private String snapshotData;

    /**
     * The user who triggered the snapshot creation.
     * Useful for auditing who saved which version.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    /**
     * Timestamp when this snapshot was captured.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optional human-readable description of what changed in this snapshot.
     * Examples: "Initial layout", "Added user flow diagram", "Fixed color scheme".
     */
    @Column(length = 500)
    private String description;
}
