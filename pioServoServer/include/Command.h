#pragma once
#include <Arduino.h>

enum class Command {
    LEFT,
    RIGHT,
    UP,
    DOWN,
    RESET,
    AUTOPAN,
    UNKNOWN
};

inline Command parseCommand(const String& str) {
    if (str == "LEFT")      return Command::LEFT;
    if (str == "RIGHT")     return Command::RIGHT;
    if (str == "UP")        return Command::UP;
    if (str == "DOWN")      return Command::DOWN;
    if (str == "RESET")     return Command::RESET;
    if (str == "AUTOPAN")   return Command::AUTOPAN;
    return Command::UNKNOWN;
}

inline const char* commandToString(Command cmd) {
    switch (cmd) {
        case Command::LEFT:    return "LEFT";
        case Command::RIGHT:   return "RIGHT";
        case Command::UP:      return "UP";
        case Command::DOWN:    return "DOWN";
        case Command::RESET:   return "RESET";
        case Command::AUTOPAN: return "AUTOPAN";
        case Command::UNKNOWN: return "UNKNOWN";
    }
    return "UNKNOWN";
}
