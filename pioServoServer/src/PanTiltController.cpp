#include "PanTiltController.h"

PanTiltController::PanTiltController()
    : logger_(bt_),
      pan_(Config::PAN_SERVO_REVERSED),
      tilt_(Config::TILT_SERVO_REVERSED),
      joystick_(Config::PIN_VRX, Config::PIN_VRY, Config::PIN_SW) {}

void PanTiltController::begin() {
    Serial.begin(115200);
    delay(100);
    Serial.println("[BOOT] Serial initialized at 115200");
    Serial.flush();

    bt_.begin("PanTilt");

    logger_.logStartup();

    joystick_.begin();

    pan_.begin(Config::PIN_SERVO_PAN, neutral_);
    tilt_.begin(Config::PIN_SERVO_TILT, neutral_);

    pan_.setMaxAngle(Config::DEFAULT_MAX_PAN_ANGLE);
    tilt_.setMaxAngle(Config::DEFAULT_MAX_TILT_ANGLE);

    logger_.logServoStatus(Config::PIN_SERVO_TILT, tilt_.isAttached());

    logger_.log("Ready. Press button to toggle auto slow panning.");
    logger_.log("Move joystick significantly → auto mode disabled.");

    joystick_.calibrate();

    delay(1000);
}

void PanTiltController::movePan(int direction, const String& source) {
    unsigned long now = millis();
    if (now - lastPanStep_ < servoStepMs_) return;
    lastPanStep_ = now;
    pan_.moveStep(direction, 1);
    Command cmd = (direction > 0) ? Command::RIGHT : Command::LEFT;
    lastManualDirection_ = (direction > 0) ? 1 : -1;
    logger_.logCommand(source, cmd, pan_.angle(), tilt_.angle());
}

void PanTiltController::moveTilt(int direction, const String& source) {
    unsigned long now = millis();
    if (now - lastTiltStep_ < servoStepMs_) return;
    lastTiltStep_ = now;
    tilt_.moveStep(direction, 1);
    Command cmd = (direction > 0) ? Command::UP : Command::DOWN;
    logger_.logCommand(source, cmd, pan_.angle(), tilt_.angle());
}

void PanTiltController::resetPosition() {
    pan_.reset(neutral_);
    tilt_.reset(neutral_);

    autoPanner_.reset();
    lastManualDirection_ = 0;

    logger_.log("Position reset to neutral (" + String(neutral_) + ")");
}

void PanTiltController::resetConfig() {
    joystick_.setCentroX(Config::DEFAULT_CENTRO_X);
    deadzone_ = Config::DEFAULT_DEADZONE;
    neutral_ = Config::DEFAULT_NEUTRAL;
    servoStepMs_ = Config::DEFAULT_SERVO_STEP_MS;
    autoPanStepMs_ = Config::DEFAULT_AUTO_PAN_STEP_MS;

    pan_.setReversed(Config::PAN_SERVO_REVERSED);
    tilt_.setReversed(Config::TILT_SERVO_REVERSED);
    pan_.setMaxAngle(Config::DEFAULT_MAX_PAN_ANGLE);
    tilt_.setMaxAngle(Config::DEFAULT_MAX_TILT_ANGLE);

    logger_.logResetBanner(pan_.angle(), tilt_.angle());
    logCurrentConfig();
}

