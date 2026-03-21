package com.whiteboard.app.dto;

import lombok.*;

/**
 * DTO returned upon successful authentication.
 * Contains the JWT token and basic user information needed by the frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    /**
     * The JWT bearer token to include in subsequent API requests.
     * Format: Authorization: Bearer {token}
     */
    private String token;

    /**
     * Token type - always "Bearer" for JWT authentication.
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Token expiration time in milliseconds from now.
     */
    private Long expiresIn;

    /**
     * The authenticated user's information.
     */
    private UserResponse user;
}
