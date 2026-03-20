#pragma once
#include <Arduino.h>

class ServoAxis;

class AutoPanner {
public:
    struct UpdateResult {
        bool moved;
        bool shouldLogBT;
    };

    void toggle(int lastManualDirection);
    void disable();
    void reset();
    bool isActive() const { return active_; }

    UpdateResult update(ServoAxis& axis, int speed);

private:
    bool active_ = false;
    int direction_ = 1;
    unsigned long lastStep_ = 0;
    int updateCount_ = 0;
};
