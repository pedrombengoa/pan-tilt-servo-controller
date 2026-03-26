# Project: Pan-Tilt Servo Controller (PlatformIO / ESP32)

## Tech Stack
- ESP32 (esp32dev) with Arduino framework via PlatformIO
- ESP32Servo library for servo control
- BluetoothSerial for wireless communication

## Coding Conventions
- C++ with Arduino idioms
- `#pragma once` for include guards
- Member variables use trailing underscore: `servo_`, `active_`
- Configuration constants in `Config` namespace using `constexpr`
- Composition over inheritance
- Code comments in English; project notes may be in Spanish

## Architecture
- `PanTiltController` is the main orchestrator, composing `ServoAxis`, `Joystick`, `AutoPanner`, `MessageLogger`
- `ServoAxis` wraps individual servo control (direction reversing, angle constraints, stepping)
- `Command.h` defines the command enum, parser, and toString — keep all three in sync when adding commands
- Bluetooth commands are processed in `PanTiltController::processBTCommands()`

## Hardware
- Joystick: pins 34 (VRx-tilt), 35 (VRy-pan), 14 (SW-button)
- Servos: pins 18 (pan), 19 (tilt)
- Pan servo is reversed by default
