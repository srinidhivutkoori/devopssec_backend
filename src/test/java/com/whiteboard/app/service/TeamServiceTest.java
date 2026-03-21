package com.whiteboard.app.service;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.exception.ResourceNotFoundException;
import com.whiteboard.app.model.Team;
import com.whiteboard.app.model.User;
import com.whiteboard.app.repository.TeamRepository;
import com.whiteboard.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TeamService covering team CRUD and membership management.
 * Verifies duplicate name prevention, creator-only access, and correct member add/remove behavior.
 */
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TeamService teamService;

    private User alice;
    private User bob;
    private Team testTeam;

    /**
     * Initialize shared test fixtures before each test.
     */
    @BeforeEach
    void setUp() {
        alice = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .createdAt(LocalDateTime.now())
                .build();

        bob = User.builder()
                .id(2L)
                .username("bob")
                .email("bob@example.com")
                .createdAt(LocalDateTime.now())
                .build();

        testTeam = Team.builder()
                .id(5L)
                .name("Engineering")
                .createdBy(alice)
                .createdAt(LocalDateTime.now())
                .members(new HashSet<>(Set.of(alice)))
                .build();
    }

    // ==================== Create Team Tests ====================

    /**
     * Happy path: creating a team with a unique name.
     */
    @Test
    @DisplayName("createTeam - should create team with unique name")
    void createTeam_shouldSucceedWithUniqueName() {
        CreateTeamRequest request = new CreateTeamRequest("Engineering");

        when(teamRepository.existsByName("Engineering")).thenReturn(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(teamRepository.save(any(Team.class))).thenReturn(testTeam);
        when(userService.mapToResponse(alice))
                .thenReturn(new UserResponse(1L, "alice", "alice@example.com", null, LocalDateTime.now()));

        TeamResponse response = teamService.createTeam(request, "alice");

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Engineering");
        verify(teamRepository).save(any(Team.class));
    }

    /**
     * Edge case: creating a team whose name is already taken throws an error.
     */
    @Test
    @DisplayName("createTeam - should throw IllegalArgumentException for duplicate name")
    void createTeam_shouldThrowForDuplicateName() {
        CreateTeamRequest request = new CreateTeamRequest("Engineering");

        when(teamRepository.existsByName("Engineering")).thenReturn(true);

        assertThatThrownBy(() -> teamService.createTeam(request, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Engineering");

        verify(teamRepository, never()).save(any());
    }

    // ==================== Get Team Tests ====================

    /**
     * Happy path: fetch a team by its ID.
     */
    @Test
    @DisplayName("getTeamById - should return TeamResponse for existing team")
    void getTeamById_shouldReturnTeam() {
        when(teamRepository.findById(5L)).thenReturn(Optional.of(testTeam));
        when(userService.mapToResponse(alice))
                .thenReturn(new UserResponse(1L, "alice", "alice@example.com", null, LocalDateTime.now()));

        TeamResponse response = teamService.getTeamById(5L);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Engineering");
        assertThat(response.getMembers()).hasSize(1);
    }

    /**
     * Edge case: fetching a non-existent team throws ResourceNotFoundException.
     */
    @Test
    @DisplayName("getTeamById - should throw ResourceNotFoundException for missing team")
    void getTeamById_shouldThrowWhenTeamNotFound() {
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ==================== Update Team Tests ====================

    /**
     * Happy path: updating a team's name to a new unique name (by creator).
     */
    @Test
    @DisplayName("updateTeam - should update team name successfully when called by creator")
    void updateTeam_shouldUpdateNameSuccessfully() {
        UpdateTeamRequest request = new UpdateTeamRequest("Product");

        Team renamedTeam = Team.builder()
                .id(5L)
                .name("Product")
                .createdBy(alice)
                .createdAt(testTeam.getCreatedAt())
                .members(new HashSet<>(Set.of(alice)))
                .build();

        when(teamRepository.findById(5L)).thenReturn(Optional.of(testTeam));
        when(teamRepository.existsByName("Product")).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenReturn(renamedTeam);
        when(userService.mapToResponse(alice))
                .thenReturn(new UserResponse(1L, "alice", "alice@example.com", null, LocalDateTime.now()));

        TeamResponse response = teamService.updateTeam(5L, request, "alice");

        assertThat(response.getName()).isEqualTo("Product");
    }

    /**
     * Edge case: updating to a name that is already taken by a different team.
     */
    @Test
    @DisplayName("updateTeam - should throw when new name is already taken by another team")
    void updateTeam_shouldThrowWhenNewNameAlreadyTaken() {
        UpdateTeamRequest request = new UpdateTeamRequest("Design");

        when(teamRepository.findById(5L)).thenReturn(Optional.of(testTeam));
        when(teamRepository.existsByName("Design")).thenReturn(true);

        assertThatThrownBy(() -> teamService.updateTeam(5L, request, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Design");

        verify(teamRepository, never()).save(any());
    }

    /**
     * Edge case: non-creator cannot update team.
     */
    @Test
    @DisplayName("updateTeam - should throw when called by non-creator")
    void updateTeam_shouldThrowWhenCalledByNonCreator() {
        UpdateTeamRequest request = new UpdateTeamRequest("Product");

        when(teamRepository.findById(5L)).thenReturn(Optional.of(testTeam));

        assertThatThrownBy(() -> teamService.updateTeam(5L, request, "bob"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("creator");

        verify(teamRepository, never()).save(any());
    }

    // ==================== Member Management Tests ====================

    /**
     * Happy path: adding a new user to a team's membership.
     */
    @Test
    @DisplayName("addMember - should add user to team membership")
    void addMember_shouldAddUserToTeam() {
        Team teamWithBob = Team.builder()
                .id(5L)
                .name("Engineering")
                .createdBy(alice)
                .createdAt(testTeam.getCreatedAt())
                .members(new HashSet<>(Set.of(alice, bob)))
                .build();

        when(teamRepository.findById(5L)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(teamRepository.save(any(Team.class))).thenReturn(teamWithBob);
        when(userService.mapToResponse(alice))
                .thenReturn(new UserResponse(1L, "alice", "alice@example.com", null, LocalDateTime.now()));
        when(userService.mapToResponse(bob))
                .thenReturn(new UserResponse(2L, "bob", "bob@example.com", null, LocalDateTime.now()));

        TeamResponse response = teamService.addMember(5L, 2L);

        assertThat(response.getMembers()).hasSize(2);
        verify(teamRepository).save(any(Team.class));
    }

    /**
     * Happy path: removing a user from a team's membership.
     */
    @Test
    @DisplayName("removeMember - should remove user from team membership")
    void removeMember_shouldRemoveUserFromTeam() {
        // Start with alice as a member, then remove her
        Team teamWithoutAlice = Team.builder()
                .id(5L)
                .name("Engineering")
                .createdBy(alice)
                .createdAt(testTeam.getCreatedAt())
                .members(new HashSet<>())
                .build();

        when(teamRepository.findById(5L)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(teamRepository.save(any(Team.class))).thenReturn(teamWithoutAlice);

        TeamResponse response = teamService.removeMember(5L, 1L);

        assertThat(response.getMembers()).isEmpty();
        verify(teamRepository).save(any(Team.class));
    }

    /**
     * Edge case: adding a member where the user ID doesn't exist.
     */
    @Test
    @DisplayName("addMember - should throw ResourceNotFoundException for non-existent user")
    void addMember_shouldThrowWhenUserNotFound() {
        when(teamRepository.findById(5L)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.addMember(5L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(teamRepository, never()).save(any());
    }

    // ==================== Delete Team Tests ====================

    /**
     * Happy path: creator deleting an existing team.
     */
    @Test
    @DisplayName("deleteTeam - should delete team when called by creator")
    void deleteTeam_shouldDeleteSuccessfully() {
        when(teamRepository.findById(5L)).thenReturn(Optional.of(testTeam));

        teamService.deleteTeam(5L, "alice");

        verify(teamRepository).delete(testTeam);
    }

    /**
     * Edge case: deleting a team that doesn't exist throws ResourceNotFoundException.
     */
    @Test
    @DisplayName("deleteTeam - should throw ResourceNotFoundException for missing team")
    void deleteTeam_shouldThrowWhenTeamNotFound() {
        when(teamRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.deleteTeam(404L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(teamRepository, never()).delete(any());
    }

    /**
     * Edge case: non-creator cannot delete team.
     */
    @Test
    @DisplayName("deleteTeam - should throw when called by non-creator")
    void deleteTeam_shouldThrowWhenCalledByNonCreator() {
        when(teamRepository.findById(5L)).thenReturn(Optional.of(testTeam));

        assertThatThrownBy(() -> teamService.deleteTeam(5L, "bob"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("creator");

        verify(teamRepository, never()).delete(any());
    }
}
