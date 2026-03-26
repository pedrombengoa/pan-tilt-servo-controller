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

    int deadzone_ = Config::DEFAULT_DEADZONE;
    int neutral_ = Config::DEFAULT_NEUTRAL;
    unsigned long servoStepMs_ = Config::DEFAULT_SERVO_STEP_MS;
    unsigned long autoPanStepMs_ = Config::DEFAULT_AUTO_PAN_STEP_MS;
    unsigned long lastPanStep_ = 0;
    unsigned long lastTiltStep_ = 0;
    int lastManualDirection_ = 0;
    bool btConnected_ = false;

    void processBTCommands();
    void processJoystickInput();
    void resetSettings();

    void movePan(int direction, const String& source);
    void moveTilt(int direction, const String& source);
    void logCurrentConfig();
};
