package com.whiteboard.app.repository;

import com.whiteboard.app.model.VersionSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for VersionSnapshot entity operations.
 * Supports snapshot listing and retrieval for board version history and replay.
 */
@Repository
public interface VersionSnapshotRepository extends JpaRepository<VersionSnapshot, Long> {

    /**
     * Find all snapshots for a board, sorted by creation time descending.
     * Most recent snapshots appear first in the version history list.
     *
     * @param boardId the ID of the board
     * @return list of snapshots ordered by creation time (newest first)
     */
    List<VersionSnapshot> findByBoardIdOrderByCreatedAtDesc(Long boardId);

    /**
     * Find the most recent snapshot for a board.
     * Used to get the latest saved state for board restore operations.
     *
     * @param boardId the board ID
     * @return Optional containing the latest snapshot if any exist
     */
    Optional<VersionSnapshot> findFirstByBoardIdOrderByCreatedAtDesc(Long boardId);

    /**
     * Count the number of snapshots for a given board.
     * Used to enforce snapshot limits or display history counts.
     *
     * @param boardId the board ID
     * @return total number of snapshots
     */
    long countByBoardId(Long boardId);

    /**
     * Find snapshots created within a specific time range.
     * Useful for filtering version history by date.
     *
     * @param boardId the board ID
     * @param from    start of the time range
     * @param to      end of the time range
     * @return list of snapshots within the range
     */
    @Query("SELECT vs FROM VersionSnapshot vs WHERE vs.board.id = :boardId " +
           "AND vs.createdAt BETWEEN :from AND :to " +
           "ORDER BY vs.createdAt DESC")
    List<VersionSnapshot> findByBoardIdAndCreatedAtBetween(
            @Param("boardId") Long boardId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Delete all snapshots for a board.
     * Called when the board itself is being deleted.
     *
     * @param boardId the board ID
     */
    void deleteAllByBoardId(Long boardId);
}
