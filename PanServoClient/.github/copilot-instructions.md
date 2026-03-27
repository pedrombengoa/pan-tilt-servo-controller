# Copilot Instructions — PanServoClient

## Project Overview

PanServoClient is a **native Android app** (Kotlin) that controls a dual-axis Pan/Tilt servo system via **Bluetooth Classic** (RFCOMM/SPP). It connects to an **ESP32**-based firmware device named `"PanTilt"` and provides real-time visual feedback of servo position.

---

## Tech Stack & Conventions

| Concern | Technology |
|---------|-----------|
| Language | **Kotlin** (Java 11 source/target) |
| Build system | **Gradle Kotlin DSL** (`build.gradle.kts`), version catalog (`gradle/libs.versions.toml`) |
| Min SDK | **24** (Android 7.0) |
| Target SDK | **36** |
| UI toolkit | **Android Views + XML layouts** (NOT Jetpack Compose) |
| Architecture | Activities (`MainActivity`, `SettingsActivity`) + singleton `BluetoothConnection` object |
| Async | **Kotlin Coroutines** (`Dispatchers.IO`, `SupervisorJob`) |
| Reactive UI | **LiveData** (`MutableLiveData` observed from Activities) |
| Material | **Material Components** (`com.google.android.material`) |
| i18n | Two locales only: **`es` (español, default)** and **`en` (English)**. Strings in `values/strings.xml` (English) and `values-es/strings.xml` (Español). Default app locale is `es`. |
| Package | `com.pben.panservoclient` |

### Key Patterns
- **No Compose** — all UI is XML-based (`activity_main.xml`, `activity_settings.xml`). Never introduce Compose.
- **No Fragments** — the app uses full Activities, not Fragments or Navigation Component.
- **Singleton BT connection** — `BluetoothConnection` is a Kotlin `object` (singleton) managing the socket, I/O streams, and log cache. Do NOT refactor this into a ViewModel or Service unless explicitly asked.
- **Immersive mode** — both activities use `enableImmersiveMode()` to hide system bars. Always preserve this.
- **Picture-in-Picture** — `MainActivity` supports PiP. Keep `supportsPictureInPicture` and related config.

---

## Bluetooth Protocol

The app communicates with the ESP32 over RFCOMM using newline-terminated text commands.

### Outgoing commands (app → ESP32)
| Command | Description |
|---------|-------------|
| `LEFT` / `RIGHT` | Pan servo |
| `UP` / `DOWN` | Tilt servo |
| `AUTOPAN` | Toggle automated panning |
| `RESET_POSITION` | Stop all movement, center servos, disable auto-pan |
| `RESET_CONFIG` | Restore all config settings to factory defaults |
| `CONFIG_PAN_REVERSED:<0\|1>` | Reverse pan direction (default 1) |
| `CONFIG_TILT_REVERSED:<0\|1>` | Reverse tilt direction (default 0) |
| `MAX_PAN_ANGLE:<value>` | Set max pan angle (0–270, step 10, default 180) |
| `MAX_TILT_ANGLE:<value>` | Set max tilt angle (0–270, step 10, default 50) |
| `CONFIG_SERVO_STEP_MS:<value>` | Servo step delay in ms (min 10, step 10, default 50) |
| `CONFIG_AUTO_PAN_STEP_MS:<value>` | Auto-pan step delay in ms (min 10, step 10, default 100) |

### Incoming messages (ESP32 → app)
- **Position feedback**: `Channel: Joystick | Command: LEFT | Pan: 89 | Tilt: 90` — parsed with regex `Pan: (\d+) \| Tilt: (\d+)`.
- **Config echoes**: Same `KEY:VALUE` format as outgoing config commands. Sent on connect and after changes.
- **RESET echo**: When `RESET` is received, reset all config UI to defaults.

### Adding a new config parameter
1. Add an entry to `ServoCommand` enum in `ServoCommand.kt`.
2. Add the UI row in `activity_settings.xml` (follow the stepper pattern: `−` button, value `TextView`, `+` button).
3. Declare the `TextView` and wire up `setupStepperButtons(...)` in `SettingsActivity.kt`.
4. Add the `KEY.value ->` branch in `applyConfigFromMessage()`.
5. Handle RESET default in the same function.
6. Add the string resource in **both** `values/strings.xml` and `values-es/strings.xml`.

---

## Project Structure

```
PanServoClient/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/pben/panservoclient/
│   │   ├── BluetoothConnection.kt   ← Singleton: socket, I/O, LiveData, log cache
│   │   ├── MainActivity.kt          ← Main control screen (joystick-style buttons, gauges)
│   │   ├── ServoCommand.kt          ← Enum of all BT commands
│   │   └── SettingsActivity.kt      ← Config screen: logs, speed/angle controls, language
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml
│       │   └── activity_settings.xml
│       ├── values/strings.xml        ← English strings
│       ├── values-es/strings.xml     ← Spanish strings (default locale)
│       ├── drawable/                  ← Neumorphic drawables, icons
│       └── xml/                       ← Backup rules, locale config
├── build.gradle.kts
├── gradle/libs.versions.toml         ← Version catalog
├── requirements.md                    ← Functional requirements
└── settings.gradle.kts
```

---

## Server API Contract

The ESP32 firmware that this app connects to has its Bluetooth API formally defined in:

```
C:\DevPedro\repo\pan-tilt-servo-controller\pioServoServer\.github\instructions\ServoAPI.instructions.md
```

**Whenever the user asks to update, modify, or extend the API contract** (add/remove/change a Bluetooth command, adjust parameter ranges, update RESET defaults, etc.):

1. **Read the server API file first** — always read `pioServoServer/.github/instructions/ServoAPI.instructions.md` before making any changes so you understand the current contract.
2. **Keep both sides in sync** — any protocol change must be reflected in both the server API definition and this client app (enum, UI, parsing, string resources).
3. **Validate ranges & defaults** — use the server API file as the source of truth for min/max values, defaults, and RESET behavior.

---

## Important Rules for AI Assistants

1. **Language**: The user prefers communication in **Spanish**. Respond in Spanish unless asked otherwise.
2. **No Compose**: Never introduce Jetpack Compose. All UI must remain XML-based.
3. **No architecture migration**: Don't refactor to MVVM/Clean Architecture/Hilt/etc. unless explicitly asked.
4. **Two locales only**: `es` and `en`. Always update both `strings.xml` files when adding/changing strings.
5. **Stepper UI pattern**: Config values use `ImageButton (−)` + `TextView (value)` + `ImageButton (+)`. Follow existing patterns in `activity_settings.xml`.
6. **ServoCommand enum**: Every BT command must have an entry in `ServoCommand.kt`.
7. **LiveData for BT state**: `BluetoothConnection` posts to `isConnected`, `messages`, and `errors`. Activities observe these. Don't change this pattern.
8. **Continuous press**: Directional buttons use `handleContinuousPress()` with 100ms repeat. Preserve this behavior.
9. **`sendCommand` format**: Config commands are sent as `"COMMAND_NAME:value\n"`. Simple commands as `"COMMAND\n"`.
10. **Immersive + PiP**: Always maintain immersive mode and PiP support in `MainActivity`.
11. **Test both locales** when touching strings — the app defaults to `es`.
12. **Gradle version catalog**: Add new dependencies to `gradle/libs.versions.toml`, not inline in `build.gradle.kts`.

