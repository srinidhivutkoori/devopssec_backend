package com.whiteboard.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration allowing the React frontend (localhost:5173) to communicate
 * with this backend (localhost:8080).
 *
 * In production, replace the allowed origin with the actual deployed frontend URL.
 */
@Configuration
public class CorsConfig {

    /**
     * Configure CORS rules:
     * - Allow requests from the Vite dev server at localhost:5173
     * - Allow standard HTTP methods used by REST APIs
     * - Allow the Authorization header so JWT tokens can be sent
     * - Allow credentials (cookies, auth headers) to be included
     *
     * @return the CORS configuration source bean
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow the React frontend origins (local dev and S3 production)
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:10002",
                "http://*.s3-website-*.amazonaws.com",
                "http://*.s3.amazonaws.com"
        ));

        // Allow standard REST methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow Authorization header (for JWT) plus standard content headers
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With"
        ));

        // Expose the Authorization header so the frontend can read it
        config.setExposedHeaders(List.of("Authorization"));

        // Allow credentials so the Authorization header is sent with requests
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour to reduce OPTIONS request overhead
        config.setMaxAge(3600L);

        // Apply this configuration to all API paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
