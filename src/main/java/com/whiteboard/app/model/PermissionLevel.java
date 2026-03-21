package com.whiteboard.app.model;

/**
 * Enum defining the access permission levels for whiteboard boards.
 * Permissions follow a hierarchical model: ADMIN > EDIT > VIEW.
 */
public enum PermissionLevel {

    /**
     * Read-only access - users can view the board and its history but cannot make changes.
     */
    VIEW,

    /**
     * Edit access - users can create, modify, and delete elements on the board.
     */
    EDIT,

    /**
     * Full administrative access - users can manage permissions, delete the board,
     * and perform all edit operations.
     */
    ADMIN
}
