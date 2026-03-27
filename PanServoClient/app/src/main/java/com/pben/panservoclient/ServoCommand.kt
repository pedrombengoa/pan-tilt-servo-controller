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

    /** Stop all movement, center servos, and disable auto-pan. */
    RESET_POSITION("RESET_POSITION"),

    /** Restore all config settings to factory defaults. */
    RESET_CONFIG("RESET_CONFIG"),

    /** Set whether pan servo is reversed (0=normal, 1=reversed). Default: 1. */
    CONFIG_PAN_REVERSED("CONFIG_PAN_REVERSED"),

    /** Set whether tilt servo is reversed (0=normal, 1=reversed). Default: 0. */
    CONFIG_TILT_REVERSED("CONFIG_TILT_REVERSED"),

    /** Set the maximum pan angle (0–270, default 180). */
    MAX_PAN_ANGLE("MAX_PAN_ANGLE"),

    /** Set the maximum tilt angle (0–270, default 50). */
    MAX_TILT_ANGLE("MAX_TILT_ANGLE"),

    /** Set the servo step delay in milliseconds (min 10, default 50). */
    CONFIG_SERVO_STEP_MS("CONFIG_SERVO_STEP_MS"),

    /** Set the auto-pan step delay in milliseconds (min 10, default 100). */
    CONFIG_AUTO_PAN_STEP_MS("CONFIG_AUTO_PAN_STEP_MS");
}
