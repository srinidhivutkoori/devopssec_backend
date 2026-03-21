package com.whiteboard.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a team of users who collaborate together.
 * Teams can be granted board permissions as a group, simplifying access management
 * for large collaborative organizations.
 */
@Entity
@Table(name = "teams", indexes = {
        @Index(name = "idx_team_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    /**
     * Primary key - auto-incremented by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique display name for the team (e.g., "Engineering", "Design").
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * The user who created this team. Only this user can delete or edit the team.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /**
     * Timestamp when this team was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * The set of users who are members of this team.
     * Uses a join table to manage the many-to-many relationship.
     * Initialized as empty HashSet to avoid NullPointerExceptions.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "team_members",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> members = new HashSet<>();
}
