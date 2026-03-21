package com.whiteboard.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration enabling STOMP messaging over WebSocket.
 *
 * Architecture overview:
 * - Clients connect to /ws endpoint via SockJS (fallback for non-WS browsers)
 * - Messages are sent by clients to /app/... destinations
 * - The in-memory broker routes messages to /topic/... subscribers
 *
 * Usage example for collaborative drawing:
 *   Client sends:    /app/board/{boardId}/element-update
 *   Server sends to: /topic/board/{boardId}
 *   All subscribers on /topic/board/{boardId} receive the update in real time.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure the in-memory message broker.
     * /topic/ prefix: broadcast destinations (one-to-many, e.g., board updates)
     * /app/ prefix: routes messages to @MessageMapping handler methods in controllers
     *
     * @param registry the message broker configuration registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable an in-memory broker for /topic destinations
        registry.enableSimpleBroker("/topic");

        // Set the application destination prefix for @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Register the STOMP WebSocket endpoint that clients connect to.
     * SockJS fallback is enabled for environments where WebSocket is unavailable.
     *
     * @param registry the STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Allow the React frontend origins (local dev + S3 production)
                .setAllowedOriginPatterns(
                        "http://localhost:10002",
                        "http://*.s3-website-*.amazonaws.com",
                        "http://*.s3.amazonaws.com"
                )
                // Enable SockJS fallback for browsers that do not support WebSocket
                .withSockJS();
    }
}
