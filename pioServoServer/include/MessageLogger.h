#pragma once
#include <Arduino.h>
#include "BluetoothSerial.h"
#include "Config.h"

class MessageLogger {
public:
    explicit MessageLogger(BluetoothSerial& bt);

    void log(const String& message);
    void logSerial(const String& message);
    void processQueue();

private:
    BluetoothSerial& bt_;
    String queue_[Config::MAX_QUEUE_SIZE];
    int head_ = 0;
    int tail_ = 0;
    int size_ = 0;
    unsigned long lastSendTime_ = 0;

    void enqueue(const String& message);
};
