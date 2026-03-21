package com.whiteboard.app.repository;

import com.whiteboard.app.model.Element;
import com.whiteboard.app.model.ElementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Element entity operations.
 * Supports fetching elements by board with ordering for consistent rendering.
 */
@Repository
public interface ElementRepository extends JpaRepository<Element, Long> {

    /**
     * Find all elements belonging to a specific board, ordered by z-index for correct layering.
     * Elements with lower z-index values are rendered first (bottom layer).
     *
     * @param boardId the ID of the board
     * @return list of elements sorted by z-index ascending
     */
    @Query("SELECT e FROM Element e WHERE e.board.id = :boardId ORDER BY e.zIndex ASC")
    List<Element> findByBoardIdOrderByZIndexAsc(@Param("boardId") Long boardId);

    /**
     * Find all elements of a specific type on a board.
     * Useful for filtering the canvas to show only certain element categories.
     *
     * @param boardId the ID of the board
     * @param type    the element type to filter by
     * @return list of matching elements
     */
    List<Element> findByBoardIdAndType(Long boardId, ElementType type);

    /**
     * Find all elements currently locked by a specific user.
     * Used to release all locks when a user disconnects unexpectedly.
     *
     * @param userId the ID of the user holding locks
     * @return list of elements locked by the user
     */
    @Query("SELECT e FROM Element e WHERE e.lockedBy.id = :userId AND e.locked = true")
    List<Element> findLockedByUser(@Param("userId") Long userId);

    /**
     * Release all locks held by a specific user on a specific board.
     * Called when a collaborative session ends or a user disconnects.
     *
     * @param boardId the board to release locks on
     * @param userId  the user whose locks should be released
     */
    @Modifying
    @Query("UPDATE Element e SET e.locked = false, e.lockedBy = null " +
           "WHERE e.board.id = :boardId AND e.lockedBy.id = :userId")
    void releaseAllLocksForUserOnBoard(@Param("boardId") Long boardId, @Param("userId") Long userId);

    /**
     * Get the maximum z-index for elements on a board.
     * Used when adding a new element to place it on top of all existing elements.
     *
     * @param boardId the board ID
     * @return the maximum z-index or null if no elements exist
     */
    @Query("SELECT MAX(e.zIndex) FROM Element e WHERE e.board.id = :boardId")
    Integer findMaxZIndexByBoardId(@Param("boardId") Long boardId);

    /**
     * Delete all elements belonging to a specific board.
     * Used as a cleanup operation before deleting the board itself.
     *
     * @param boardId the ID of the board whose elements to delete
     */
    @Modifying
    @Query("DELETE FROM Element e WHERE e.board.id = :boardId")
    void deleteAllByBoardId(@Param("boardId") Long boardId);
}
