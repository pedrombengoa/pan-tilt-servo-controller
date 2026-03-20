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

    logger_.log("Bluetooth started! Connect from phone.");
    logger_.log("BT commands: LEFT, RIGHT, RESET, AUTOPAN, P0..P180, C = center, S = hold");

    joystick_.begin();

    pan_.begin(Config::PIN_SERVO_PAN, neutral_);
    tilt_.begin(Config::PIN_SERVO_TILT, neutral_);

    if (tilt_.isAttached()) {
        logger_.log("Tilt servo attached on pin: " + String(Config::PIN_SERVO_TILT));
    } else {
        logger_.log("Warning: Tilt servo not attached (check pin/wiring)");
    }

    logger_.log("Ready. Press button to toggle auto slow panning.");
    logger_.log("Move joystick significantly → auto mode disabled.");

    joystick_.calibrate();
    logger_.log("[CAL] centroX=" + String(joystick_.centroX()) + " centroY=" + String(joystick_.centroY()));

    delay(1000);
}

void PanTiltController::movePan(int direction, const String& source) {
    pan_.moveStep(direction, movementSpeed_);
    if (direction > 0) {
        lastManualDirection_ = 1;
        logger_.log("Channel: " + source + " | Command: RIGHT | Position: " + String(pan_.angle()));
    } else {
        lastManualDirection_ = -1;
        logger_.log("Channel: " + source + " | Command: LEFT | Position: " + String(pan_.angle()));
    }
}

void PanTiltController::moveTilt(int direction, const String& source) {
    tilt_.moveStep(direction, movementSpeed_);
    if (direction > 0) {
        logger_.log("Channel: " + source + " | Command: UP | Position: " + String(tilt_.angle()));
    } else {
        logger_.log("Channel: " + source + " | Command: DOWN | Position: " + String(tilt_.angle()));
    }
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

    logger_.log("\n╔════════════════════════════════════════╗");
    logger_.log("║  SETTINGS RESET TO DEFAULTS  ║");
    logger_.log("╚════════════════════════════════════════╝");
    logger_.log("Channel: Reset | Command: RESET | Position: " + String(pan_.angle()));
}

void PanTiltController::processBTCommands() {
    if (!bt_.available()) return;

    String cmd = bt_.readStringUntil('\n');
    cmd.trim();

    if (cmd == "LEFT") {
        movePan(-1, "bluetooth");
    }
    else if (cmd == "RIGHT") {
        movePan(1, "bluetooth");
    }
    else if (cmd == "UP") {
        moveTilt(1, "bluetooth");   // BT UP → down (reversed per user config)
    }
    else if (cmd == "DOWN") {
        moveTilt(-1, "bluetooth");  // BT DOWN → up (reversed per user config)
    }
    else if (cmd == "TILTSWEEP") {
        for (int p = 0; p <= 180; p += 30) {
            tilt_.writeTo(p);
            logger_.log("Channel: Test | Command: TILTSWEEP | Position: " + String(p));
            delay(150);
        }
        for (int p = 180; p >= 0; p -= 30) {
            tilt_.writeTo(p);
            logger_.log("Channel: Test | Command: TILTSWEEP | Position: " + String(p));
            delay(150);
        }
        tilt_.writeTo(neutral_);
    }
    else if (cmd == "RESET") {
        resetSettings();
        logger_.log("Reset to defaults");
    }
    else if (cmd == "AUTOPAN") {
        autoPanner_.toggle(lastManualDirection_);
        logger_.log(autoPanner_.isActive() ? "AUTO PANNING → ON" : "AUTO PANNING → OFF");
    }
    else {
        logger_.log("Unknown command: " + cmd);
    }
}

void PanTiltController::processJoystickInput() {
    // Button handling
    Joystick::ButtonEvent btnEvent = joystick_.updateButton();
    if (btnEvent == Joystick::ButtonEvent::LONG_PRESS) {
        resetSettings();
    } else if (btnEvent == Joystick::ButtonEvent::SHORT_PRESS) {
        autoPanner_.toggle(lastManualDirection_);
        logger_.log(autoPanner_.isActive() ? "AUTO PANNING → ON" : "AUTO PANNING → OFF");
    }

    // Joystick movement
    int currentDeadzone = autoPanner_.isActive() ? Config::AUTO_PAN_DEADZONE : deadzone_;
    JoystickReading reading = joystick_.read(currentDeadzone);

    if (reading.panMoved || reading.tiltMoved) {
        if (autoPanner_.isActive() && reading.panMoved) {
            logger_.log("Joystick moved → AUTO PANNING DISABLED");
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
        if (result.shouldLogBT) {
            logger_.log("Channel: AutoPan | Command: Move | Position: " + String(pan_.angle()));
        } else {
            logger_.logSerial("Channel: AutoPan | Command: Move | Position: " + String(pan_.angle()));
        }
    }
}

void PanTiltController::update() {
    processBTCommands();
    processJoystickInput();
    logger_.processQueue();
    delay(10);
}
