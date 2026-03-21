package com.whiteboard.app.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for user registration requests.
 * Contains all required fields for creating a new user account.
 * All fields are validated to ensure data integrity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    /**
     * Desired username - must be unique and between 3 and 50 characters.
     * Alphanumeric characters and underscores only.
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    /**
     * Valid email address for the account.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    /**
     * Password - must be at least 8 characters with complexity requirements.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    /**
     * Optional display name shown during collaboration.
     */
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;
}
