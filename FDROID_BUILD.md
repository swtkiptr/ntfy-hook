# Build Instructions for ntfy Hook

ntfy Hook is a simple Android application that forwards notifications to ntfy servers.

## Prerequisites

- Android Studio Electric Eel (2022.1.1) or newer
- Java JDK 11 or newer
- Android SDK 33 or newer

## Building

1. Clone the repository:
   ```
   git clone https://github.com/swtkiptr/ntfy-hook.git
   cd ntfy-hook
   ```

2. Open the project in Android Studio

3. Build the project:
   ```
   ./gradlew assembleRelease
   ```

4. The APK will be available at:
   ```
   app/build/outputs/apk/release/app-release.apk
   ```

## Notes for F-Droid

- This app uses only free/libre dependencies
- No proprietary blobs or services are required
- The app is licensed under GPL-3.0
- The app doesn't track users
- No Anti-Features

## App Functionality

The app needs the following permissions:
- BIND_NOTIFICATION_LISTENER_SERVICE - to access notifications
- INTERNET - to send notifications to ntfy servers
- FOREGROUND_SERVICE - to run reliably in the background
- RECEIVE_BOOT_COMPLETED - to restart after device reboot

These permissions are used legitimately and are essential for the app's core functionality.
