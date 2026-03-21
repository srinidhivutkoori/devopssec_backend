package com.whiteboard.app.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO returned when a board is fetched from the API.
 * Contains all board metadata including the owner's basic information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardResponse {

    /** Unique database identifier for the board. */
    private Long id;

    /** Human-readable board name. */
    private String name;

    /** Canvas width in pixels. */
    private Integer width;

    /** Canvas height in pixels. */
    private Integer height;

    /** Background color as hex string. */
    private String backgroundColor;

    /** Basic information about the board owner. */
    private UserResponse owner;

    /** When the board was first created. */
    private LocalDateTime createdAt;

    /** When the board metadata was last updated. */
    private LocalDateTime updatedAt;

    /** The requesting user's effective permission level on this board (OWNER, ADMIN, EDIT, VIEW). */
    private String permissionLevel;
}
