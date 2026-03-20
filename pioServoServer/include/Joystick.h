#pragma once
#include <Arduino.h>
#include "Config.h"

struct JoystickReading {
    bool panMoved;
    int panDirection;    // -1 = left, +1 = right (0 if not moved)
    bool tiltMoved;
    int tiltDirection;   // -1 = up, +1 = down (0 if not moved)
};

class Joystick {
public:
    Joystick(int pinX, int pinY, int pinButton);

    void begin();
    void calibrate();

    JoystickReading read(int deadzone) const;

    enum class ButtonEvent { NONE, SHORT_PRESS, LONG_PRESS };
    ButtonEvent updateButton();

    int centroX() const { return centroX_; }
    int centroY() const { return centroY_; }
    void setCentroX(int val) { centroX_ = val; }

private:
    int pinX_, pinY_, pinButton_;
    int centroX_, centroY_;
    bool lastButton_ = false;
    unsigned long pressStart_ = 0;
    bool longPressTriggered_ = false;
};
