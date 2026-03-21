package com.whiteboard.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiteboard.app.dto.*;
import com.whiteboard.app.exception.ResourceNotFoundException;
import com.whiteboard.app.model.Board;
import com.whiteboard.app.model.Element;
import com.whiteboard.app.model.User;
import com.whiteboard.app.model.VersionSnapshot;
import com.whiteboard.app.repository.ElementRepository;
import com.whiteboard.app.repository.UserRepository;
import com.whiteboard.app.repository.VersionSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for creating and retrieving version snapshots of whiteboards.
 * A snapshot serializes the complete state of all elements on a board at a point in time.
 * Snapshots enable version history browsing and board state restoration (replay).
 *
 * The snapshot data is stored as JSON, making it easy to deserialize and re-apply
 * to the board when replaying a historical version.
 */
@Service
@Slf4j
public class VersionSnapshotService {

    private final VersionSnapshotRepository snapshotRepository;
    private final ElementRepository elementRepository;
    private final UserRepository userRepository;
    private final BoardService boardService;
    private final UserService userService;
    private final ElementService elementService;
    private final ObjectMapper objectMapper;
    private final ActivityLogService activityLogService;

    /**
     * Constructor injection of all required dependencies.
     *
     * @param snapshotRepository data access for version snapshots
     * @param elementRepository  fetches current elements to serialize into snapshot
     * @param userRepository     fetches the user creating the snapshot
     * @param boardService       validates and fetches board entities
     * @param userService        maps User entities to DTOs
     * @param objectMapper       Jackson mapper for JSON serialization of element state
     */
    public VersionSnapshotService(VersionSnapshotRepository snapshotRepository,
                                   ElementRepository elementRepository,
                                   UserRepository userRepository,
                                   BoardService boardService,
                                   UserService userService,
                                   ElementService elementService,
                                   ObjectMapper objectMapper,
                                   ActivityLogService activityLogService) {
        this.snapshotRepository = snapshotRepository;
        this.elementRepository = elementRepository;
        this.userRepository = userRepository;
        this.boardService = boardService;
        this.userService = userService;
        this.elementService = elementService;
        this.objectMapper = objectMapper;
        this.activityLogService = activityLogService;
    }

    /**
     * Create a new snapshot of the board's current state.
     * Serializes all current elements on the board into a JSON string
     * and stores it as the snapshot data.
     *
     * @param boardId  the board to snapshot
     * @param request  optional description for the snapshot
     * @param username the user creating the snapshot
     * @return the created snapshot as a response DTO
     * @throws IllegalStateException if JSON serialization fails
     */
    @Transactional
    public SnapshotResponse createSnapshot(Long boardId, CreateSnapshotRequest request, String username) {
        Board board = boardService.findBoardOrThrow(boardId);
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Fetch all current elements for this board ordered by z-index
        List<Element> elements = elementRepository.findByBoardIdOrderByZIndexAsc(boardId);

        // Convert to DTOs to avoid Hibernate lazy-loading serialization issues
        List<ElementResponse> elementDtos = elements.stream()
                .map(elementService::mapToResponse)
                .collect(Collectors.toList());

        // Serialize the element DTOs to JSON for storage
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(elementDtos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize board state for snapshot: " + e.getMessage(), e);
        }

        VersionSnapshot snapshot = VersionSnapshot.builder()
                .board(board)
                .snapshotData(snapshotJson)
                .createdBy(creator)
                .description(request != null ? request.getDescription() : null)
                .build();

        VersionSnapshot saved = snapshotRepository.save(snapshot);
        log.info("Snapshot created: id={} for board={} by user='{}' with {} elements",
                saved.getId(), boardId, username, elements.size());
        activityLogService.logActivity("SNAPSHOT_SAVED",
                "Saved version snapshot of board '" + board.getName() + "' (" + elements.size() + " elements)",
                creator, board);
        return mapToResponse(saved);
    }

    /**
     * Retrieve all snapshots for a board, newest first.
     * This provides the complete version history for the board.
     *
     * @param boardId the board whose history to retrieve
     * @return list of snapshots ordered by creation time descending
     */
    @Transactional(readOnly = true)
    public List<SnapshotResponse> getSnapshotsByBoard(Long boardId) {
        boardService.findBoardOrThrow(boardId); // Validate board exists
        return snapshotRepository.findByBoardIdOrderByCreatedAtDesc(boardId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a single snapshot by its ID.
     * The snapshotData field contains the full JSON board state for replay.
     *
     * @param boardId    the board the snapshot belongs to (for validation)
     * @param snapshotId the snapshot's database ID
     * @return the snapshot as a response DTO
     * @throws ResourceNotFoundException if no snapshot with that ID exists
     * @throws IllegalArgumentException  if the snapshot does not belong to the board
     */
    @Transactional(readOnly = true)
    public SnapshotResponse getSnapshotById(Long boardId, Long snapshotId) {
        VersionSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("VersionSnapshot", "id", snapshotId));

        // Ensure the snapshot belongs to the requested board
        if (!snapshot.getBoard().getId().equals(boardId)) {
            throw new IllegalArgumentException(
                    "Snapshot " + snapshotId + " does not belong to board " + boardId);
        }

        return mapToResponse(snapshot);
    }

    /**
     * Map a VersionSnapshot entity to its response DTO.
     *
     * @param snapshot the entity to map
     * @return the response DTO
     */
    private SnapshotResponse mapToResponse(VersionSnapshot snapshot) {
        return SnapshotResponse.builder()
                .id(snapshot.getId())
                .boardId(snapshot.getBoard().getId())
                .snapshotData(snapshot.getSnapshotData())
                .createdBy(userService.mapToResponse(snapshot.getCreatedBy()))
                .createdAt(snapshot.getCreatedAt())
                .description(snapshot.getDescription())
                .build();
    }
}
