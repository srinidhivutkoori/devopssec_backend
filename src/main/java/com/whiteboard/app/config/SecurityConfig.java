package com.whiteboard.app.config;

import com.whiteboard.app.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the whiteboard backend.
 * Configures JWT-based stateless authentication:
 * - Authentication endpoints are public (register, login)
 * - All other endpoints require a valid JWT token
 * - CSRF is disabled as the API is stateless
 * - Sessions are not created or stored server-side (STATELESS policy)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserRepository userRepository;

    /**
     * Constructor injection of the JWT filter and user repository.
     *
     * @param jwtAuthFilter   the JWT authentication filter
     * @param userRepository  used to build the UserDetailsService
     */
    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserRepository userRepository) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userRepository = userRepository;
    }

    /**
     * Configure HTTP security rules, filter chain, and session management.
     * Defines which endpoints are public and which require authentication.
     *
     * @param http the HttpSecurity builder
     * @return the configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS using the CorsConfigurationSource bean
            .cors(cors -> {})

            // Disable CSRF protection - not needed for stateless JWT API
            .csrf(AbstractHttpConfigurer::disable)

            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Authentication endpoints are open to everyone
                .requestMatchers("/api/auth/**").permitAll()
                // Swagger/OpenAPI docs are publicly accessible
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                // H2 console access in development (should be disabled in production)
                .requestMatchers("/h2-console/**").permitAll()
                // WebSocket endpoint requires authentication but is handled by STOMP
                .requestMatchers("/ws/**").permitAll()
                // Actuator health endpoint for CI/CD smoke tests and load balancer checks
                .requestMatchers("/actuator/health").permitAll()
                // Application status endpoint is publicly accessible
                .requestMatchers("/api/status").permitAll()
                // All other API endpoints require a valid JWT token
                .anyRequest().authenticated()
            )

            // Use stateless session management - no server-side session state
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Set our custom DaoAuthenticationProvider
            .authenticationProvider(authenticationProvider())

            // Add JWT filter before Spring's default username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Allow H2 console frames in development
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /**
     * UserDetailsService bean that loads users from the database by username.
     * Used by the DaoAuthenticationProvider during authentication.
     *
     * @return a UserDetailsService backed by the UserRepository
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            // Load user from database and wrap in Spring Security's UserDetails
            com.whiteboard.app.model.User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "User not found with username: " + username));

            // Build a Spring Security UserDetails object with the stored BCrypt password
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .roles("USER") // Default role - can be extended with a roles table
                    .build();
        };
    }

    /**
     * Authentication provider that uses our UserDetailsService and BCrypt password encoder.
     *
     * @return configured DaoAuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCrypt password encoder bean for hashing and verifying user passwords.
     * Strength 10 is the default and provides good security vs. performance balance.
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager bean required for programmatic authentication in AuthService.
     *
     * @param config the authentication configuration
     * @return the AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
