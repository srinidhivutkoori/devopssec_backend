package com.whiteboard.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO for user login requests.
 * Accepts username or email plus password for authentication.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    /**
     * The username or email address of the user attempting to log in.
     */
    @NotBlank(message = "Username is required")
    private String username;

    /**
     * The user's password in plaintext (HTTPS transport required).
     * Will be compared against the stored BCrypt hash.
     */
    @NotBlank(message = "Password is required")
    private String password;
}
