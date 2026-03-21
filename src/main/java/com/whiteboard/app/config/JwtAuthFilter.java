package com.whiteboard.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that intercepts every HTTP request exactly once.
 * Extracts the JWT token from the Authorization header, validates it,
 * and populates the Spring Security context with the authenticated user's details.
 *
 * Token format expected in the Authorization header:
 *   Authorization: Bearer {jwt-token}
 *
 * If the token is missing, invalid, or expired, the filter chain continues
 * without setting authentication - subsequent security rules will deny access.
 */
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    /**
     * Constructor injection to avoid field-level @Autowired.
     *
     * @param jwtTokenProvider  handles JWT parsing and validation
     * @param userDetailsService loads user details from the database
     */
    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider, @Lazy UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Core filter logic executed for every request.
     * Validates the JWT and sets authentication in the SecurityContext if valid.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain to execute
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Extract the Bearer token from the Authorization header
        String token = extractJwtFromRequest(request);

        // Only proceed if a token was found and is structurally valid
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            // Extract the username from the verified token claims
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // Load the full user details (roles, enabled status, etc.) from the database
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Build the authentication token with user details and authorities
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Credentials are null after successful authentication
                            userDetails.getAuthorities()
                    );

            // Attach request-specific details (remote IP, session ID) for auditing
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Store the authentication in the thread-local security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Set authentication for user: {}", username);
        }

        // Continue the filter chain regardless of authentication result
        filterChain.doFilter(request, response);
    }

    /**
     * Extract the JWT token string from the Authorization header.
     * Expects the format: "Bearer {token}".
     *
     * @param request the HTTP request
     * @return the raw JWT token string, or null if not present or malformed
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // Check for the "Bearer " prefix (7 characters)
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
