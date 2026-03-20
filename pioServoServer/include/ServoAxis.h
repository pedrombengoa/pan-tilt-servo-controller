#pragma once
#include <ESP32Servo.h>

class ServoAxis {
public:
    explicit ServoAxis(bool reversed = false);

    void begin(int pin, int neutral);
    void moveStep(int direction, int speed);
    void writeTo(int angle);
    void reset(int neutral);

    int angle() const { return angle_; }
    bool isAttached() { return servo_.attached(); }
    void setReversed(bool rev) { reversed_ = rev; }
    bool isReversed() const { return reversed_; }

private:
    Servo servo_;
    int angle_ = 90;
    bool reversed_;
};
