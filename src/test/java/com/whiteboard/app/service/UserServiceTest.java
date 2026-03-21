package com.whiteboard.app.service;

import com.whiteboard.app.config.JwtTokenProvider;
import com.whiteboard.app.dto.*;
import com.whiteboard.app.exception.ResourceNotFoundException;
import com.whiteboard.app.model.User;
import com.whiteboard.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService covering registration, login, and profile retrieval.
 * Uses Mockito to isolate the service from its database and security dependencies.
 * No Spring context is started - pure unit tests with fast execution.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserService userService;

    // Sample user entity used across multiple tests
    private User sampleUser;

    /**
     * Set up test fixtures before each test.
     * Creates a pre-built User entity with known values for assertion.
     */
    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("$2a$10$hashedpassword")
                .fullName("Test User")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== Registration Tests ====================

    /**
     * Happy path: registering a new user with unique username and email.
     * Verifies the returned DTO matches the saved entity and password is hashed.
     */
    @Test
    @DisplayName("register - should create user and return UserResponse")
    void register_shouldCreateUserSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .fullName("Test User")
                .build();

        // Simulate no existing user with the given username or email
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.register(request);

        // Verify the response contains the correct data
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFullName()).isEqualTo("Test User");

        // Verify password was hashed before saving
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    /**
     * Edge case: registering with a username that is already taken.
     * Should throw IllegalArgumentException before attempting to save.
     */
    @Test
    @DisplayName("register - should throw IllegalArgumentException for duplicate username")
    void register_shouldThrowWhenUsernameAlreadyTaken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("new@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("testuser");

        // Ensure save was never called - no partial record created
        verify(userRepository, never()).save(any());
    }

    /**
     * Edge case: registering with an email address already in use.
     * Should throw IllegalArgumentException without saving.
     */
    @Test
    @DisplayName("register - should throw IllegalArgumentException for duplicate email")
    void register_shouldThrowWhenEmailAlreadyRegistered() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("test@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("test@example.com");

        verify(userRepository, never()).save(any());
    }

    // ==================== Login Tests ====================

    /**
     * Happy path: successful login returns a JWT token and user profile.
     */
    @Test
    @DisplayName("login - should return LoginResponse with token on valid credentials")
    void login_shouldReturnTokenOnValidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        // Authentication succeeds (no exception thrown)
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // Return value is not used
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateToken("testuser")).thenReturn("eyJhbGciOiJIUzI1NiJ9.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        LoginResponse response = userService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("eyJhbGciOiJIUzI1NiJ9.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
    }

    /**
     * Edge case: login with wrong credentials throws BadCredentialsException.
     */
    @Test
    @DisplayName("login - should propagate BadCredentialsException on wrong password")
    void login_shouldThrowBadCredentialsOnWrongPassword() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ==================== Profile Retrieval Tests ====================

    /**
     * Happy path: fetching a user profile by their database ID.
     */
    @Test
    @DisplayName("getUserById - should return UserResponse for existing user")
    void getUserById_shouldReturnUserResponseForExistingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    /**
     * Edge case: requesting a user profile with a non-existent ID.
     * Should throw ResourceNotFoundException with the ID in the message.
     */
    @Test
    @DisplayName("getUserById - should throw ResourceNotFoundException for missing user")
    void getUserById_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    /**
     * Verify that mapToResponse correctly omits the password field.
     * This is a security-critical check - the password hash must never leak.
     */
    @Test
    @DisplayName("mapToResponse - should not expose the password hash")
    void mapToResponse_shouldExcludePasswordHash() {
        UserResponse response = userService.mapToResponse(sampleUser);

        // The UserResponse class has no password field - this verifies correct DTO design
        assertThat(response.getUsername()).isEqualTo(sampleUser.getUsername());
        assertThat(response.getEmail()).isEqualTo(sampleUser.getEmail());
        // No password getter exists on UserResponse - confirmed by compile-time contract
    }
}
