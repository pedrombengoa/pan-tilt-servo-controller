#pragma once
#include <Arduino.h>
#include "BluetoothSerial.h"
#include "Config.h"
#include "Command.h"

class MessageLogger {
public:
    explicit MessageLogger(BluetoothSerial& bt);

    void log(const String& message);
    void logSerial(const String& message);
    void processQueue();

    // Structured log methods
    void logCommand(const String& channel, Command cmd, int panPosition, int tiltPosition, bool bluetooth = true);
    void logStartup();
    void logAvailableCommands();
    void logServoStatus(int pin, bool attached);
    void logResetBanner(int panPosition, int tiltPosition);
    void logResetComplete();
    void logAutoPanState(bool active);
    void logAutoPanDisabled();
    void logUnknownCommand(const String& raw);

private:
    BluetoothSerial& bt_;
    String queue_[Config::MAX_QUEUE_SIZE];
    int head_ = 0;
    int tail_ = 0;
    int size_ = 0;
    unsigned long lastSendTime_ = 0;

    void enqueue(const String& message);
};
