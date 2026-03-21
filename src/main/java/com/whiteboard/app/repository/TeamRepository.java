package com.whiteboard.app.repository;

import com.whiteboard.app.model.Team;
import com.whiteboard.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Team entity operations.
 * Includes queries for finding teams by member, creator, and name.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * Find all teams created by a specific user.
     *
     * @param createdBy the user who created the teams
     * @return list of teams created by the user
     */
    List<Team> findByCreatedBy(User createdBy);

    /**
     * Find all teams that a specific user is a member of.
     * Used to determine a user's team-based board access.
     *
     * @param userId the ID of the user
     * @return list of teams containing the user as a member
     */
    @Query("SELECT t FROM Team t JOIN t.members m WHERE m.id = :userId")
    List<Team> findTeamsByMemberId(@Param("userId") Long userId);

    /**
     * Find a team by its exact name.
     *
     * @param name the team name
     * @return Optional containing the team if found
     */
    Optional<Team> findByName(String name);

    /**
     * Check if a team with the given name already exists.
     *
     * @param name the name to check
     * @return true if a team with this name exists
     */
    boolean existsByName(String name);
}
