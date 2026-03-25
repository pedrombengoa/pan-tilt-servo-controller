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

    /** Set the speed of pan movements. */
    CONFIG_PAN_SPEED("CONFIG_PAN_SPEED"),

    /** Set the speed of tilt movements. */
    CONFIG_TILT_SPEED("CONFIG_TILT_SPEED"),

    /** Set the speed of autopan movements. */
    CONFIG_AUTOPAN_SPEED("CONFIG_AUTOPAN_SPEED"),

    /** Set whether pan servo is reversed. */
    CONFIG_PAN_REVERSED("CONFIG_PAN_REVERSED"),

    /** Set whether tilt servo is reversed. */
    CONFIG_TILT_REVERSED("CONFIG_TILT_REVERSED"),

    /** Set the maximum pan angle (0–270). */
    MAX_PAN_ANGLE("MAX_PAN_ANGLE"),

    /** Set the maximum tilt angle (0–270). */
    MAX_TILT_ANGLE("MAX_TILT_ANGLE");
}
