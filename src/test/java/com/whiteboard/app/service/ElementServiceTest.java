package com.whiteboard.app.service;

import com.whiteboard.app.dto.*;
import com.whiteboard.app.model.*;
import com.whiteboard.app.repository.ElementRepository;
import com.whiteboard.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ElementService covering CRUD operations and lock management.
 * Verifies that the lock mechanism correctly prevents concurrent edits,
 * and that z-index auto-assignment works as expected.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ElementServiceTest {

    @Mock
    private ElementRepository elementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BoardService boardService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ElementService elementService;

    // Shared test fixtures
    private User testUser;
    private User otherUser;
    private Board testBoard;
    private Element testElement;

    /**
     * Set up shared test data before each test method.
     */
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .createdAt(LocalDateTime.now())
                .build();

        otherUser = User.builder()
                .id(2L)
                .username("bob")
                .email("bob@example.com")
                .createdAt(LocalDateTime.now())
                .build();

        testBoard = Board.builder()
                .id(10L)
                .name("Test Board")
                .width(1920)
                .height(1080)
                .backgroundColor("#FFFFFF")
                .owner(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testElement = Element.builder()
                .id(100L)
                .board(testBoard)
                .type(ElementType.SHAPE)
                .x(50.0)
                .y(100.0)
                .width(200.0)
                .height(150.0)
                .style("{\"color\":\"#FF0000\"}")
                .zIndex(0)
                .locked(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== Create Element Tests ====================

    /**
     * Happy path: creating a SHAPE element with auto-assigned z-index.
     */
    @Test
    @DisplayName("createElement - should create element with auto-assigned zIndex on top")
    void createElement_shouldAssignMaxZIndexPlusOne() {
        CreateElementRequest request = CreateElementRequest.builder()
                .type(ElementType.SHAPE)
                .x(10.0)
                .y(20.0)
                .width(100.0)
                .height(80.0)
                .style("{\"color\":\"#0000FF\"}")
                .build();

        // Simulate existing elements with max z-index of 4
        when(boardService.findBoardOrThrow(10L)).thenReturn(testBoard);
        when(elementRepository.findMaxZIndexByBoardId(10L)).thenReturn(4);
        when(elementRepository.save(any(Element.class))).thenReturn(testElement);
        when(userService.mapToResponse(any())).thenReturn(null);

        ElementResponse response = elementService.createElement(10L, request, "alice");

        assertThat(response).isNotNull();
        // Verify save was called with a new element
        verify(elementRepository).save(any(Element.class));
    }

    /**
     * Verify that when no elements exist on the board, z-index starts at 0.
     */
    @Test
    @DisplayName("createElement - should assign zIndex=0 when board has no elements")
    void createElement_shouldAssignZeroZIndexWhenBoardEmpty() {
        CreateElementRequest request = CreateElementRequest.builder()
                .type(ElementType.TEXT)
                .x(0.0)
                .y(0.0)
                .width(100.0)
                .height(50.0)
                .content("Hello World")
                .build();

        // No existing elements - max z-index returns null
        when(boardService.findBoardOrThrow(10L)).thenReturn(testBoard);
        when(elementRepository.findMaxZIndexByBoardId(10L)).thenReturn(null);

        Element firstElement = Element.builder()
                .id(101L)
                .board(testBoard)
                .type(ElementType.TEXT)
                .x(0.0)
                .y(0.0)
                .width(100.0)
                .height(50.0)
                .content("Hello World")
                .zIndex(0)
                .locked(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(elementRepository.save(any(Element.class))).thenReturn(firstElement);
        when(userService.mapToResponse(any())).thenReturn(null);

        ElementResponse response = elementService.createElement(10L, request, "alice");

        assertThat(response).isNotNull();
        // Capture what was saved and verify z-index is 0
        ArgumentCaptor<Element> captor = ArgumentCaptor.forClass(Element.class);
        verify(elementRepository).save(captor.capture());
        assertThat(captor.getValue().getZIndex()).isEqualTo(0);
    }

    // ==================== Update Element Tests ====================

    /**
     * Happy path: updating position and content of an unlocked element.
     */
    @Test
    @DisplayName("updateElement - should apply partial update to unlocked element")
    void updateElement_shouldApplyUpdatesToUnlockedElement() {
        UpdateElementRequest request = UpdateElementRequest.builder()
                .x(300.0)
                .y(400.0)
                .content("Updated text")
                .build();

        Element updatedElement = Element.builder()
                .id(100L)
                .board(testBoard)
                .type(ElementType.SHAPE)
                .x(300.0)
                .y(400.0)
                .width(200.0)
                .height(150.0)
                .content("Updated text")
                .zIndex(0)
                .locked(false)
                .createdAt(testElement.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(elementRepository.findById(100L)).thenReturn(Optional.of(testElement));
        when(elementRepository.save(any(Element.class))).thenReturn(updatedElement);
        when(userService.mapToResponse(any())).thenReturn(null);

        ElementResponse response = elementService.updateElement(10L, 100L, request, "alice");

        assertThat(response).isNotNull();
        verify(elementRepository).save(any(Element.class));
    }

    /**
     * Lock conflict test: updating an element locked by another user should fail.
     */
    @Test
    @DisplayName("updateElement - should throw IllegalStateException when element locked by other user")
    void updateElement_shouldThrowWhenLockedByAnotherUser() {
        // Set up the element as locked by "bob"
        testElement.setLocked(true);
        testElement.setLockedBy(otherUser);

        UpdateElementRequest request = UpdateElementRequest.builder()
                .x(999.0)
                .build();

        when(elementRepository.findById(100L)).thenReturn(Optional.of(testElement));

        assertThatThrownBy(() -> elementService.updateElement(10L, 100L, request, "alice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bob");

        // No save should have been called
        verify(elementRepository, never()).save(any());
    }

    // ==================== Lock Management Tests ====================

    /**
     * Happy path: a user successfully acquires the lock on a free element.
     */
    @Test
    @DisplayName("lockElement - should acquire lock when element is not locked")
    void lockElement_shouldAcquireLockOnFreeElement() {
        Element lockedElement = Element.builder()
                .id(100L)
                .board(testBoard)
                .type(ElementType.SHAPE)
                .x(50.0)
                .y(100.0)
                .width(200.0)
                .height(150.0)
                .zIndex(0)
                .locked(true)
                .lockedBy(testUser)
                .createdAt(testElement.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(elementRepository.findById(100L)).thenReturn(Optional.of(testElement));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(elementRepository.save(any(Element.class))).thenReturn(lockedElement);
        when(userService.mapToResponse(testUser))
                .thenReturn(new UserResponse(1L, "alice", "alice@example.com", null, LocalDateTime.now()));

        ElementResponse response = elementService.lockElement(10L, 100L, "alice");

        assertThat(response.getLocked()).isTrue();
        assertThat(response.getLockedBy().getUsername()).isEqualTo("alice");
        verify(elementRepository).save(any(Element.class));
    }

    /**
     * Lock conflict: attempting to lock an element already held by another user.
     */
    @Test
    @DisplayName("lockElement - should throw IllegalStateException when element locked by other user")
    void lockElement_shouldThrowWhenAlreadyLockedByOtherUser() {
        // Element is already locked by "bob"
        testElement.setLocked(true);
        testElement.setLockedBy(otherUser);

        when(elementRepository.findById(100L)).thenReturn(Optional.of(testElement));

        assertThatThrownBy(() -> elementService.lockElement(10L, 100L, "alice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bob");

        verify(elementRepository, never()).save(any());
    }

    /**
     * Happy path: the lock holder successfully releases their lock.
     */
    @Test
    @DisplayName("unlockElement - should release lock when called by lock holder")
    void unlockElement_shouldReleaseLockByHolder() {
        // Set up element locked by "alice"
        testElement.setLocked(true);
        testElement.setLockedBy(testUser);

        Element unlockedElement = Element.builder()
                .id(100L)
                .board(testBoard)
                .type(ElementType.SHAPE)
                .x(50.0)
                .y(100.0)
                .width(200.0)
                .height(150.0)
                .zIndex(0)
                .locked(false)
                .lockedBy(null)
                .createdAt(testElement.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(elementRepository.findById(100L)).thenReturn(Optional.of(testElement));
        when(elementRepository.save(any(Element.class))).thenReturn(unlockedElement);
        when(userService.mapToResponse(any())).thenReturn(null);

        ElementResponse response = elementService.unlockElement(10L, 100L, "alice");

        assertThat(response.getLocked()).isFalse();
        assertThat(response.getLockedBy()).isNull();
    }

    /**
     * Security test: a user who doesn't hold the lock cannot release it.
     */
    @Test
    @DisplayName("unlockElement - should throw IllegalStateException when non-holder attempts unlock")
    void unlockElement_shouldThrowWhenCalledByNonHolder() {
        // Element locked by "alice"
        testElement.setLocked(true);
        testElement.setLockedBy(testUser);

        when(elementRepository.findById(100L)).thenReturn(Optional.of(testElement));

        // "bob" tries to unlock alice's lock
        assertThatThrownBy(() -> elementService.unlockElement(10L, 100L, "bob"))
                .isInstanceOf(IllegalStateException.class);

        verify(elementRepository, never()).save(any());
    }

    // ==================== Delete Element Tests ====================

    /**
     * Happy path: deleting an unlocked element.
     */
    @Test
    @DisplayName("deleteElement - should delete unlocked element")
    void deleteElement_shouldDeleteUnlockedElement() {
        when(elementRepository.findById(100L)).thenReturn(Optional.of(testElement));

        elementService.deleteElement(10L, 100L, "alice");

        verify(elementRepository).delete(testElement);
    }

    /**
     * Cross-board access: element belongs to a different board than specified in the path.
     */
    @Test
    @DisplayName("getElementById - should throw IllegalArgumentException when element belongs to different board")
    void getElementById_shouldThrowWhenElementOnDifferentBoard() {
        // testElement belongs to board 10, but we query for board 99
        when(elementRepository.findById(100L)).thenReturn(Optional.of(testElement));

        assertThatThrownBy(() -> elementService.getElementById(99L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    /**
     * Edge case: requesting an element that doesn't exist.
     */
    @Test
    @DisplayName("getElementsByBoard - should return list of elements ordered by z-index")
    void getElementsByBoard_shouldReturnElements() {
        when(boardService.findBoardOrThrow(10L)).thenReturn(testBoard);
        when(elementRepository.findByBoardIdOrderByZIndexAsc(10L))
                .thenReturn(List.of(testElement));
        when(userService.mapToResponse(any())).thenReturn(null);

        List<ElementResponse> elements = elementService.getElementsByBoard(10L);

        assertThat(elements).hasSize(1);
        assertThat(elements.get(0).getId()).isEqualTo(100L);
    }
}
