package com.whiteboard.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an activity event in the system.
 * Captures user actions like board creation, element edits,
 * snapshot saves, team changes, etc. for an audit trail / activity feed.
 */
@Entity
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_activity_created_at", columnList = "created_at"),
        @Index(name = "idx_activity_user", columnList = "user_id"),
        @Index(name = "idx_activity_board", columnList = "board_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The type of action performed (e.g. BOARD_CREATED, ELEMENT_ADDED, SNAPSHOT_SAVED) */
    @Column(nullable = false, length = 50)
    private String action;

    /** Human-readable description of the activity */
    @Column(nullable = false, length = 500)
    private String description;

    /** The user who performed the action */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The board related to this activity (nullable for non-board actions like team changes) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
