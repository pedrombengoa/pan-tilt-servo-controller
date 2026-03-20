# Requirements Document - PanServoClient

## 1. Overview
PanServoClient is an Android application designed to remotely control a dual-axis (Pan and Tilt) servo system via Bluetooth. It provides real-time visual feedback of the servo's position and supports both manual and automated movement modes.

## 2. Bluetooth Connectivity
- **Device Targeting**: Specifically searches for and connects to a paired Bluetooth device named "PanTilt".
- **Auto-Connect**: The application attempts to initiate a Bluetooth connection automatically upon startup.
- **Manual Control**: Users can manually connect or disconnect via a toggle button in the main interface.
- **Protocol**: Uses RFCOMM (Serial Port Profile) with the standard HC-05 UUID (`00001101-0000-1000-8000-00805F9B34FB`).
- **Status Indication**: 
    - Textual status display (e.g., "Status: Connected to PanTilt").
    - Dynamic icon changes on the connection button to reflect state.

## 3. Servo Control Features
### 3.1 Directional Manual Control
- **Pan (Horizontal)**: Controlled by "Left" and "Right" buttons.
- **Tilt (Vertical)**: Controlled by "Up" (+) and "Down" (-) buttons.
- **Continuous Press**: Holding down any directional button sends the corresponding command (`LEFT`, `RIGHT`, `UP`, `DOWN`) repeatedly every 100ms.
- **Commands Sent**:
    - `LEFT` / `RIGHT` for pan.
    - `UP` / `DOWN` for tilt.

### 3.2 Movement Modes & Utilities
- **Autopan**: A "Play/Pause" button toggles automated panning. Sends the `AUTOPAN` command when enabled.
- **Reset**: A "Stop" button stops all movement and centers the servos. Sends the `RESET` command.

## 4. Real-time Feedback & UI
- **Angle Display**: Displays the current pan angle in degrees, parsed from Bluetooth incoming messages containing the pattern "Position: X".
- **Speedometer Gauge**: A custom-designed gauge that visually represents the current pan angle.
    - 0°: Far left.
    - 90°: Center/Up.
    - 180°: Far right.
- **Picture-in-Picture (PiP)**: Supports entering PiP mode when the user leaves the app, maintaining basic visibility of the status.

## 5. Diagnostics & Logging
- **Log Activity**: A dedicated "Settings" screen provides access to detailed communication logs.
- **Persistence**: Logs are cached in a global object, ensuring history is preserved when navigating between the Main and Settings activities.
- **Log Content**: Captures all outgoing commands, incoming messages from the server, and connection errors.
- **Manual Maintenance**: Includes a "Clear Logs" button to wipe the current session's log history.
- **Smart Scrolling**: The log view automatically scrolls to the bottom only if the user was already at the end of the log, allowing for uninterrupted reading of previous entries.

## 6. Technical Specifications
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Architecture**: Kotlin-based with Coroutines for asynchronous Bluetooth I/O and LiveData for reactive UI updates.
- **Permissions Required**: 
    - `BLUETOOTH`
    - `BLUETOOTH_ADMIN`
    - `BLUETOOTH_CONNECT` (Android 12+)
    - `BLUETOOTH_SCAN` (Android 12+)
