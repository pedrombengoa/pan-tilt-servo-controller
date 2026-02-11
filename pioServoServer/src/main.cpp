#include <ESP32Servo.h>
#include "BluetoothSerial.h"   // ← added

Servo servoPan;
BluetoothSerial SerialBT;      // ← added

const int pinVRx = 34;
const int pinSW = 14;           // Joystick button
const int pinServoPan = 18;

// ── Long press detection ───────────────────────────────
const unsigned long LONG_PRESS_DURATION = 2000;  // 2 seconds for long press
unsigned long buttonPressStart = 0;
bool longPressTriggered = false;

// ── Default calibration values ────────────────────────
const int DEFAULT_CENTRO_X = 1928;
const int DEFAULT_DEADZONE = 60;
const int DEFAULT_NEUTRAL = 90;
const int DEFAULT_MANUAL_PACE = 1;
const int DEFAULT_AUTO_PACE = 1;

// ── Calibration ───────────────────────────────────────
int centroX = DEFAULT_CENTRO_X;
int deadzone = DEFAULT_DEADZONE;
int neutral = DEFAULT_NEUTRAL;

int manualPace = DEFAULT_MANUAL_PACE;
int autoPace = DEFAULT_AUTO_PACE;

// ── State variables ───────────────────────────────────
int currentAngle = 90;
bool autoPanningActive = false;
int autoDirection = 1;          // 1 = right (+), -1 = left (-)
// ── Reset function ────────────────────────────────────
void resetSettings() {
  centroX = DEFAULT_CENTRO_X;
  deadzone = DEFAULT_DEADZONE;
  neutral = DEFAULT_NEUTRAL;
  manualPace = DEFAULT_MANUAL_PACE;
  autoPace = DEFAULT_AUTO_PACE;
  
  // Reset servo position to neutral
  currentAngle = neutral;
  servoPan.write(neutral);
  
  // Disable auto panning
  autoPanningActive = false;
  autoDirection = 1;
  
  Serial.println("\n╔════════════════════════════════════════╗");
  Serial.println("║  SETTINGS RESET TO DEFAULTS  ║");
  Serial.println("╚════════════════════════════════════════╝");
  SerialBT.println("\n╔════════════════════════════════════════╗");
  SerialBT.println("║  SETTINGS RESET TO DEFAULTS  ║");
  SerialBT.println("╚════════════════════════════════════════╝");
}
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

    // ── Pan control commands ────────────────────────────
    if (cmd.startsWith("P")) {
      int target = cmd.substring(1).toInt();
      if (target >= 0 && target <= 180) {
        currentAngle = target;
        servoPan.write(currentAngle);
        Serial.printf("Pan set to %d\n", currentAngle);
        SerialBT.printf("Pan set to %d\n", currentAngle);
      }
    }
    else if (cmd == "C") {
      currentAngle = 90;
      servoPan.write(90);
      Serial.println("Centered");
      SerialBT.println("Centered");
    }
    else if (cmd == "S") {
      Serial.println("Holding current position");
      SerialBT.println("Holding current position");
      // already holding — no extra action needed
    }
    
    // ── Calibration commands ────────────────────────────
    else if (cmd.startsWith("CAL_X:")) {
      int newCentroX = cmd.substring(6).toInt();
      if (newCentroX >= 0 && newCentroX <= 4095) {
        centroX = newCentroX;
        Serial.printf("CentroX set to %d\n", centroX);
        SerialBT.printf("CentroX set to %d\n", centroX);
      } else {
        Serial.println("CAL_X: Value must be 0-4095");
        SerialBT.println("CAL_X: Value must be 0-4095");
      }
    }
    else if (cmd.startsWith("CAL_DZ:")) {
      int newDeadzone = cmd.substring(7).toInt();
      if (newDeadzone >= 0 && newDeadzone <= 500) {
        deadzone = newDeadzone;
        Serial.printf("Deadzone set to %d\n", deadzone);
        SerialBT.printf("Deadzone set to %d\n", deadzone);
      } else {
        Serial.println("CAL_DZ: Value must be 0-500");
        SerialBT.println("CAL_DZ: Value must be 0-500");
      }
    }
    else if (cmd.startsWith("CAL_N:")) {
      int newNeutral = cmd.substring(6).toInt();
      if (newNeutral >= 0 && newNeutral <= 180) {
        neutral = newNeutral;
        servoPan.write(neutral);
        currentAngle = neutral;
        Serial.printf("Neutral set to %d\n", neutral);
        SerialBT.printf("Neutral set to %d\n", neutral);
      } else {
        Serial.println("CAL_N: Value must be 0-180");
        SerialBT.println("CAL_N: Value must be 0-180");
      }
    }
    
    // ── Panning commands ────────────────────────────────
    else if (cmd.startsWith("PAN_MP:")) {
      int newManualPace = cmd.substring(7).toInt();
      if (newManualPace >= 1 && newManualPace <= 20) {
        manualPace = newManualPace;
        Serial.printf("Manual Pace set to %d\n", manualPace);
        SerialBT.printf("Manual Pace set to %d\n", manualPace);
      } else {
        Serial.println("PAN_MP: Value must be 1-20");
        SerialBT.println("PAN_MP: Value must be 1-20");
      }
    }
    else if (cmd.startsWith("PAN_AP:")) {
      int newAutoPace = cmd.substring(7).toInt();
      if (newAutoPace >= 1 && newAutoPace <= 20) {
        autoPace = newAutoPace;
        Serial.printf("Auto Pace set to %d\n", autoPace);
        SerialBT.printf("Auto Pace set to %d\n", autoPace);
      } else {
        Serial.println("PAN_AP: Value must be 1-20");
        SerialBT.println("PAN_AP: Value must be 1-20");
      }
    }
    
    // ── Get current settings ────────────────────────────
    else if (cmd == "INFO") {
      Serial.println("=== CALIBRATION ===");
      SerialBT.println("=== CALIBRATION ===");
      Serial.printf("CentroX: %d\n", centroX);
      SerialBT.printf("CentroX: %d\n", centroX);
      Serial.printf("Deadzone: %d\n", deadzone);
      SerialBT.printf("Deadzone: %d\n", deadzone);
      Serial.printf("Neutral: %d\n", neutral);
      SerialBT.printf("Neutral: %d\n", neutral);
      Serial.println("=== PANNING ===");
      SerialBT.println("=== PANNING ===");
      Serial.printf("Manual Pace: %d\n", manualPace);
      SerialBT.printf("Manual Pace: %d\n", manualPace);
      Serial.printf("Auto Pace: %d\n", autoPace);
      SerialBT.printf("Auto Pace: %d\n", autoPace);
    }
  }

  // ── Read inputs ─────────────────────────────────────
  int valorX = analogRead(pinVRx);
  int distance = abs(valorX - centroX);

  // ── Button handling (toggle auto mode / long press to reset) ──
  static bool lastButton = HIGH;
  bool buttonNow = (digitalRead(pinSW) == LOW);

  if (buttonNow && !lastButton) {           // button pressed (falling edge)
    buttonPressStart = millis();
    longPressTriggered = false;
    delay(200);                             // simple debounce
  }
  
  if (buttonNow && !longPressTriggered) {   // button still held and long press not yet triggered
    if (millis() - buttonPressStart >= LONG_PRESS_DURATION) {
      longPressTriggered = true;
      resetSettings();
    }
  }
  
  if (!buttonNow && lastButton) {           // button released (rising edge)
    if (!longPressTriggered && (millis() - buttonPressStart) < LONG_PRESS_DURATION) {
      // Short press → toggle auto panning
      autoPanningActive = !autoPanningActive;
      Serial.println(autoPanningActive ? "AUTO PANNING → ON" : "AUTO PANNING → OFF");
    }
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