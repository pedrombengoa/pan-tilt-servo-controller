#include "PanTiltController.h"

PanTiltController::PanTiltController()
    : logger_(bt_),
      pan_(true),        // panServoReversed = true
      tilt_(false),      // tiltServoReversed = false
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

    logger_.logServoStatus(Config::PIN_SERVO_TILT, tilt_.isAttached());

    logger_.log("Ready. Press button to toggle auto slow panning.");
    logger_.log("Move joystick significantly → auto mode disabled.");

    joystick_.calibrate();

    delay(1000);
}

void PanTiltController::movePan(int direction, const String& source) {
    pan_.moveStep(direction, movementSpeed_);
    Command cmd = (direction > 0) ? Command::RIGHT : Command::LEFT;
    lastManualDirection_ = (direction > 0) ? 1 : -1;
    logger_.logCommand(source, cmd, pan_.angle(), tilt_.angle());
}

void PanTiltController::moveTilt(int direction, const String& source) {
    tilt_.moveStep(direction, movementSpeed_);
    Command cmd = (direction > 0) ? Command::UP : Command::DOWN;
    logger_.logCommand(source, cmd, pan_.angle(), tilt_.angle());
}

void PanTiltController::resetSettings() {
    joystick_.setCentroX(Config::DEFAULT_CENTRO_X);
    deadzone_ = Config::DEFAULT_DEADZONE;
    neutral_ = Config::DEFAULT_NEUTRAL;
    movementSpeed_ = Config::DEFAULT_MOVEMENT_SPEED;

    pan_.reset(neutral_);
    tilt_.reset(neutral_);

    autoPanner_.reset();
    lastManualDirection_ = 0;

    logger_.logResetBanner(pan_.angle(), tilt_.angle());
}

void PanTiltController::processBTCommands() {
    if (!bt_.available()) return;

    String raw = bt_.readStringUntil('\n');
    raw.trim();
    Command cmd = parseCommand(raw);

    switch (cmd) {
        case Command::LEFT:
            movePan(-1, "bluetooth");
            break;
        case Command::RIGHT:
            movePan(1, "bluetooth");
            break;
        case Command::UP:
            moveTilt(1, "bluetooth");
            break;
        case Command::DOWN:
            moveTilt(-1, "bluetooth");
            break;
        case Command::RESET:
            resetSettings();
            logger_.logResetComplete();
            break;
        case Command::AUTOPAN:
            autoPanner_.toggle(lastManualDirection_);
            logger_.logAutoPanState(autoPanner_.isActive());
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
        resetSettings();
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
    AutoPanner::UpdateResult result = autoPanner_.update(pan_, movementSpeed_);
    if (result.moved) {
        logger_.logCommand("AutoPan", Command::AUTOPAN, pan_.angle(), tilt_.angle(), result.shouldLogBT);
    }
}

void PanTiltController::update() {
    processBTCommands();
    processJoystickInput();
    logger_.processQueue();
    delay(10);
}
