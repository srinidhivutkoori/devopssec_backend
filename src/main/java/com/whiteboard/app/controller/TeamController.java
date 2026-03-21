package com.whiteboard.app.controller;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for team management operations.
 * Teams group users so board permissions can be granted at the team level.
 * All endpoints require JWT authentication.
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    /**
     * Constructor injection of the team service.
     *
     * @param teamService handles team lifecycle and membership logic
     */
    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    /**
     * Create a new team owned by the authenticated user.
     *
     * POST /api/teams
     *
     * @param request     team creation parameters (name)
     * @param userDetails the authenticated user
     * @return 201 Created with the new team's details
     */
    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TeamResponse response = teamService.createTeam(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve teams visible to the authenticated user
     * (created by them or where they are a member).
     *
     * GET /api/teams
     *
     * @param userDetails the authenticated user
     * @return 200 OK with list of visible teams
     */
    @GetMapping
    public ResponseEntity<List<TeamResponse>> getMyTeams(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<TeamResponse> teams = teamService.getTeamsForUser(userDetails.getUsername());
        return ResponseEntity.ok(teams);
    }

    /**
     * Retrieve a specific team by its ID.
     *
     * GET /api/teams/{teamId}
     *
     * @param teamId the team's database ID
     * @return 200 OK with team details, or 404 if not found
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long teamId) {
        TeamResponse response = teamService.getTeamById(teamId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a team's name. Only the team creator can update it.
     *
     * PUT /api/teams/{teamId}
     *
     * @param teamId      the team to update
     * @param request     the new name
     * @param userDetails the authenticated user
     * @return 200 OK with updated team details
     */
    @PutMapping("/{teamId}")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TeamResponse response = teamService.updateTeam(teamId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a team by its ID. Only the team creator can delete it.
     *
     * DELETE /api/teams/{teamId}
     *
     * @param teamId      the team to delete
     * @param userDetails the authenticated user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        teamService.deleteTeam(teamId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Leave a team. The authenticated user removes themselves from the team.
     * The team creator cannot leave (they must delete the team instead).
     *
     * POST /api/teams/{teamId}/leave
     *
     * @param teamId      the team to leave
     * @param userDetails the authenticated user
     * @return 200 OK with updated team details
     */
    @PostMapping("/{teamId}/leave")
    public ResponseEntity<TeamResponse> leaveTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        TeamResponse response = teamService.leaveTeam(teamId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Add a user to a team's membership list.
     *
     * POST /api/teams/{teamId}/members/{userId}
     *
     * @param teamId the team to add the member to
     * @param userId the user to add
     * @return 200 OK with updated team details
     */
    @PostMapping("/{teamId}/members/{userId}")
    public ResponseEntity<TeamResponse> addMember(
            @PathVariable Long teamId,
            @PathVariable Long userId) {
        TeamResponse response = teamService.addMember(teamId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Add a user to a team by username (from request body).
     *
     * POST /api/teams/{teamId}/members
     *
     * @param teamId  the team to add the member to
     * @param request body containing "username" field
     * @return 200 OK with updated team details
     */
    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamResponse> addMemberByUsername(
            @PathVariable Long teamId,
            @RequestBody Map<String, String> request) {
        String username = request.get("username");
        TeamResponse response = teamService.addMemberByUsername(teamId, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a user from a team's membership list.
     *
     * DELETE /api/teams/{teamId}/members/{userId}
     *
     * @param teamId the team to remove the member from
     * @param userId the user to remove
     * @return 200 OK with updated team details
     */
    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<TeamResponse> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long userId) {
        TeamResponse response = teamService.removeMember(teamId, userId);
        return ResponseEntity.ok(response);
    }
}
