package com.whiteboard.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a single visual element on a whiteboard canvas.
 * Elements can be shapes, text, sticky notes, images, or freehand drawings.
 * Locking prevents concurrent edit conflicts in collaborative sessions.
 */
@Entity
@Table(name = "elements", indexes = {
        @Index(name = "idx_element_board", columnList = "board_id"),
        @Index(name = "idx_element_locked_by", columnList = "locked_by_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Element {

    /**
     * Primary key - auto-incremented by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The board this element belongs to.
     * Cascade delete is handled at the board level.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    /**
     * The type of this element, determines how it is rendered on the canvas.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ElementType type;

    /**
     * X-coordinate position of the element's top-left corner on the canvas.
     */
    @Column(nullable = false)
    private Double x;

    /**
     * Y-coordinate position of the element's top-left corner on the canvas.
     */
    @Column(nullable = false)
    private Double y;

    /**
     * Width of the element's bounding box in pixels. Must be positive.
     */
    @Column(nullable = false)
    private Double width;

    /**
     * Height of the element's bounding box in pixels. Must be positive.
     */
    @Column(nullable = false)
    private Double height;

    /**
     * Text content for TEXT and STICKY_NOTE types.
     * For FREEHAND, this stores the serialized path data (SVG path or point array).
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * JSON string containing visual styling properties such as:
     * color, strokeWidth, fontSize, fontFamily, opacity, etc.
     * Stored as a raw JSON string for flexibility.
     */
    @Column(columnDefinition = "TEXT")
    private String style;

    /**
     * Z-index controls the stacking order of elements.
     * Higher values render on top of lower values.
     */
    @Column(name = "z_index", nullable = false)
    @Builder.Default
    private Integer zIndex = 0;

    /**
     * Whether this element is currently locked by another user for editing.
     * Prevents concurrent modifications that would cause conflicts.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean locked = false;

    /**
     * The user who currently holds the lock on this element.
     * Null when the element is not locked.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by_id")
    private User lockedBy;

    /**
     * Timestamp when this element was first created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the most recent update to this element's properties.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
