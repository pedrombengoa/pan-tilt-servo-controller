#pragma once
#include <Arduino.h>

enum class Command {
    LEFT,
    RIGHT,
    UP,
    DOWN,
    RESET,
    AUTOPAN,
    CONFIG_PAN_REVERSED,
    CONFIG_TILT_REVERSED,
    MAX_PAN_ANGLE,
    MAX_TILT_ANGLE,
    CONFIG_SERVO_STEP_MS,
    CONFIG_AUTO_PAN_STEP_MS,
    UNKNOWN
};

struct ParsedCommand {
    Command command;
    int value;
};

inline ParsedCommand parseCommand(const String& str) {
    String upper = str;
    upper.toUpperCase();

    if (upper == "LEFT")      return { Command::LEFT, 0 };
    if (upper == "RIGHT")     return { Command::RIGHT, 0 };
    if (upper == "UP")        return { Command::UP, 0 };
    if (upper == "DOWN")      return { Command::DOWN, 0 };
    if (upper == "RESET")     return { Command::RESET, 0 };
    if (upper == "AUTOPAN")   return { Command::AUTOPAN, 0 };

    int sep = upper.indexOf(':');
    if (sep > 0) {
        String key = upper.substring(0, sep);
        int val = str.substring(sep + 1).toInt();

        if (key == "CONFIG_PAN_REVERSED")      return { Command::CONFIG_PAN_REVERSED, val };
        if (key == "CONFIG_TILT_REVERSED")     return { Command::CONFIG_TILT_REVERSED, val };
        if (key == "MAX_PAN_ANGLE")            return { Command::MAX_PAN_ANGLE, val };
        if (key == "MAX_TILT_ANGLE")           return { Command::MAX_TILT_ANGLE, val };
        if (key == "CONFIG_SERVO_STEP_MS")     return { Command::CONFIG_SERVO_STEP_MS, val };
        if (key == "CONFIG_AUTO_PAN_STEP_MS")  return { Command::CONFIG_AUTO_PAN_STEP_MS, val };
    }

    return { Command::UNKNOWN, 0 };
}

inline const char* commandToString(Command cmd) {
    switch (cmd) {
        case Command::LEFT:                    return "LEFT";
        case Command::RIGHT:                   return "RIGHT";
        case Command::UP:                      return "UP";
        case Command::DOWN:                    return "DOWN";
        case Command::RESET:                   return "RESET";
        case Command::AUTOPAN:                 return "AUTOPAN";
        case Command::CONFIG_PAN_REVERSED:     return "CONFIG_PAN_REVERSED";
        case Command::CONFIG_TILT_REVERSED:    return "CONFIG_TILT_REVERSED";
        case Command::MAX_PAN_ANGLE:           return "MAX_PAN_ANGLE";
        case Command::MAX_TILT_ANGLE:          return "MAX_TILT_ANGLE";
        case Command::CONFIG_SERVO_STEP_MS:    return "CONFIG_SERVO_STEP_MS";
        case Command::CONFIG_AUTO_PAN_STEP_MS: return "CONFIG_AUTO_PAN_STEP_MS";
        case Command::UNKNOWN:                 return "UNKNOWN";
    }
    return "UNKNOWN";
}
