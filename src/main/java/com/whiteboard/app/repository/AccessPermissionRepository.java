package com.whiteboard.app.repository;

import com.whiteboard.app.model.AccessPermission;
import com.whiteboard.app.model.PermissionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AccessPermission entity operations.
 * Provides methods to check and retrieve board access for users and teams.
 */
@Repository
public interface AccessPermissionRepository extends JpaRepository<AccessPermission, Long> {

    /**
     * Find all permissions for a specific board.
     * Returns both user-level and team-level permissions.
     *
     * @param boardId the ID of the board
     * @return list of all permissions on the board
     */
    List<AccessPermission> findByBoardId(Long boardId);

    /**
     * Find the permission record for a specific user on a specific board.
     *
     * @param boardId the board ID
     * @param userId  the user ID
     * @return Optional containing the permission if found
     */
    Optional<AccessPermission> findByBoardIdAndUserId(Long boardId, Long userId);

    /**
     * Find the permission record for a specific team on a specific board.
     *
     * @param boardId the board ID
     * @param teamId  the team ID
     * @return Optional containing the permission if found
     */
    Optional<AccessPermission> findByBoardIdAndTeamId(Long boardId, Long teamId);

    /**
     * Find all permissions granted to a specific user across all boards.
     *
     * @param userId the user ID
     * @return list of permissions for the user
     */
    List<AccessPermission> findByUserId(Long userId);

    /**
     * Check whether a user has at least the given permission level on a board,
     * considering both direct user permissions and team memberships.
     *
     * @param boardId         the board ID
     * @param userId          the user ID
     * @param permissionLevel the minimum required permission
     * @return true if the user has the required access
     */
    @Query("SELECT COUNT(ap) > 0 FROM AccessPermission ap " +
           "WHERE ap.board.id = :boardId " +
           "AND ap.user.id = :userId " +
           "AND ap.permissionLevel IN :levels")
    boolean hasUserPermission(
            @Param("boardId") Long boardId,
            @Param("userId") Long userId,
            @Param("levels") List<PermissionLevel> levels
    );

    /**
     * Check whether a user has the required permission via team membership.
     *
     * @param boardId the board ID
     * @param userId  the user ID
     * @param levels  the list of acceptable permission levels
     * @return true if the user's team has the required access
     */
    @Query("SELECT COUNT(ap) > 0 FROM AccessPermission ap " +
           "JOIN ap.team t JOIN t.members m " +
           "WHERE ap.board.id = :boardId " +
           "AND m.id = :userId " +
           "AND ap.permissionLevel IN :levels")
    boolean hasTeamPermission(
            @Param("boardId") Long boardId,
            @Param("userId") Long userId,
            @Param("levels") List<PermissionLevel> levels
    );

    /**
     * Delete all permissions associated with a specific board.
     * Called before deleting the board itself.
     *
     * @param boardId the board ID
     */
    void deleteAllByBoardId(Long boardId);
}
