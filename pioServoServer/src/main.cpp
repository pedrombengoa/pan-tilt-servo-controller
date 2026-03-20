#include "PanTiltController.h"

PanTiltController controller;

void setup() {
    controller.begin();
}

void loop() {
    controller.update();
}

