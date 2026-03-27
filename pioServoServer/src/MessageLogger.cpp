#include "MessageLogger.h"

MessageLogger::MessageLogger(BluetoothSerial& bt) : bt_(bt) {}

void MessageLogger::enqueue(const String& message) {
    if (size_ < Config::MAX_QUEUE_SIZE) {
        queue_[tail_] = message;
        tail_ = (tail_ + 1) % Config::MAX_QUEUE_SIZE;
        size_++;
    }
}

void MessageLogger::processQueue() {
    unsigned long now = millis();
    if (size_ > 0 && bt_.hasClient() && (now - lastSendTime_ >= Config::BT_SEND_INTERVAL_MS)) {
        bt_.println(queue_[head_]);
        head_ = (head_ + 1) % Config::MAX_QUEUE_SIZE;
        size_--;
        lastSendTime_ = now;
    }
}

void MessageLogger::log(const String& message) {
    Serial.println(message);
    Serial.flush();
    enqueue(message);
}

void MessageLogger::logSerial(const String& message) {
    Serial.println(message);
    Serial.flush();
}

void MessageLogger::logCommand(const String& channel, Command cmd, int panPosition, int tiltPosition, bool bluetooth) {
    String message = "Channel: " + channel + " | Command: " + commandToString(cmd) + " | Pan: " + String(panPosition) + " | Tilt: " + String(tiltPosition);
    if (bluetooth) {
        log(message);
    } else {
        logSerial(message);
    }
}

void MessageLogger::logStartup() {
    log("Bluetooth started! Connect from phone.");
    logAvailableCommands();
}

void MessageLogger::logAvailableCommands() {
    log("BT commands: " + String(commandToString(Command::LEFT)) + ", " +
        commandToString(Command::RIGHT) + ", " +
        commandToString(Command::UP) + ", " +
        commandToString(Command::DOWN) + ", " +
        commandToString(Command::RESET_POSITION) + ", " +
        commandToString(Command::RESET_CONFIG) + ", " +
        commandToString(Command::AUTOPAN));
}

void MessageLogger::logServoStatus(int pin, bool attached) {
    if (attached) {
        log("Tilt servo attached on pin: " + String(pin));
    } else {
        log("Warning: Tilt servo not attached (check pin/wiring)");
    }
}

void MessageLogger::logResetBanner(int panPosition, int tiltPosition) {
    log("\n╔════════════════════════════════════════╗");
    log("║  SETTINGS RESET TO DEFAULTS  ║");
    log("╚════════════════════════════════════════╝");
    logCommand("Reset", Command::RESET_CONFIG, panPosition, tiltPosition);
}

void MessageLogger::logResetComplete() {
    log("Reset to defaults");
}

void MessageLogger::logAutoPanState(bool active) {
    log(active ? "AUTO PANNING → ON" : "AUTO PANNING → OFF");
}

void MessageLogger::logAutoPanDisabled() {
    log("Joystick moved → AUTO PANNING DISABLED");
}

void MessageLogger::logUnknownCommand(const String& raw) {
    log("Unknown command: " + raw);
}
