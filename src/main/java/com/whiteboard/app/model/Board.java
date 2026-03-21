package com.whiteboard.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a collaborative whiteboard.
 * A board is the canvas on which users place and edit elements.
 * Each board has an owner and configurable dimensions and background color.
 */
@Entity
@Table(name = "boards", indexes = {
        @Index(name = "idx_board_owner", columnList = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {

    /**
     * Primary key - auto-incremented by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable name for the board, shown in listings and browser tabs.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Canvas width in pixels. Must be between 100 and 10000.
     */
    @Column(nullable = false)
    private Integer width;

    /**
     * Canvas height in pixels. Must be between 100 and 10000.
     */
    @Column(nullable = false)
    private Integer height;

    /**
     * Background color of the canvas as a hex color string (e.g., "#FFFFFF").
     */
    @Column(name = "background_color", length = 20)
    @Builder.Default
    private String backgroundColor = "#FFFFFF";

    /**
     * The user who created and owns this board.
     * The owner always has implicit ADMIN permission.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Timestamp when the board was first created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the most recent modification to the board metadata.
     * Element changes do not update this field directly.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
