#pragma once
#include "BluetoothSerial.h"
#include "MessageLogger.h"
#include "ServoAxis.h"
#include "Joystick.h"
#include "AutoPanner.h"
#include "Command.h"
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

    int panSpeed_ = Config::DEFAULT_PAN_SPEED;
    int tiltSpeed_ = Config::DEFAULT_TILT_SPEED;
    int autoPanSpeed_ = Config::DEFAULT_AUTOPAN_SPEED;
    int deadzone_ = Config::DEFAULT_DEADZONE;
    int neutral_ = Config::DEFAULT_NEUTRAL;
    int lastManualDirection_ = 0;
    bool btConnected_ = false;

    void processBTCommands();
    void processJoystickInput();
    void resetSettings();

    void movePan(int direction, int speed, const String& source);
    void moveTilt(int direction, int speed, const String& source);
};
