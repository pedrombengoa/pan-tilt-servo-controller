#include "AutoPanner.h"
#include "ServoAxis.h"
#include "Config.h"

void AutoPanner::toggle(int lastManualDirection) {
    active_ = !active_;
    if (active_ && lastManualDirection != 0) {
        direction_ = lastManualDirection;
    }
}

void AutoPanner::disable() {
    active_ = false;
}

void AutoPanner::reset() {
    active_ = false;
    direction_ = 1;
    updateCount_ = 0;
}

AutoPanner::UpdateResult AutoPanner::update(ServoAxis& axis, int speed) {
    UpdateResult result = {false, false};
    if (!active_) return result;

    unsigned long now = millis();
    if (now - lastStep_ < Config::AUTO_PAN_STEP_MS) return result;

    int newAngle = axis.angle() + speed * direction_;

    if (newAngle >= 180) {
        newAngle = 180;
        direction_ = -1;
    } else if (newAngle <= 0) {
        newAngle = 0;
        direction_ = 1;
    }

    axis.writeTo(newAngle);
    lastStep_ = now;
    result.moved = true;

    updateCount_++;
    if (updateCount_ >= Config::AUTO_PAN_LOG_EVERY) {
        result.shouldLogBT = true;
        updateCount_ = 0;
    }

    return result;
}
