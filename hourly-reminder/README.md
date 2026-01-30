# Hourly Reminder (Android)

This project builds a minimal Android app that lets you create hourly reminders that trigger notifications.

## Build a debug APK

### Option 1: Android Studio (recommended)
1. Open the `hourly-reminder/` folder in Android Studio.
2. Let Android Studio download the Android SDK + build tools.
3. Use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. The generated APK will be in `hourly-reminder/app/build/outputs/apk/debug/app-debug.apk`.

### Option 2: Command line
1. Ensure you have **JDK 17** and the **Android SDK** installed.
2. From `hourly-reminder/`, run:
   ```bash
   ./gradlew assembleDebug
   ```
3. The generated APK will be in `hourly-reminder/app/build/outputs/apk/debug/app-debug.apk`.

> Note: If `./gradlew` is missing, run `gradle wrapper` (from a JDK 17+ compatible environment) to generate the wrapper scripts.
