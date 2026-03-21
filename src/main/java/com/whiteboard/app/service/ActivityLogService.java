package com.whiteboard.app.service;

import com.whiteboard.app.dto.ActivityLogResponse;
import com.whiteboard.app.exception.ResourceNotFoundException;
import com.whiteboard.app.model.ActivityLog;
import com.whiteboard.app.model.Board;
import com.whiteboard.app.model.User;
import com.whiteboard.app.repository.ActivityLogRepository;
import com.whiteboard.app.repository.BoardRepository;
import com.whiteboard.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for recording and retrieving activity logs.
 * Provides an audit trail / activity feed for the application.
 * Activity is filtered so users only see entries for boards they can access.
 */
@Service
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository,
                              BoardRepository boardRepository,
                              UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
    }

    /**
     * Record a new activity event.
     */
    @Transactional
    public void logActivity(String action, String description, User user, Board board) {
        ActivityLog entry = ActivityLog.builder()
                .action(action)
                .description(description)
                .user(user)
                .board(board)
                .build();
        activityLogRepository.save(entry);
        log.debug("Activity logged: {} by {} - {}", action, user.getUsername(), description);
    }

    /**
     * Get recent activity filtered to boards the user can access.
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getRecentActivity(int limit, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        Long userId = user.getId();

        // Collect all board IDs the user can access
        Set<Long> accessibleBoardIds = Stream.of(
                        boardRepository.findByOwnerOrderByCreatedAtDesc(user),
                        boardRepository.findBoardsAccessibleByUser(userId),
                        boardRepository.findBoardsAccessibleByTeamMember(userId))
                .flatMap(List::stream)
                .map(Board::getId)
                .collect(Collectors.toSet());

        // Fetch more entries than requested to account for filtering
        return activityLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit * 3))
                .stream()
                .filter(a -> a.getBoard() == null || accessibleBoardIds.contains(a.getBoard().getId()))
                .limit(limit)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get recent activity for a specific board.
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getBoardActivity(Long boardId, int limit) {
        return activityLogRepository.findByBoardIdOrderByCreatedAtDesc(boardId, PageRequest.of(0, limit))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ActivityLogResponse mapToResponse(ActivityLog entry) {
        return ActivityLogResponse.builder()
                .id(entry.getId())
                .action(entry.getAction())
                .description(entry.getDescription())
                .username(entry.getUser().getUsername())
                .boardId(entry.getBoard() != null ? entry.getBoard().getId() : null)
                .boardName(entry.getBoard() != null ? entry.getBoard().getName() : null)
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
