package com.whiteboard.app.controller;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for authentication endpoints.
 * Provides user registration and login functionality.
 * These endpoints are publicly accessible (no JWT required).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    /**
     * Constructor injection of the user service.
     *
     * @param userService handles registration and authentication logic
     */
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Register a new user account.
     * Returns 201 Created with the new user's profile on success.
     *
     * POST /api/auth/register
     *
     * @param request registration details (username, email, password, fullName)
     * @return 201 with UserResponse, or 400 if validation fails
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate a user and return a JWT token.
     * Returns 200 OK with the JWT token and user info on success.
     *
     * POST /api/auth/login
     *
     * @param request login credentials (username + password)
     * @return 200 with LoginResponse containing the JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Search users by username (case-insensitive, contains match).
     * Used for autocomplete when adding team members or granting permissions.
     *
     * GET /api/auth/users/search?q=ali
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam("q") String query) {
        if (query == null || query.trim().length() < 1) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(userService.searchUsers(query.trim()));
    }
}
