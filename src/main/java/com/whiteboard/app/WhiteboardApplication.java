package com.whiteboard.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Real-Time Collaborative Whiteboard backend.
 *
 * This Spring Boot application provides:
 * - RESTful APIs for board, element, team, permission, and snapshot management
 * - JWT-based stateless authentication
 * - Real-time WebSocket collaboration via STOMP protocol
 * - Version history and board state replay via snapshots
 * - Analytics on collaboration patterns using Apache Commons Math regression
 *
 * Default port: 8080
 * API docs: http://localhost:8080/swagger-ui.html
 * WebSocket: ws://localhost:8080/ws
 */
@SpringBootApplication
public class WhiteboardApplication {

    /**
     * Application entry point.
     * Spring Boot auto-configures JPA, Security, WebSocket, and other components
     * based on the classpath and application.properties settings.
     *
     * @param args command-line arguments (passed to Spring context)
     */
    public static void main(String[] args) {
        SpringApplication.run(WhiteboardApplication.class, args);
    }
}
