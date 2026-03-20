#pragma once
#include "BluetoothSerial.h"
#include "MessageLogger.h"
#include "ServoAxis.h"
#include "Joystick.h"
#include "AutoPanner.h"
#include "Config.h"

class PanTiltController {
public:
    PanTiltController();

    void begin();
    void update();

private:
    BluetoothSerial bt_;
    MessageLogger logger_;
    ServoAxis pan_;
    ServoAxis tilt_;
    Joystick joystick_;
    AutoPanner autoPanner_;

    int movementSpeed_ = Config::DEFAULT_MOVEMENT_SPEED;
    int deadzone_ = Config::DEFAULT_DEADZONE;
    int neutral_ = Config::DEFAULT_NEUTRAL;
    int lastManualDirection_ = 0;

    void processBTCommands();
    void processJoystickInput();
    void resetSettings();

    void movePan(int direction, const String& source);
    void moveTilt(int direction, const String& source);
};
