---
applyTo: "**/Command.h,**/PanTiltController.*,**/ServoAxis.*,**/AutoPanner.*"
description: "Use when working with Bluetooth commands, command parsing, servo API, or adding/modifying commands."
---

# Pan-Tilt Servo Controller — Bluetooth API Reference

Commands are sent as plain-text strings over Bluetooth Serial.
Parsing is case-insensitive. Parameterized commands use `KEY:VALUE` format.

---

## Movement Commands

| Command  | Format   | Effect                           |
|----------|----------|----------------------------------|
| `LEFT`   | no param | Move pan servo left by 1°        |
| `RIGHT`  | no param | Move pan servo right by 1°       |
| `UP`     | no param | Move tilt servo up by 1°         |
| `DOWN`   | no param | Move tilt servo down by 1°       |

- Speed is governed by `CONFIG_SERVO_STEP_MS` (one step per interval).
- LEFT/RIGHT disable auto-pan. UP/DOWN do **not**.
- If reversed flag is set, directions are inverted.

## Control Commands

| Command          | Format   | Effect                                          |
|------------------|----------|--------------------------------------------------|
| `RESET_POSITION` | no param | Move both servos to neutral, stop auto-panner    |
| `RESET_CONFIG`   | no param | Restore all config settings to factory defaults  |
| `AUTOPAN`        | no param | Toggle automatic pan sweeping                    |

### RESET_POSITION details
Moves both servos to neutral (90°), disables auto-panner, clears last manual direction.

### RESET_CONFIG details
Restores: `servoStepMs`=50, `deadzone`=800, `neutral`=90°,
pan reversed=true, tilt reversed=false, pan maxAngle=180°, tilt maxAngle=50°.

### AUTOPAN details
- Oscillates pan servo between its min/max limits at the current step speed.
- Resumes the last manual direction if available, otherwise sweeps right first.
- Disabled automatically by: LEFT, RIGHT, significant joystick movement, or RESET_POSITION.

## Configuration Commands

Format: `COMMAND_NAME:value` (integer value)

| Command                    | Default | Min  | Max | Effect                                     |
|----------------------------|---------|------|-----|--------------------------------------------|
| `CONFIG_PAN_REVERSED:val`  | 1       | 0    | 1   | Invert LEFT/RIGHT direction (0=normal, 1=reversed) |
| `CONFIG_TILT_REVERSED:val` | 0       | 0    | 1   | Invert UP/DOWN direction (0=normal, 1=reversed)    |
| `MAX_PAN_ANGLE:val`        | 180     | 0    | 270 | Pan range in degrees, centered on neutral (90°)    |
| `MAX_TILT_ANGLE:val`       | 50      | 0    | 270 | Tilt range in degrees, centered on neutral (90°)   |
| `CONFIG_SERVO_STEP_MS:val` | 50      | 10   | —   | Milliseconds between manual servo steps            |
| `CONFIG_AUTO_PAN_STEP_MS:val` | 100 | 10   | —   | Milliseconds between auto-pan steps                |

### Angle limit calculation
```
minLimit = constrain(neutral - maxAngle/2, 0, 270)
maxLimit = constrain(neutral + maxAngle/2, 0, 270)
```
Examples:
- `MAX_PAN_ANGLE:180` → pan range 0°–180°
- `MAX_TILT_ANGLE:50`  → tilt range 65°–115°
- `MAX_PAN_ANGLE:0`    → servo locked at neutral

### Speed reference
- 50ms  → 20°/sec
- 100ms → 10°/sec
- 10ms  → 100°/sec (minimum allowed)

## On Bluetooth Connect

The device sends the current configuration to the client:
```
CONFIG_PAN_REVERSED:<0|1>
CONFIG_TILT_REVERSED:<0|1>
MAX_PAN_ANGLE:<value>
MAX_TILT_ANGLE:<value>
CONFIG_SERVO_STEP_MS:<value>
CONFIG_AUTO_PAN_STEP_MS:<value>
```

## Error Handling

- Unknown commands → logged as `UNKNOWN_COMMAND`, ignored.
- Non-numeric parameter values → `.toInt()` returns 0.
- Out-of-range angles → clamped to 0–270°.
- Step interval below 10ms → forced to 10ms.

## Keeping Command.h in Sync

When adding a new command, update **all three** in `Command.h`:
1. `enum class Command` — add the new variant
2. `parseCommand()` — add the string-matching branch
3. `commandToString()` — add the switch case
