package com.whiteboard.app.repository;

import com.whiteboard.app.model.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ActivityLog entity operations.
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /** Get recent activity across all boards, newest first. */
    List<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Get recent activity for a specific board. */
    List<ActivityLog> findByBoardIdOrderByCreatedAtDesc(Long boardId, Pageable pageable);

    /** Get recent activity by a specific user. */
    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
