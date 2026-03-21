package com.whiteboard.app.dto;

import com.whiteboard.app.model.PermissionLevel;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for granting a new access permission on a board.
 * Either userId or teamId must be provided, but not both.
 * The permission level determines what operations the grantee can perform.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePermissionRequest {

    /**
     * The ID of the user to grant permission to.
     * Mutually exclusive with teamId - provide one or the other.
     */
    private Long userId;

    /**
     * Username of the user to grant permission to.
     * Alternative to userId - looked up by the service.
     */
    private String username;

    /**
     * The ID of the team to grant permission to.
     * Mutually exclusive with userId - provide one or the other.
     */
    private Long teamId;

    /**
     * The level of access to grant: VIEW, EDIT, or ADMIN.
     */
    @NotNull(message = "Permission level is required")
    private PermissionLevel permissionLevel;
}
