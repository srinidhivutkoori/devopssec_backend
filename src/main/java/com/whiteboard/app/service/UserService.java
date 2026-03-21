package com.whiteboard.app.service;

import com.whiteboard.app.config.JwtTokenProvider;
import com.whiteboard.app.dto.*;
import com.whiteboard.app.exception.ResourceNotFoundException;
import com.whiteboard.app.model.User;
import com.whiteboard.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user registration, authentication, and profile retrieval.
 * Passwords are hashed with BCrypt before storage - never stored in plaintext.
 * JWT tokens are issued upon successful login for subsequent API access.
 */
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructor injection of all dependencies.
     *
     * @param userRepository        data access for users
     * @param passwordEncoder       BCrypt encoder for password hashing
     * @param authenticationManager Spring Security authentication entry point
     * @param jwtTokenProvider      generates and validates JWT tokens
     */
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Register a new user account.
     * Validates uniqueness of username and email before persisting.
     * The password is BCrypt-hashed before storage.
     *
     * @param request the registration details
     * @return the created user's public profile
     * @throws IllegalArgumentException if username or email is already taken
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Validate username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already taken");
        }

        // Validate email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' is already registered");
        }

        // Build and persist the new user with a hashed password
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getUsername());
        return mapToResponse(savedUser);
    }

    /**
     * Authenticate a user and return a JWT token.
     * Delegates credential verification to Spring Security's AuthenticationManager.
     *
     * @param request login credentials (username + password)
     * @return login response containing JWT token and user profile
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Authenticate via Spring Security - throws BadCredentialsException on failure
        // The returned Authentication object is intentionally discarded; we only need
        // the side-effect of credential verification before issuing the JWT token.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // Authentication succeeded - generate a JWT token for the user
        String token = jwtTokenProvider.generateToken(request.getUsername());

        // Load the full user entity for the response body
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

        log.info("User logged in: {}", request.getUsername());
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .user(mapToResponse(user))
                .build();
    }

    /**
     * Get a user's public profile by their ID.
     *
     * @param userId the user's database ID
     * @return the user's public profile DTO
     * @throws ResourceNotFoundException if no user with that ID exists
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToResponse(user);
    }

    /**
     * Get a user's public profile by their username.
     *
     * @param username the user's login username
     * @return the user's public profile DTO
     * @throws ResourceNotFoundException if no user with that username exists
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return mapToResponse(user);
    }

    /**
     * Search users by username prefix (case-insensitive).
     * Used for autocomplete when adding team members or granting permissions.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String query) {
        return userRepository.findByUsernameContainingIgnoreCase(query)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map a User entity to its public-facing DTO.
     * Excludes sensitive fields like the password hash.
     *
     * @param user the entity to map
     * @return the response DTO
     */
    public UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
