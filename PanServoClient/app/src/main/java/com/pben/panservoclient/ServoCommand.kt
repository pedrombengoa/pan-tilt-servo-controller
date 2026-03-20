package com.pben.panservoclient

/**
 * Enum representing all supported servo commands
 * that can be sent over Bluetooth to the PanTilt device.
 */
enum class ServoCommand(val value: String) {
    /** Pan the servo to the left. */
    LEFT("LEFT"),

    /** Pan the servo to the right. */
    RIGHT("RIGHT"),

    /** Tilt the servo upward. */
    UP("UP"),

    /** Tilt the servo downward. */
    DOWN("DOWN"),

    /** Toggle automated panning mode. */
    AUTOPAN("AUTOPAN"),

    /** Stop all movement and center the servos. */
    RESET("RESET"),

    /** Request device info on connection. */
    INFO("INFO");
}

