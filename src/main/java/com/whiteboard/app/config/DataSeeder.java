package com.whiteboard.app.config;

import com.whiteboard.app.model.*;
import com.whiteboard.app.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Seeds demo users, teams, boards, elements, and permissions on startup.
 * Runs in both dev and prod profiles. Skips seeding boards/teams if data already exists.
 */
@Component
@Profile({"dev", "prod"})
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BoardRepository boardRepository;
    private final ElementRepository elementRepository;
    private final TeamRepository teamRepository;
    private final AccessPermissionRepository permissionRepository;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      BoardRepository boardRepository,
                      ElementRepository elementRepository,
                      TeamRepository teamRepository,
                      AccessPermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.boardRepository = boardRepository;
        this.elementRepository = elementRepository;
        this.teamRepository = teamRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void run(String... args) {
        // --- Users ---
        User srinidhi = createUserIfNotExists("srinidhi", "srinidhi@example.com", "password123", "Srinidhi Vutkoori");
        User alice = createUserIfNotExists("alice", "alice@example.com", "password123", "Alice Johnson");
        User bob = createUserIfNotExists("bob", "bob@example.com", "password123", "Bob Williams");
        log.info("Demo users seeded");

        // Only seed boards/teams if no boards exist yet (avoid duplicate data on restart)
        if (boardRepository.count() > 0) {
            log.info("Seed data already present, skipping boards/teams/elements");
            return;
        }

        // --- Teams ---
        Team engineering = createTeam("Engineering", srinidhi, Set.of(alice, bob));
        Team design = createTeam("Design", alice, Set.of(srinidhi));
        Team product = createTeam("Product", bob, Set.of(srinidhi, alice));
        log.info("Demo teams seeded");

        // --- Board 1: Sprint Planning (srinidhi) ---
        Board sprintBoard = createBoard("Sprint Planning \u2013 Q2 2026", 1600, 1000, "#F8FAFC", srinidhi);
        // Column headers
        createElement(sprintBoard, ElementType.SHAPE, 40, 30, 360, 50, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#3B82F6\",\"strokeColor\":\"#2563EB\",\"strokeWidth\":1}", 0);
        createElement(sprintBoard, ElementType.TEXT, 50, 38, 340, 36, "TO DO",
                "{\"fontSize\":20,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#FFFFFF\"}", 1);
        createElement(sprintBoard, ElementType.SHAPE, 420, 30, 360, 50, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#F59E0B\",\"strokeColor\":\"#D97706\",\"strokeWidth\":1}", 0);
        createElement(sprintBoard, ElementType.TEXT, 430, 38, 340, 36, "IN PROGRESS",
                "{\"fontSize\":20,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#FFFFFF\"}", 1);
        createElement(sprintBoard, ElementType.SHAPE, 800, 30, 360, 50, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#10B981\",\"strokeColor\":\"#059669\",\"strokeWidth\":1}", 0);
        createElement(sprintBoard, ElementType.TEXT, 810, 38, 340, 36, "DONE",
                "{\"fontSize\":20,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#FFFFFF\"}", 1);
        // Sticky notes in columns
        createElement(sprintBoard, ElementType.STICKY_NOTE, 50, 110, 180, 120, "Set up CI/CD pipeline for microservices",
                "{\"noteColor\":\"#DBEAFE\",\"fontSize\":13}", 2);
        createElement(sprintBoard, ElementType.STICKY_NOTE, 50, 250, 180, 120, "Add integration tests for auth module",
                "{\"noteColor\":\"#DBEAFE\",\"fontSize\":13}", 2);
        createElement(sprintBoard, ElementType.STICKY_NOTE, 240, 110, 180, 120, "Database migration script for v2.0",
                "{\"noteColor\":\"#FEF3C7\",\"fontSize\":13}", 2);
        createElement(sprintBoard, ElementType.STICKY_NOTE, 430, 110, 180, 120, "Implement WebSocket notifications",
                "{\"noteColor\":\"#FEF9C3\",\"fontSize\":13}", 2);
        createElement(sprintBoard, ElementType.STICKY_NOTE, 430, 250, 180, 120, "Design system token migration",
                "{\"noteColor\":\"#FEF9C3\",\"fontSize\":13}", 2);
        createElement(sprintBoard, ElementType.STICKY_NOTE, 810, 110, 180, 120, "User registration API endpoint",
                "{\"noteColor\":\"#D1FAE5\",\"fontSize\":13}", 2);
        createElement(sprintBoard, ElementType.STICKY_NOTE, 810, 250, 180, 120, "Dashboard layout responsive fix",
                "{\"noteColor\":\"#D1FAE5\",\"fontSize\":13}", 2);
        createElement(sprintBoard, ElementType.STICKY_NOTE, 810, 390, 180, 120, "JWT refresh token rotation",
                "{\"noteColor\":\"#D1FAE5\",\"fontSize\":13}", 2);

        // --- Board 2: System Architecture (srinidhi) ---
        Board archBoard = createBoard("System Architecture \u2013 Whiteboard App", 1800, 1100, "#FFFFFF", srinidhi);
        createElement(archBoard, ElementType.TEXT, 550, 20, 700, 40, "System Architecture Overview",
                "{\"fontSize\":26,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#1E293B\"}", 5);
        createElement(archBoard, ElementType.SHAPE, 120, 100, 280, 160, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#EFF6FF\",\"strokeColor\":\"#3B82F6\",\"strokeWidth\":2}", 1);
        createElement(archBoard, ElementType.TEXT, 150, 140, 220, 30, "React Frontend",
                "{\"fontSize\":18,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#1E40AF\"}", 2);
        createElement(archBoard, ElementType.TEXT, 150, 175, 220, 50, "Vite + Tailwind CSS\nWebSocket Client",
                "{\"fontSize\":12,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#64748B\"}", 2);
        createElement(archBoard, ElementType.SHAPE, 550, 100, 280, 160, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#F0FDF4\",\"strokeColor\":\"#16A34A\",\"strokeWidth\":2}", 1);
        createElement(archBoard, ElementType.TEXT, 580, 140, 220, 30, "Spring Boot API",
                "{\"fontSize\":18,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#166534\"}", 2);
        createElement(archBoard, ElementType.TEXT, 580, 175, 220, 50, "REST + STOMP WS\nJWT Auth + RBAC",
                "{\"fontSize\":12,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#64748B\"}", 2);
        createElement(archBoard, ElementType.SHAPE, 550, 380, 280, 140, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#FFF7ED\",\"strokeColor\":\"#EA580C\",\"strokeWidth\":2}", 1);
        createElement(archBoard, ElementType.TEXT, 580, 410, 220, 30, "PostgreSQL",
                "{\"fontSize\":18,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#9A3412\"}", 2);
        createElement(archBoard, ElementType.TEXT, 580, 445, 220, 50, "Users, Boards, Elements\nPermissions, Versions",
                "{\"fontSize\":12,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#64748B\"}", 2);
        createElement(archBoard, ElementType.SHAPE, 980, 100, 260, 140, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#FDF4FF\",\"strokeColor\":\"#A855F7\",\"strokeWidth\":2}", 1);
        createElement(archBoard, ElementType.TEXT, 1010, 135, 200, 30, "AWS S3",
                "{\"fontSize\":18,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#7E22CE\"}", 2);
        createElement(archBoard, ElementType.TEXT, 1010, 165, 200, 40, "Static File Hosting\nFrontend Deployment",
                "{\"fontSize\":12,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#64748B\"}", 2);
        createElement(archBoard, ElementType.SHAPE, 980, 380, 260, 140, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#FEF2F2\",\"strokeColor\":\"#EF4444\",\"strokeWidth\":2}", 1);
        createElement(archBoard, ElementType.TEXT, 1010, 410, 200, 30, "AWS EC2",
                "{\"fontSize\":18,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#B91C1C\"}", 2);
        createElement(archBoard, ElementType.TEXT, 1010, 445, 200, 40, "Backend Server\nJava 17 Runtime",
                "{\"fontSize\":12,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#64748B\"}", 2);

        // --- Board 3: UI Wireframes (alice) ---
        Board wireframeBoard = createBoard("Dashboard Wireframes v2", 1400, 900, "#F1F5F9", alice);
        createElement(wireframeBoard, ElementType.SHAPE, 100, 60, 900, 50, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#E2E8F0\",\"strokeColor\":\"#94A3B8\",\"strokeWidth\":1}", 0);
        createElement(wireframeBoard, ElementType.SHAPE, 115, 75, 12, 12, null,
                "{\"shape\":\"ELLIPSE\",\"fillColor\":\"#EF4444\",\"strokeColor\":\"#EF4444\",\"strokeWidth\":0}", 1);
        createElement(wireframeBoard, ElementType.SHAPE, 135, 75, 12, 12, null,
                "{\"shape\":\"ELLIPSE\",\"fillColor\":\"#F59E0B\",\"strokeColor\":\"#F59E0B\",\"strokeWidth\":0}", 1);
        createElement(wireframeBoard, ElementType.SHAPE, 155, 75, 12, 12, null,
                "{\"shape\":\"ELLIPSE\",\"fillColor\":\"#22C55E\",\"strokeColor\":\"#22C55E\",\"strokeWidth\":0}", 1);
        createElement(wireframeBoard, ElementType.SHAPE, 100, 110, 180, 600, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#1E293B\",\"strokeColor\":\"#0F172A\",\"strokeWidth\":1}", 0);
        createElement(wireframeBoard, ElementType.TEXT, 120, 130, 140, 25, "Dashboard",
                "{\"fontSize\":14,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#F8FAFC\"}", 1);
        createElement(wireframeBoard, ElementType.TEXT, 120, 165, 140, 25, "Boards",
                "{\"fontSize\":14,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#94A3B8\"}", 1);
        createElement(wireframeBoard, ElementType.TEXT, 120, 200, 140, 25, "Teams",
                "{\"fontSize\":14,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#94A3B8\"}", 1);
        createElement(wireframeBoard, ElementType.TEXT, 120, 235, 140, 25, "Activity",
                "{\"fontSize\":14,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#94A3B8\"}", 1);
        createElement(wireframeBoard, ElementType.SHAPE, 280, 110, 720, 600, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#FFFFFF\",\"strokeColor\":\"#E2E8F0\",\"strokeWidth\":1}", 0);
        createElement(wireframeBoard, ElementType.SHAPE, 310, 140, 155, 80, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#EFF6FF\",\"strokeColor\":\"#BFDBFE\",\"strokeWidth\":1}", 1);
        createElement(wireframeBoard, ElementType.TEXT, 325, 155, 125, 20, "12",
                "{\"fontSize\":24,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#1E40AF\"}", 2);
        createElement(wireframeBoard, ElementType.TEXT, 325, 185, 125, 20, "Total Boards",
                "{\"fontSize\":11,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#64748B\"}", 2);
        createElement(wireframeBoard, ElementType.SHAPE, 485, 140, 155, 80, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#F0FDF4\",\"strokeColor\":\"#BBF7D0\",\"strokeWidth\":1}", 1);
        createElement(wireframeBoard, ElementType.TEXT, 500, 155, 125, 20, "48",
                "{\"fontSize\":24,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#166534\"}", 2);
        createElement(wireframeBoard, ElementType.TEXT, 500, 185, 125, 20, "Elements",
                "{\"fontSize\":11,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#64748B\"}", 2);
        createElement(wireframeBoard, ElementType.SHAPE, 660, 140, 155, 80, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#FDF4FF\",\"strokeColor\":\"#E9D5FF\",\"strokeWidth\":1}", 1);
        createElement(wireframeBoard, ElementType.TEXT, 675, 155, 125, 20, "3",
                "{\"fontSize\":24,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#7E22CE\"}", 2);
        createElement(wireframeBoard, ElementType.TEXT, 675, 185, 125, 20, "Teams",
                "{\"fontSize\":11,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#64748B\"}", 2);
        createElement(wireframeBoard, ElementType.SHAPE, 310, 250, 510, 200, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#FAFAFA\",\"strokeColor\":\"#E2E8F0\",\"strokeWidth\":1}", 1);
        createElement(wireframeBoard, ElementType.TEXT, 490, 330, 200, 25, "Chart Area",
                "{\"fontSize\":16,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#CBD5E1\"}", 2);
        createElement(wireframeBoard, ElementType.STICKY_NOTE, 850, 140, 160, 100, "Need to add dark mode toggle in the sidebar",
                "{\"noteColor\":\"#FEF08A\",\"fontSize\":12}", 3);

        // --- Board 4: Retrospective (bob) ---
        Board retroBoard = createBoard("Sprint 14 Retrospective", 1400, 900, "#FFFBEB", bob);
        createElement(retroBoard, ElementType.SHAPE, 50, 30, 400, 50, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#22C55E\",\"strokeColor\":\"#16A34A\",\"strokeWidth\":1}", 0);
        createElement(retroBoard, ElementType.TEXT, 140, 38, 200, 36, "Went Well",
                "{\"fontSize\":20,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#FFFFFF\"}", 1);
        createElement(retroBoard, ElementType.SHAPE, 480, 30, 400, 50, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#EF4444\",\"strokeColor\":\"#DC2626\",\"strokeWidth\":1}", 0);
        createElement(retroBoard, ElementType.TEXT, 580, 38, 200, 36, "To Improve",
                "{\"fontSize\":20,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#FFFFFF\"}", 1);
        createElement(retroBoard, ElementType.SHAPE, 910, 30, 400, 50, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#3B82F6\",\"strokeColor\":\"#2563EB\",\"strokeWidth\":1}", 0);
        createElement(retroBoard, ElementType.TEXT, 1010, 38, 200, 36, "Action Items",
                "{\"fontSize\":20,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#FFFFFF\"}", 1);
        createElement(retroBoard, ElementType.STICKY_NOTE, 60, 110, 180, 110, "Team collaboration on code reviews was excellent",
                "{\"noteColor\":\"#DCFCE7\",\"fontSize\":12}", 2);
        createElement(retroBoard, ElementType.STICKY_NOTE, 260, 110, 180, 110, "CI/CD pipeline runs 40% faster after optimization",
                "{\"noteColor\":\"#DCFCE7\",\"fontSize\":12}", 2);
        createElement(retroBoard, ElementType.STICKY_NOTE, 60, 240, 180, 110, "Zero production incidents this sprint",
                "{\"noteColor\":\"#DCFCE7\",\"fontSize\":12}", 2);
        createElement(retroBoard, ElementType.STICKY_NOTE, 490, 110, 180, 110, "Documentation lagging behind feature releases",
                "{\"noteColor\":\"#FEE2E2\",\"fontSize\":12}", 2);
        createElement(retroBoard, ElementType.STICKY_NOTE, 490, 240, 180, 110, "Need better load testing before deployments",
                "{\"noteColor\":\"#FEE2E2\",\"fontSize\":12}", 2);
        createElement(retroBoard, ElementType.STICKY_NOTE, 920, 110, 180, 110, "Schedule doc sprint next week",
                "{\"noteColor\":\"#DBEAFE\",\"fontSize\":12}", 2);
        createElement(retroBoard, ElementType.STICKY_NOTE, 920, 240, 180, 110, "Set up Gatling load tests in CI pipeline",
                "{\"noteColor\":\"#DBEAFE\",\"fontSize\":12}", 2);

        // --- Board 5: Brainstorm (alice) ---
        Board brainstormBoard = createBoard("Feature Brainstorm \u2013 v3.0", 1500, 1000, "#FAFAF9", alice);
        createElement(brainstormBoard, ElementType.SHAPE, 580, 380, 280, 80, null,
                "{\"shape\":\"ELLIPSE\",\"fillColor\":\"#7C3AED\",\"strokeColor\":\"#6D28D9\",\"strokeWidth\":2}", 1);
        createElement(brainstormBoard, ElementType.TEXT, 630, 405, 180, 30, "Whiteboard v3.0",
                "{\"fontSize\":18,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#FFFFFF\"}", 2);
        createElement(brainstormBoard, ElementType.SHAPE, 180, 150, 220, 70, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#DBEAFE\",\"strokeColor\":\"#3B82F6\",\"strokeWidth\":2}", 1);
        createElement(brainstormBoard, ElementType.TEXT, 200, 170, 180, 30, "Real-time Cursors",
                "{\"fontSize\":14,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#1E40AF\"}", 2);
        createElement(brainstormBoard, ElementType.SHAPE, 500, 100, 220, 70, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#FCE7F3\",\"strokeColor\":\"#EC4899\",\"strokeWidth\":2}", 1);
        createElement(brainstormBoard, ElementType.TEXT, 520, 120, 180, 30, "Template Gallery",
                "{\"fontSize\":14,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#BE185D\"}", 2);
        createElement(brainstormBoard, ElementType.SHAPE, 820, 150, 220, 70, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#D1FAE5\",\"strokeColor\":\"#10B981\",\"strokeWidth\":2}", 1);
        createElement(brainstormBoard, ElementType.TEXT, 840, 170, 180, 30, "PDF Export",
                "{\"fontSize\":14,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#065F46\"}", 2);
        createElement(brainstormBoard, ElementType.SHAPE, 180, 600, 220, 70, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#FEF3C7\",\"strokeColor\":\"#F59E0B\",\"strokeWidth\":2}", 1);
        createElement(brainstormBoard, ElementType.TEXT, 200, 620, 180, 30, "Dark Mode",
                "{\"fontSize\":14,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#92400E\"}", 2);
        createElement(brainstormBoard, ElementType.SHAPE, 820, 600, 220, 70, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#FEE2E2\",\"strokeColor\":\"#EF4444\",\"strokeWidth\":2}", 1);
        createElement(brainstormBoard, ElementType.TEXT, 840, 620, 180, 30, "Comments Thread",
                "{\"fontSize\":14,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#991B1B\"}", 2);
        createElement(brainstormBoard, ElementType.SHAPE, 500, 680, 220, 70, null,
                "{\"shape\":\"RECTANGLE\",\"fillColor\":\"#E0E7FF\",\"strokeColor\":\"#6366F1\",\"strokeWidth\":2}", 1);
        createElement(brainstormBoard, ElementType.TEXT, 520, 700, 180, 30, "Mobile App",
                "{\"fontSize\":14,\"fontFamily\":\"sans-serif\",\"strokeColor\":\"#3730A3\"}", 2);

        // --- Permissions: share boards via teams ---
        createPermission(sprintBoard, null, engineering, PermissionLevel.EDIT);
        createPermission(archBoard, null, engineering, PermissionLevel.VIEW);
        createPermission(wireframeBoard, null, design, PermissionLevel.EDIT);
        createPermission(retroBoard, null, product, PermissionLevel.VIEW);
        createPermission(brainstormBoard, null, product, PermissionLevel.EDIT);
        createPermission(retroBoard, alice, null, PermissionLevel.EDIT);

        log.info("Demo boards, elements, teams, and permissions seeded successfully");
    }

    private User createUserIfNotExists(String username, String email, String password, String fullName) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .build();
            User saved = userRepository.save(user);
            log.info("Created demo user: {}", username);
            return saved;
        });
    }

    private Team createTeam(String name, User creator, Set<User> members) {
        return teamRepository.findByName(name).orElseGet(() -> {
            Team team = Team.builder()
                    .name(name)
                    .createdBy(creator)
                    .members(new HashSet<>(members))
                    .build();
            Team saved = teamRepository.save(team);
            log.info("Created demo team: {}", name);
            return saved;
        });
    }

    private Board createBoard(String name, int width, int height, String bgColor, User owner) {
        Board board = Board.builder()
                .name(name)
                .width(width)
                .height(height)
                .backgroundColor(bgColor)
                .owner(owner)
                .build();
        return boardRepository.save(board);
    }

    private void createElement(Board board, ElementType type, double x, double y,
                                double width, double height, String content, String style, int zIndex) {
        Element element = Element.builder()
                .board(board)
                .type(type)
                .x(x)
                .y(y)
                .width(width)
                .height(height)
                .content(content)
                .style(style)
                .zIndex(zIndex)
                .build();
        elementRepository.save(element);
    }

    private void createPermission(Board board, User user, Team team, PermissionLevel level) {
        AccessPermission permission = AccessPermission.builder()
                .board(board)
                .user(user)
                .team(team)
                .permissionLevel(level)
                .build();
        permissionRepository.save(permission);
    }
}
