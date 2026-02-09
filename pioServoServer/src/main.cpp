#include <ESP32Servo.h>
#include "BluetoothSerial.h"   // ← added

Servo servoPan;
BluetoothSerial SerialBT;      // ← added

const int pinVRx = 34;
const int pinSW = 14;           // Joystick button
const int pinServoPan = 18;

// ── Calibration ───────────────────────────────────────
const int centroX = 1928;
const int deadzone = 60;        // increased a bit → less sensitive to noise
const int neutral = 90;         // your stop value

const int manualPace = 5;
const int autoPace = 3;

// ── State variables ───────────────────────────────────
int currentAngle = 90;
bool autoPanningActive = false;
int autoDirection = 1;          // 1 = right (+), -1 = left (-)

void setup() {
  Serial.begin(115200);
  SerialBT.begin("PanTilt");   // ← added: Bluetooth device name
  Serial.println("Bluetooth started! Connect from phone.");
  Serial.println("BT commands: P0..P180, C = center, S = hold");

  pinMode(pinSW, INPUT_PULLUP);

  servoPan.setPeriodHertz(50);
  servoPan.attach(pinServoPan, 500, 2400);
  servoPan.write(neutral);
  
  Serial.println("Ready. Press button to toggle auto slow panning.");
  Serial.println("Move joystick significantly → auto mode disabled.");
  delay(1000);
}

void loop() {
  // ── Bluetooth commands ──────────────────────────────────────
  if (SerialBT.available()) {
    String cmd = SerialBT.readStringUntil('\n');
    cmd.trim();

    if (cmd.startsWith("P")) {
      int target = cmd.substring(1).toInt();
      if (target >= 0 && target <= 180) {
        currentAngle = target;
        servoPan.write(currentAngle);
        SerialBT.printf("Pan set to %d\n", currentAngle);
      }
    }
    else if (cmd == "C") {
      currentAngle = 90;
      servoPan.write(90);
      SerialBT.println("Centered");
    }
    else if (cmd == "S") {
      SerialBT.println("Holding current position");
      // already holding — no extra action needed
    }
  }

  // ── Read inputs ─────────────────────────────────────
  int valorX = analogRead(pinVRx);
  int distance = abs(valorX - centroX);

  // ── Button handling (toggle auto mode) ──────────────
  static bool lastButton = HIGH;
  bool buttonNow = (digitalRead(pinSW) == LOW);

  if (buttonNow && !lastButton) {           // falling edge
    autoPanningActive = !autoPanningActive;
    Serial.println(autoPanningActive ? "AUTO PANNING → ON" : "AUTO PANNING → OFF");
    delay(200);                             // simple debounce
  }
  lastButton = buttonNow;

  int currentDeadzone = autoPanningActive ? 200 : deadzone;
  // ── Joystick movement disables auto mode ────────────
  if (distance > currentDeadzone) {
    if (autoPanningActive) {
      Serial.println("Joystick moved → AUTO PANNING DISABLED");
      autoPanningActive = false;
    }

    // Manual control
    if (valorX < centroX) {
      currentAngle = constrain(currentAngle - manualPace, 0, 180);
    } else {
      currentAngle = constrain(currentAngle + manualPace, 0, 180);
    }
    servoPan.write(currentAngle);
  }

  // ── Auto panning (runs independently when enabled) ──
  if (autoPanningActive) {
    static unsigned long lastStep = 0;
    unsigned long now = millis();

    if (now - lastStep >= 80) {           // ~12 steps/sec - quite slow
      currentAngle += (autoPace * autoDirection);

      // Bounce at ends
      if (currentAngle >= 180) {
        currentAngle = 180;
        autoDirection = -1;
      } else if (currentAngle <= 0) {
        currentAngle = 0;
        autoDirection = 1;
      }

      servoPan.write(currentAngle);
      lastStep = now;

      Serial.print("Auto → ");
      Serial.println(currentAngle);
    }
  }

  delay(10);   // small loop delay
}