#include <ESP32Servo.h>
#include "BluetoothSerial.h"   // ← added

Servo servoPan;
BluetoothSerial SerialBT;      // ← added

// ── Message Queue for BT to prevent congestion ─────
#define MAX_QUEUE_SIZE 50
String messageQueue[MAX_QUEUE_SIZE];
int queueHead = 0;
int queueTail = 0;
int queueSize = 0;

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
const int DEFAULT_MOVEMENT_SPEED = 1;

// ── Calibration ───────────────────────────────────────
int centroX = DEFAULT_CENTRO_X;
int deadzone = DEFAULT_DEADZONE;
int neutral = DEFAULT_NEUTRAL;
int movementSpeed = DEFAULT_MOVEMENT_SPEED;

// ── State variables ───────────────────────────────────
int currentAngle = 90;
bool autoPanningActive = false;
int autoDirection = 1;          // 1 = right (+), -1 = left (-)
// track last button state to avoid spurious toggles at startup
bool lastButton = HIGH;
// ── Forward declarations ──────────────────────────────
void log(const String& message);
void moveLeft();
void moveRight();
void processBTCommands();
void processConfigCommands(const String& cmd);
void processJoystickCommands();
// ── Reset function ────────────────────────────────────
void resetSettings() {
  centroX = DEFAULT_CENTRO_X;
  deadzone = DEFAULT_DEADZONE;
  neutral = DEFAULT_NEUTRAL;
  movementSpeed = DEFAULT_MOVEMENT_SPEED;
  
  // Reset servo position to neutral
  currentAngle = neutral;
  servoPan.write(neutral);
  
  // Disable auto panning
  autoPanningActive = false;
  autoDirection = 1;
  
  log("\n╔════════════════════════════════════════╗");
  log("║  SETTINGS RESET TO DEFAULTS  ║");
  log("╚════════════════════════════════════════╝");
}
void setup() {
  Serial.begin(115200);
  delay(100);
  // sanity check: ensure Serial is writable immediately after begin
  Serial.println("[BOOT] Serial initialized at 115200");
  Serial.flush();
  SerialBT.begin("PanTilt");   // ← added: Bluetooth device name
  // Ensure autopanning is off on startup
  autoPanningActive = false;

  log("Bluetooth started! Connect from phone.");
  log("BT commands: LEFT, RIGHT, RESET, AUTOPAN, P0..P180, C = center, S = hold");

  pinMode(pinSW, INPUT_PULLUP);

  // Initialize lastButton with current physical state to avoid immediate toggles
  lastButton = (digitalRead(pinSW) == LOW);

  servoPan.setPeriodHertz(50);
  servoPan.attach(pinServoPan, 500, 2400);
  servoPan.write(neutral);
  
  log("Ready. Press button to toggle auto slow panning.");
  log("Move joystick significantly → auto mode disabled.");
  delay(1000);
}

// ──────────────────────────────────────────────────────────────
// ── Logging Method (with queue to prevent BT congestion) ─────────
// ──────────────────────────────────────────────────────────────

void enqueueMessage(const String& message) {
  if (queueSize < MAX_QUEUE_SIZE) {
    messageQueue[queueTail] = message;
    queueTail = (queueTail + 1) % MAX_QUEUE_SIZE;
    queueSize++;
  }
}

void processMessageQueue() {
  // Process one message per loop iteration to avoid blocking
  // Throttle sends to prevent BT buffer overflow
  static unsigned long lastSendTime = 0;
  unsigned long now = millis();
  
  if (queueSize > 0 && SerialBT.hasClient() && (now - lastSendTime >= 200)) {
    Serial.println("Logging to BT: " + messageQueue[queueHead]);
    SerialBT.println(messageQueue[queueHead]);
    queueHead = (queueHead + 1) % MAX_QUEUE_SIZE;
    queueSize--;
    lastSendTime = now;
  }
}

void log(const String& message) {
  Serial.println(message);
  Serial.flush();
  // Queue the message for Bluetooth instead of blocking
  enqueueMessage(message);
}

// ──────────────────────────────────────────────────────────────
// ── Movement Methods ───────────────────────────────────────────
// ──────────────────────────────────────────────────────────────

void moveLeft() {
  currentAngle = constrain(currentAngle - movementSpeed, 0, 180);
  servoPan.write(currentAngle);
  log("Moving Left → " + String(currentAngle));
}

void moveRight() {
  currentAngle = constrain(currentAngle + movementSpeed, 0, 180);
  servoPan.write(currentAngle);
  log("Moving Right → " + String(currentAngle));
}

// ──────────────────────────────────────────────────────────────
// ── Bluetooth Command Processors ───────────────────────────────
// ──────────────────────────────────────────────────────────────

void processBTCommands() {
  if (!SerialBT.available()) return;
  
  String cmd = SerialBT.readStringUntil('\n');
  cmd.trim();

  // ── Pan control commands ────────────────────────────
  if (cmd == "LEFT") {
    moveRight();
  }
  else if (cmd == "RIGHT") {
    moveLeft();
  }
  else if (cmd == "RESET") {
    resetSettings();
    log("Reset to defaults");
  }
  else if (cmd == "AUTOPAN") {
    autoPanningActive = !autoPanningActive;
    log(autoPanningActive ? "AUTO PANNING → ON" : "AUTO PANNING → OFF");
  }
  else {
    log("Unknown command: " + cmd);
  }
}

void processJoystickCommands() {
  // ── Read inputs ─────────────────────────────────────
  int valorX = analogRead(pinVRx);
  int distance = abs(valorX - centroX);

  // ── Button handling (toggle auto mode / long press to reset) ──
  // use global `lastButton` initialized in setup to avoid startup toggles
  bool buttonNow = (digitalRead(pinSW) == LOW);

  if (buttonNow && !lastButton) {
    buttonPressStart = millis();
    longPressTriggered = false;
    delay(200);
  }
  
  if (buttonNow && !longPressTriggered) {
    if (millis() - buttonPressStart >= LONG_PRESS_DURATION) {
      longPressTriggered = true;
      resetSettings();
    }
  }
  
  if (!buttonNow && lastButton) {
    if (!longPressTriggered && (millis() - buttonPressStart) < LONG_PRESS_DURATION) {
      autoPanningActive = !autoPanningActive;
      log(autoPanningActive ? "AUTO PANNING → ON" : "AUTO PANNING → OFF");
    }
  }
  lastButton = buttonNow;

  int currentDeadzone = autoPanningActive ? 200 : deadzone;
  
  // ── Joystick movement disables auto mode ────────────
  if (distance > currentDeadzone) {
    if (autoPanningActive) {
      log("Joystick moved → AUTO PANNING DISABLED");
      autoPanningActive = false;
    }

    // Manual control
    if (valorX < centroX) {
      moveLeft();
    } else {
      moveRight();
    }
  }

  // ── Auto panning (runs independently when enabled) ──
  if (autoPanningActive) {
    static unsigned long lastStep = 0;
    unsigned long now = millis();

    if (now - lastStep >= 80) {
      currentAngle += (movementSpeed * autoDirection);

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

      log("Auto → " + String(currentAngle));
    }
  }
}

void loop() {
  processBTCommands();
  processJoystickCommands();
  processMessageQueue();  // ← Process queued BT messages to prevent congestion
  delay(10);
}