void PanTiltController::processBTCommands() {
    if (!bt_.available()) return;

    String raw = bt_.readStringUntil('\n');
    raw.trim();

    ParsedCommand parsed = parseCommand(raw);

    switch (parsed.command) {
        case Command::LEFT:
            autoPanner_.disable();
            movePan(-1, "bluetooth");
            break;
        case Command::RIGHT:
            autoPanner_.disable();
            movePan(1, "bluetooth");
            break;
        case Command::UP:
            moveTilt(1, "bluetooth");
            break;
        case Command::DOWN:
            moveTilt(-1, "bluetooth");
            break;
        case Command::RESET_POSITION:
            resetPosition();
            logger_.logResetComplete();
            break;
        case Command::RESET_CONFIG:
            resetConfig();
            logger_.logResetComplete();
            break;
        case Command::AUTOPAN:
            autoPanner_.toggle(lastManualDirection_);
            logger_.logAutoPanState(autoPanner_.isActive());
            break;
        case Command::CONFIG_PAN_REVERSED:
            pan_.setReversed(parsed.value != 0);
            logger_.log("Config set: pan_reversed = " + String(parsed.value));
            break;
        case Command::CONFIG_TILT_REVERSED:
            tilt_.setReversed(parsed.value != 0);
            logger_.log("Config set: tilt_reversed = " + String(parsed.value));
            break;
        case Command::MAX_PAN_ANGLE:
            pan_.setMaxAngle(constrain(parsed.value, 0, 270));
            logger_.log("Config set: max_pan_angle = " + String(parsed.value));
            break;
        case Command::MAX_TILT_ANGLE:
            tilt_.setMaxAngle(constrain(parsed.value, 0, 270));
            logger_.log("Config set: max_tilt_angle = " + String(parsed.value));
            break;
        case Command::CONFIG_SERVO_STEP_MS:
            servoStepMs_ = max(10UL, (unsigned long)parsed.value);
            logger_.log("Config set: CONFIG_SERVO_STEP_MS = " + String(servoStepMs_));
            break;
        case Command::CONFIG_AUTO_PAN_STEP_MS:
            autoPanStepMs_ = max(10UL, (unsigned long)parsed.value);
            logger_.log("Config set: CONFIG_AUTO_PAN_STEP_MS = " + String(autoPanStepMs_));
            break;
        case Command::UNKNOWN:
            logger_.logUnknownCommand(raw);
            break;
    }
}

void PanTiltController::processJoystickInput() {
    // Button handling
    Joystick::ButtonEvent btnEvent = joystick_.updateButton();
    if (btnEvent == Joystick::ButtonEvent::LONG_PRESS) {
        resetConfig();
        resetPosition();
        logger_.logResetComplete();
    } else if (btnEvent == Joystick::ButtonEvent::SHORT_PRESS) {
        autoPanner_.toggle(lastManualDirection_);
        logger_.logAutoPanState(autoPanner_.isActive());
    }

    // Joystick movement
    int currentDeadzone = autoPanner_.isActive() ? Config::AUTO_PAN_DEADZONE : deadzone_;
    JoystickReading reading = joystick_.read(currentDeadzone);

    if (reading.panMoved || reading.tiltMoved) {
        if (autoPanner_.isActive() && reading.panMoved) {
            logger_.logAutoPanDisabled();
            autoPanner_.disable();
        }

        if (reading.panMoved) {
            movePan(reading.panDirection, "Joystick");
        }

        if (reading.tiltMoved) {
            moveTilt(reading.tiltDirection, "Joystick");
        }
    }

    // Auto panning
    AutoPanner::UpdateResult result = autoPanner_.update(pan_, 1, autoPanStepMs_);
    if (result.moved) {
        logger_.logCommand("AutoPan", Command::AUTOPAN, pan_.angle(), tilt_.angle(), result.shouldLogBT);
    }
}

void PanTiltController::logCurrentConfig() {
    logger_.log("CONFIG_PAN_REVERSED:" + String(pan_.isReversed() ? 1 : 0));
    logger_.log("CONFIG_TILT_REVERSED:" + String(tilt_.isReversed() ? 1 : 0));
    logger_.log("MAX_PAN_ANGLE:" + String(pan_.maxAngle()));
    logger_.log("MAX_TILT_ANGLE:" + String(tilt_.maxAngle()));
    logger_.log("CONFIG_SERVO_STEP_MS:" + String(servoStepMs_));
    logger_.log("CONFIG_AUTO_PAN_STEP_MS:" + String(autoPanStepMs_));
}

void PanTiltController::update() {
    bool connected = bt_.hasClient();
    if (connected && !btConnected_) {
        logger_.log("Bluetooth client connected.");
        logCurrentConfig();
    } else if (!connected && btConnected_) {
        logger_.logSerial("Bluetooth client disconnected.");
    }
    btConnected_ = connected;

    processBTCommands();
    processJoystickInput();
    logger_.processQueue();
    delay(10);
}
