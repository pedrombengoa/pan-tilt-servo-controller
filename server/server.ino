#include <ESP32Servo.h>

Servo servoPan;

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
  pinMode(pinSW, INPUT_PULLUP);

  servoPan.setPeriodHertz(50);
  servoPan.attach(pinServoPan, 500, 2400);
  servoPan.write(neutral);
  
  Serial.println("Ready. Press button to toggle auto slow panning.");
  Serial.println("Move joystick significantly → auto mode disabled.");
  delay(1000);
}

void loop() {
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

    // Manual control (optional - remove if you want pure auto/manual separation)
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