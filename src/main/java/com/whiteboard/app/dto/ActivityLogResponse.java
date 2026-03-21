package com.whiteboard.app.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO returned for activity feed entries.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogResponse {
    private Long id;
    private String action;
    private String description;
    private String username;
    private Long boardId;
    private String boardName;
    private LocalDateTime createdAt;
}
