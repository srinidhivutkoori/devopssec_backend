package com.whiteboard.app.service;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.exception.ResourceNotFoundException;
import com.whiteboard.app.model.Team;
import com.whiteboard.app.model.User;
import com.whiteboard.app.repository.TeamRepository;
import com.whiteboard.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing teams and their memberships.
 * Teams group users together so board permissions can be granted to the entire group
 * rather than assigning them to each individual user separately.
 */
@Service
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Constructor injection of all required dependencies.
     *
     * @param teamRepository data access for teams
     * @param userRepository data access for users (member lookup)
     * @param userService    maps User entities to DTOs
     */
    public TeamService(TeamRepository teamRepository,
                       UserRepository userRepository,
                       UserService userService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * Create a new team with a unique name, owned by the authenticated user.
     *
     * @param request  the team creation parameters
     * @param username the authenticated user who will own the team
     * @return the created team as a response DTO
     * @throws IllegalArgumentException if a team with that name already exists
     */
    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request, String username) {
        if (teamRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "Team with name '" + request.getName() + "' already exists");
        }

        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Team team = Team.builder()
                .name(request.getName())
                .createdBy(creator)
                .build();

        Team saved = teamRepository.save(team);
        log.info("Team created: id={} name='{}' by user='{}'", saved.getId(), saved.getName(), username);
        return mapToResponse(saved);
    }

    /**
     * Retrieve a team by its ID.
     *
     * @param teamId the team's database ID
     * @return the team as a response DTO
     * @throws ResourceNotFoundException if no team with that ID exists
     */
    @Transactional(readOnly = true)
    public TeamResponse getTeamById(Long teamId) {
        Team team = findTeamOrThrow(teamId);
        return mapToResponse(team);
    }

    /**
     * Retrieve teams visible to the authenticated user
     * (teams they created or are a member of).
     *
     * @param username the authenticated user's username
     * @return list of teams the user can see
     */
    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<Team> createdTeams = teamRepository.findByCreatedBy(user);
        List<Team> memberTeams = teamRepository.findTeamsByMemberId(user.getId());

        return java.util.stream.Stream.of(createdTeams, memberTeams)
                .flatMap(List::stream)
                .collect(Collectors.toMap(Team::getId, t -> t, (a, b) -> a))
                .values()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update a team's name. Only the team creator can update it.
     *
     * @param teamId   the team to update
     * @param request  the new name
     * @param username the authenticated user requesting the update
     * @return the updated team as a response DTO
     * @throws IllegalArgumentException if the user is not the creator or name is taken
     */
    @Transactional
    public TeamResponse updateTeam(Long teamId, UpdateTeamRequest request, String username) {
        Team team = findTeamOrThrow(teamId);
        assertCreator(team, username);

        // Only validate uniqueness if the name is actually changing
        if (!team.getName().equals(request.getName()) &&
                teamRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "Team with name '" + request.getName() + "' already exists");
        }

        team.setName(request.getName());
        Team updated = teamRepository.save(team);
        log.info("Team updated: id={} new name='{}'", teamId, request.getName());
        return mapToResponse(updated);
    }

    /**
     * Delete a team by its ID. Only the team creator can delete it.
     *
     * @param teamId   the team to delete
     * @param username the authenticated user requesting deletion
     * @throws IllegalArgumentException if the user is not the creator
     */
    @Transactional
    public void deleteTeam(Long teamId, String username) {
        Team team = findTeamOrThrow(teamId);
        assertCreator(team, username);
        teamRepository.delete(team);
        log.info("Team deleted: id={} by user='{}'", teamId, username);
    }

    /**
     * Allow a member to leave a team voluntarily.
     *
     * @param teamId   the team to leave
     * @param username the authenticated user leaving the team
     * @return the updated team as a response DTO
     */
    @Transactional
    public TeamResponse leaveTeam(Long teamId, String username) {
        Team team = findTeamOrThrow(teamId);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (team.getCreatedBy() != null && team.getCreatedBy().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Team creator cannot leave the team. Delete the team instead.");
        }

        team.getMembers().remove(user);
        Team updated = teamRepository.save(team);
        log.info("User '{}' left team id={}", username, teamId);
        return mapToResponse(updated);
    }

    /**
     * Add a user as a member of a team.
     * Silently succeeds if the user is already a member.
     *
     * @param teamId the team to add the member to
     * @param userId the user to add
     * @return the updated team as a response DTO
     */
    @Transactional
    public TeamResponse addMember(Long teamId, Long userId) {
        Team team = findTeamOrThrow(teamId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        team.getMembers().add(user);
        Team updated = teamRepository.save(team);
        log.info("User id={} added to team id={}", userId, teamId);
        return mapToResponse(updated);
    }

    /**
     * Add a user as a member of a team by username.
     *
     * @param teamId   the team to add the member to
     * @param username the username to look up and add
     * @return the updated team as a response DTO
     */
    @Transactional
    public TeamResponse addMemberByUsername(Long teamId, String username) {
        Team team = findTeamOrThrow(teamId);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        team.getMembers().add(user);
        Team updated = teamRepository.save(team);
        log.info("User '{}' added to team id={}", username, teamId);
        return mapToResponse(updated);
    }

    /**
     * Remove a user from a team's membership.
     * Silently succeeds if the user was not a member.
     *
     * @param teamId the team to remove the member from
     * @param userId the user to remove
     * @return the updated team as a response DTO
     */
    @Transactional
    public TeamResponse removeMember(Long teamId, Long userId) {
        Team team = findTeamOrThrow(teamId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        team.getMembers().remove(user);
        Team updated = teamRepository.save(team);
        log.info("User id={} removed from team id={}", userId, teamId);
        return mapToResponse(updated);
    }

    /**
     * Fetch a team entity or throw ResourceNotFoundException.
     *
     * @param teamId the team ID to look up
     * @return the team entity
     */
    private Team findTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
    }

    /**
     * Assert that the current user is the creator of the team.
     *
     * @param team            the team entity
     * @param currentUsername the authenticated user's username
     * @throws IllegalArgumentException if the user is not the creator
     */
    private void assertCreator(Team team, String currentUsername) {
        if (team.getCreatedBy() == null ||
                !team.getCreatedBy().getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("Only the team creator can perform this action");
        }
    }

    /**
     * Map a Team entity to its response DTO including all member details.
     *
     * @param team the entity to map
     * @return the response DTO
     */
    public TeamResponse mapToResponse(Team team) {
        Set<UserResponse> memberResponses = team.getMembers()
                .stream()
                .map(userService::mapToResponse)
                .collect(Collectors.toSet());

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .createdBy(team.getCreatedBy() != null ? userService.mapToResponse(team.getCreatedBy()) : null)
                .createdAt(team.getCreatedAt())
                .members(memberResponses)
                .build();
    }
}
