package com.whiteboard.app.model;

/**
 * Enum representing the supported element types on a whiteboard canvas.
 * Each type corresponds to a different visual component that users can draw or place.
 */
public enum ElementType {

    /**
     * Geometric shapes such as rectangles, circles, triangles, etc.
     */
    SHAPE,

    /**
     * Text elements placed anywhere on the canvas with configurable font and style.
     */
    TEXT,

    /**
     * Sticky note elements, typically rendered as colored rectangles with text content.
     */
    STICKY_NOTE,

    /**
     * Image elements embedded or linked into the canvas.
     */
    IMAGE,

    /**
     * Freehand drawing paths created by the user drawing with a pointer device.
     */
    FREEHAND
}
