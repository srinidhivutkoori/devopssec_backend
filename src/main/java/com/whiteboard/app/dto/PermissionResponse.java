package com.whiteboard.app.dto;

import com.whiteboard.app.model.PermissionLevel;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO returned when an access permission is fetched from the API.
 * Shows who has what level of access to a board.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {

    /** Unique database identifier for the permission record. */
    private Long id;

    /** The ID of the board this permission applies to. */
    private Long boardId;

    /** The user granted this permission (null for team permissions). */
    private UserResponse user;

    /** The team granted this permission (null for user permissions). */
    private TeamResponse team;

    /** The level of access granted. */
    private PermissionLevel permissionLevel;

    /** When this permission was created. */
    private LocalDateTime createdAt;
}
