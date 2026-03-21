package com.whiteboard.app.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for returning user information in API responses.
 * Excludes sensitive fields like the password hash.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    /** Unique database identifier for the user. */
    private Long id;

    /** The user's unique login username. */
    private String username;

    /** The user's email address. */
    private String email;

    /** The user's display name for collaboration sessions. */
    private String fullName;

    /** When the user account was created. */
    private LocalDateTime createdAt;
}
