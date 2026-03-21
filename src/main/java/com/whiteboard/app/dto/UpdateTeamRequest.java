package com.whiteboard.app.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for updating an existing team's name.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTeamRequest {

    /**
     * The new name for the team.
     */
    @NotBlank(message = "Team name is required")
    @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
    private String name;
}
