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
    void setMaxAngle(int max) { maxAngle_ = constrain(max, 0, 270); }
    int maxAngle() const { return maxAngle_; }

    int minLimit() const { return constrain(neutral_ - maxAngle_ / 2, 0, 270); }
    int maxLimit() const { return constrain(neutral_ + maxAngle_ / 2, 0, 270); }

private:
    Servo servo_;
    int angle_ = 90;
    int neutral_ = 90;
    int maxAngle_ = 180;
    bool reversed_;
};
