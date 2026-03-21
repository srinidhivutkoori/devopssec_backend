package com.whiteboard.app.repository;

import com.whiteboard.app.model.Board;
import com.whiteboard.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Board entity operations.
 * Provides board lookup by owner and boards accessible via permissions.
 */
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    /**
     * Find all boards owned by a specific user.
     * Returns boards sorted by creation date descending (most recent first).
     *
     * @param owner the user who owns the boards
     * @return list of boards owned by the user
     */
    List<Board> findByOwnerOrderByCreatedAtDesc(User owner);

    /**
     * Find all boards where the given user has explicit access permissions.
     * This query joins with access_permissions to find boards shared with the user
     * either directly or via team membership.
     *
     * @param userId the ID of the user to check permissions for
     * @return list of boards accessible to the user through permissions
     */
    @Query("SELECT DISTINCT b FROM Board b " +
           "JOIN AccessPermission ap ON ap.board = b " +
           "WHERE ap.user.id = :userId")
    List<Board> findBoardsAccessibleByUser(@Param("userId") Long userId);

    /**
     * Find all boards accessible to a user either as owner or via team membership.
     *
     * @param userId the user ID
     * @return list of boards accessible via team permissions
     */
    @Query("SELECT DISTINCT b FROM Board b " +
           "JOIN AccessPermission ap ON ap.board = b " +
           "JOIN ap.team t " +
           "JOIN t.members m " +
           "WHERE m.id = :userId")
    List<Board> findBoardsAccessibleByTeamMember(@Param("userId") Long userId);

    /**
     * Check if a user is the owner of a specific board.
     *
     * @param id      the board ID
     * @param ownerId the user ID to check
     * @return true if the user is the board owner
     */
    boolean existsByIdAndOwnerId(Long id, Long ownerId);
}
