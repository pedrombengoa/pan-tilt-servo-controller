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
