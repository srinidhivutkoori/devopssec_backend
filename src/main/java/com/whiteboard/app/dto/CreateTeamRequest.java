package com.whiteboard.app.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for creating a new team.
 * Teams group users together for simplified board permission management.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTeamRequest {

    /**
     * Unique name for the team (e.g., "Engineering", "Design Team").
     */
    @NotBlank(message = "Team name is required")
    @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
    private String name;
}
