#include "Joystick.h"

Joystick::Joystick(int pinX, int pinY, int pinButton)
    : pinX_(pinX), pinY_(pinY), pinButton_(pinButton),
      centroX_(Config::DEFAULT_CENTRO_X), centroY_(Config::DEFAULT_CENTRO_Y) {}

void Joystick::begin() {
    pinMode(pinButton_, INPUT_PULLUP);
    lastButton_ = (digitalRead(pinButton_) == LOW);
}

void Joystick::calibrate() {
    centroX_ = analogRead(pinX_);
    centroY_ = analogRead(pinY_);
}

JoystickReading Joystick::read(int deadzone) const {
    JoystickReading r = {false, 0, false, 0};

    int valorX = analogRead(pinX_);
    int valorY = analogRead(pinY_);
    int distX = abs(valorX - centroX_);
    int distY = abs(valorY - centroY_);

    r.panMoved = (distX > deadzone);
    if (r.panMoved) {
        r.panDirection = (valorX < centroX_) ? 1 : -1;
    }

    r.tiltMoved = (distY > deadzone);
    if (r.tiltMoved) {
        r.tiltDirection = (valorY < centroY_) ? -1 : 1;
    }

    return r;
}

Joystick::ButtonEvent Joystick::updateButton() {
    bool buttonNow = (digitalRead(pinButton_) == LOW);
    ButtonEvent event = ButtonEvent::NONE;

    if (buttonNow && !lastButton_) {
        pressStart_ = millis();
        longPressTriggered_ = false;
        delay(200);
    }

    if (buttonNow && !longPressTriggered_) {
        if (millis() - pressStart_ >= Config::LONG_PRESS_MS) {
            longPressTriggered_ = true;
            event = ButtonEvent::LONG_PRESS;
        }
    }

    if (!buttonNow && lastButton_) {
        if (!longPressTriggered_ && (millis() - pressStart_) < Config::LONG_PRESS_MS) {
            event = ButtonEvent::SHORT_PRESS;
        }
    }

    lastButton_ = buttonNow;
    return event;
}
