package com.whiteboard.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a registered user in the whiteboard application.
 * Users can own boards, belong to teams, and collaborate in real-time.
 * The password field is stored as a BCrypt hash - never in plaintext.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password") // Exclude password from toString for security
public class User {

    /**
     * Primary key - auto-incremented by the database sequence.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique username for login and display purposes.
     * Must be between 3 and 50 characters.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Unique email address for account identification and notifications.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * BCrypt-hashed password. Never expose this field in API responses.
     */
    @Column(nullable = false)
    private String password;

    /**
     * Display name shown in the UI during collaboration sessions.
     */
    @Column(name = "full_name", length = 100)
    private String fullName;

    /**
     * Timestamp of when this user account was created.
     * Automatically populated by Hibernate on insert.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
