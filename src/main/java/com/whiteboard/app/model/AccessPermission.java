package com.whiteboard.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an access permission grant for a whiteboard board.
 * A permission can be granted to an individual user OR to a team.
 * At least one of user or team must be non-null; the other should be null.
 * Permission levels: VIEW (read-only), EDIT (create/modify elements), ADMIN (full control).
 */
@Entity
@Table(name = "access_permissions", indexes = {
        @Index(name = "idx_perm_board", columnList = "board_id"),
        @Index(name = "idx_perm_user", columnList = "user_id"),
        @Index(name = "idx_perm_team", columnList = "team_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessPermission {

    /**
     * Primary key - auto-incremented by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The board this permission applies to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    /**
     * The user this permission is granted to.
     * Null when the permission is granted to a team instead.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * The team this permission is granted to.
     * Null when the permission is granted to an individual user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    /**
     * The level of access granted: VIEW, EDIT, or ADMIN.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 10)
    private PermissionLevel permissionLevel;

    /**
     * Timestamp when this permission was granted.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
