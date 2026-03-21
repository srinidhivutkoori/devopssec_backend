package com.whiteboard.app.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO returned when a team is fetched from the API.
 * Includes the team's membership list for display purposes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamResponse {

    /** Unique database identifier for the team. */
    private Long id;

    /** The team's display name. */
    private String name;

    /** The user who created this team. */
    private UserResponse createdBy;

    /** When the team was created. */
    private LocalDateTime createdAt;

    /** The set of users who are members of this team. */
    private Set<UserResponse> members;
}
