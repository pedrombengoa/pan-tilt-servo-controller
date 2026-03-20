#include "ServoAxis.h"

ServoAxis::ServoAxis(bool reversed) : reversed_(reversed) {}

void ServoAxis::begin(int pin, int neutral) {
    angle_ = neutral;
    servo_.setPeriodHertz(50);
    servo_.attach(pin, 500, 2400);
    servo_.write(neutral);
}

void ServoAxis::moveStep(int direction, int speed) {
    int actualDir = reversed_ ? -direction : direction;
    angle_ = constrain(angle_ + actualDir * speed, 0, 180);
    servo_.write(angle_);
}

void ServoAxis::writeTo(int angle) {
    angle_ = constrain(angle, 0, 180);
    servo_.write(angle_);
}

void ServoAxis::reset(int neutral) {
    angle_ = neutral;
    servo_.write(neutral);
}
