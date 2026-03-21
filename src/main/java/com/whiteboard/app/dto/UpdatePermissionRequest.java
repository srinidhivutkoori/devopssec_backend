package com.whiteboard.app.dto;

import com.whiteboard.app.model.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO for updating an existing permission's level.
 * Used to promote a user from VIEW to EDIT, or demote from ADMIN to EDIT, etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePermissionRequest {

    /**
     * The new permission level to assign.
     * Must be one of: VIEW, EDIT, ADMIN.
     */
    @NotNull(message = "Permission level is required")
    private PermissionLevel permissionLevel;
}
