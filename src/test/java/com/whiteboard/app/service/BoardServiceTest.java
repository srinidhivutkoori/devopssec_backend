package com.whiteboard.app.service;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.exception.ResourceNotFoundException;
import com.whiteboard.app.model.Board;
import com.whiteboard.app.model.User;
import com.whiteboard.app.repository.BoardRepository;
import com.whiteboard.app.repository.UserRepository;
import com.whiteboard.app.repository.ElementRepository;
import com.whiteboard.app.repository.AccessPermissionRepository;
import com.whiteboard.app.repository.VersionSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BoardService covering CRUD operations and ownership validation.
 * Uses Mockito to mock the repository and UserService dependencies.
 */
@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private ElementRepository elementRepository;

    @Mock
    private AccessPermissionRepository permissionRepository;

    @Mock
    private VersionSnapshotRepository snapshotRepository;

    @Mock
    private ActivityLogService activityLogService;

    private BoardService boardService;

    // Shared test fixtures
    private User testOwner;
    private Board testBoard;

    /**
     * Initialize test fixtures with known values for assertions.
     */
    @BeforeEach
    void setUp() {
        boardService = new BoardService(boardRepository, userRepository, userService,
                elementRepository, permissionRepository, snapshotRepository, activityLogService);
        testOwner = User.builder()
                .id(1L)
                .username("owneruser")
                .email("owner@example.com")
                .fullName("Board Owner")
                .createdAt(LocalDateTime.now())
                .build();

        testBoard = Board.builder()
                .id(10L)
                .name("Test Board")
                .width(1920)
                .height(1080)
                .backgroundColor("#FFFFFF")
                .owner(testOwner)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== Create Board Tests ====================

    /**
     * Happy path: creating a board with all valid inputs.
     */
    @Test
    @DisplayName("createBoard - should create and return BoardResponse")
    void createBoard_shouldSucceedWithValidInputs() {
        CreateBoardRequest request = CreateBoardRequest.builder()
                .name("Test Board")
                .width(1920)
                .height(1080)
                .backgroundColor("#FFFFFF")
                .build();

        when(userRepository.findByUsername("owneruser")).thenReturn(Optional.of(testOwner));
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        when(userService.mapToResponse(testOwner))
                .thenReturn(new UserResponse(1L, "owneruser", "owner@example.com", "Board Owner", LocalDateTime.now()));

        BoardResponse response = boardService.createBoard(request, "owneruser");

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Board");
        assertThat(response.getWidth()).isEqualTo(1920);
        assertThat(response.getHeight()).isEqualTo(1080);
        assertThat(response.getOwner().getUsername()).isEqualTo("owneruser");
        verify(boardRepository).save(any(Board.class));
    }

    /**
     * Edge case: creating a board when the specified owner username doesn't exist.
     */
    @Test
    @DisplayName("createBoard - should throw ResourceNotFoundException for unknown owner")
    void createBoard_shouldThrowWhenOwnerNotFound() {
        CreateBoardRequest request = CreateBoardRequest.builder()
                .name("Board")
                .width(800)
                .height(600)
                .build();

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.createBoard(request, "ghost"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(boardRepository, never()).save(any());
    }

    /**
     * Verify that a null backgroundColor defaults to white "#FFFFFF".
     */
    @Test
    @DisplayName("createBoard - should default backgroundColor to #FFFFFF when not provided")
    void createBoard_shouldDefaultBackgroundColorToWhite() {
        CreateBoardRequest request = CreateBoardRequest.builder()
                .name("Board")
                .width(800)
                .height(600)
                .backgroundColor(null) // Explicitly null
                .build();

        Board whiteBoard = Board.builder()
                .id(11L)
                .name("Board")
                .width(800)
                .height(600)
                .backgroundColor("#FFFFFF")
                .owner(testOwner)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("owneruser")).thenReturn(Optional.of(testOwner));
        when(boardRepository.save(any(Board.class))).thenReturn(whiteBoard);
        when(userService.mapToResponse(testOwner))
                .thenReturn(new UserResponse(1L, "owneruser", "owner@example.com", "Board Owner", LocalDateTime.now()));

        BoardResponse response = boardService.createBoard(request, "owneruser");

        assertThat(response.getBackgroundColor()).isEqualTo("#FFFFFF");
    }

    // ==================== Get Board Tests ====================

    /**
     * Happy path: retrieving an existing board by ID.
     */
    @Test
    @DisplayName("getBoardById - should return BoardResponse for existing board")
    void getBoardById_shouldReturnBoard() {
        when(boardRepository.findById(10L)).thenReturn(Optional.of(testBoard));
        when(userService.mapToResponse(testOwner))
                .thenReturn(new UserResponse(1L, "owneruser", "owner@example.com", "Board Owner", LocalDateTime.now()));

        BoardResponse response = boardService.getBoardById(10L, "owneruser");

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Test Board");
    }

    /**
     * Edge case: requesting a board that doesn't exist throws 404.
     */
    @Test
    @DisplayName("getBoardById - should throw ResourceNotFoundException for missing board")
    void getBoardById_shouldThrowWhenBoardNotFound() {
        when(boardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.getBoardById(999L, "owneruser"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ==================== Update Board Tests ====================

    /**
     * Happy path: owner updates a board's name and dimensions.
     */
    @Test
    @DisplayName("updateBoard - should update name and dimensions when called by owner")
    void updateBoard_shouldApplyUpdatesWhenCalledByOwner() {
        UpdateBoardRequest request = UpdateBoardRequest.builder()
                .name("Updated Board Name")
                .width(2560)
                .height(1440)
                .build();

        Board updatedBoard = Board.builder()
                .id(10L)
                .name("Updated Board Name")
                .width(2560)
                .height(1440)
                .backgroundColor("#FFFFFF")
                .owner(testOwner)
                .createdAt(testBoard.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(boardRepository.findById(10L)).thenReturn(Optional.of(testBoard));
        when(boardRepository.save(any(Board.class))).thenReturn(updatedBoard);
        when(userService.mapToResponse(testOwner))
                .thenReturn(new UserResponse(1L, "owneruser", "owner@example.com", "Board Owner", LocalDateTime.now()));

        BoardResponse response = boardService.updateBoard(10L, request, "owneruser");

        assertThat(response.getName()).isEqualTo("Updated Board Name");
        assertThat(response.getWidth()).isEqualTo(2560);
        assertThat(response.getHeight()).isEqualTo(1440);
    }

    /**
     * Security test: a non-owner attempting to update a board is rejected.
     */
    @Test
    @DisplayName("updateBoard - should throw IllegalArgumentException when called by non-owner")
    void updateBoard_shouldThrowWhenCalledByNonOwner() {
        UpdateBoardRequest request = UpdateBoardRequest.builder()
                .name("Hijacked Board")
                .build();

        when(boardRepository.findById(10L)).thenReturn(Optional.of(testBoard));

        assertThatThrownBy(() -> boardService.updateBoard(10L, request, "otheruser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner");

        verify(boardRepository, never()).save(any());
    }

    // ==================== Delete Board Tests ====================

    /**
     * Happy path: owner successfully deletes their board.
     */
    @Test
    @DisplayName("deleteBoard - should delete board when called by owner")
    void deleteBoard_shouldDeleteWhenCalledByOwner() {
        when(boardRepository.findById(10L)).thenReturn(Optional.of(testBoard));

        boardService.deleteBoard(10L, "owneruser");

        verify(boardRepository).delete(testBoard);
    }

    /**
     * Security test: a non-owner cannot delete the board.
     */
    @Test
    @DisplayName("deleteBoard - should throw IllegalArgumentException when called by non-owner")
    void deleteBoard_shouldThrowWhenCalledByNonOwner() {
        when(boardRepository.findById(10L)).thenReturn(Optional.of(testBoard));

        assertThatThrownBy(() -> boardService.deleteBoard(10L, "nottheowner"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(boardRepository, never()).delete(any());
    }

    // ==================== getBoardsByOwner Tests ====================

    /**
     * Happy path: retrieving all boards for a given user.
     */
    @Test
    @DisplayName("getBoardsByOwner - should return all boards for the given user")
    void getBoardsByOwner_shouldReturnAllBoards() {
        Board secondBoard = Board.builder()
                .id(11L)
                .name("Second Board")
                .width(800)
                .height(600)
                .backgroundColor("#F0F0F0")
                .owner(testOwner)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("owneruser")).thenReturn(Optional.of(testOwner));
        when(boardRepository.findByOwnerOrderByCreatedAtDesc(testOwner))
                .thenReturn(List.of(testBoard, secondBoard));
        when(userService.mapToResponse(testOwner))
                .thenReturn(new UserResponse(1L, "owneruser", "owner@example.com", "Board Owner", LocalDateTime.now()));

        List<BoardResponse> boards = boardService.getBoardsByOwner("owneruser");

        assertThat(boards).hasSize(2);
        assertThat(boards.get(0).getName()).isEqualTo("Test Board");
        assertThat(boards.get(1).getName()).isEqualTo("Second Board");
    }
}
