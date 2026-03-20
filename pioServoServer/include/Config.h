#pragma once
#include <Arduino.h>

namespace Config {
    // Pin assignments
    constexpr int PIN_VRX = 34;
    constexpr int PIN_VRY = 35;
    constexpr int PIN_SW = 14;
    constexpr int PIN_SERVO_PAN = 18;
    constexpr int PIN_SERVO_TILT = 19;

    // Default calibration values
    constexpr int DEFAULT_CENTRO_X = 1928;
    constexpr int DEFAULT_CENTRO_Y = 1928;
    constexpr int DEFAULT_DEADZONE = 800;
    constexpr int DEFAULT_NEUTRAL = 90;
    constexpr int DEFAULT_MOVEMENT_SPEED = 1;

    // Timing
    constexpr unsigned long LONG_PRESS_MS = 2000;
    constexpr unsigned long AUTO_PAN_STEP_MS = 80;
    constexpr unsigned long BT_SEND_INTERVAL_MS = 200;
    constexpr int AUTO_PAN_LOG_EVERY = 10;
    constexpr int AUTO_PAN_DEADZONE = 200;

    // Message queue
    constexpr int MAX_QUEUE_SIZE = 50;
}
