# IQ BOT PRO - Native Android APK Build Guide

This directory contains the complete native Android Jetpack Compose project for **IQ BOT PRO**.

---

## 📁 Android Project Structure

```
android/
├── build.gradle.kts                # Root project build configuration
├── settings.gradle.kts             # Subproject & repository mappings
├── gradle.properties               # JVM memory & AndroidX properties
└── app/
    ├── build.gradle.kts            # App dependencies (Compose, EncryptedStorage, OkHttp)
    ├── proguard-rules.pro          # Obfuscation & serialization rules
    └── src/
        └── main/
            ├── AndroidManifest.xml # Permissions (INTERNET, NETWORK_STATE) & Activity
            ├── res/
            │   ├── values/
            │   │   ├── strings.xml # English string resources
            │   │   ├── colors.xml  # Dark black (#080C0E) & Deep Teal (#00E5BC)
            │   │   └── themes.xml  # Material dark theme
            │   └── values-ar/
            │       └── strings.xml # Arabic localized strings (RTL)
            └── java/com/iqbotpro/app/
                ├── MainActivity.kt # Entry point Activity
                ├── ui/
                │   ├── MainApp.kt  # Root navigation & bottom bar
                │   ├── theme/
                │   │   ├── Color.kt# Color tokens
                │   │   ├── Type.kt # Typography
                │   │   └── Theme.kt# Dark Jetpack Compose Theme
                │   └── screens/
                │       ├── DashboardScreen.kt # Balance, Daily P&L, Status, Start/Stop
                │       ├── StrategyScreen.kt  # Keltner15 & 9-Level Martingale
                │       ├── HistoryScreen.kt   # Trade history with WIN, LOSS, DRAW
                │       └── SettingsScreen.kt  # Encrypted credentials & Bridge host
                └── data/
                    ├── model/
                    │   └── BotModels.kt       # Data classes & 9 Martingale steps
                    ├── security/
                    │   └── SecureStorageManager.kt # Android KeyStore encryption
                    └── network/
                        └── BotBridgeClient.kt # Decoupled Python bot WebSocket client
```

---

## 🛠️ How to Build into an APK (3 Simple Methods)

### Method 1: Using Android Studio (Recommended)
1. Download or clone this project to your computer.
2. Open **Android Studio** (Hedgehog, Iguana, or Koala).
3. Select **Open an Existing Project** and choose the `/android` folder.
4. Allow Gradle to sync dependencies automatically.
5. In the top menu, click **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
6. Android Studio will generate the APK at:
   `app/build/outputs/apk/debug/app-debug.apk`
7. Transfer to your Android phone and install!

---

### Method 2: Command Line (Linux, macOS, Windows)
If you have the Android SDK command-line tools installed:

```bash
cd android
# Build debug APK:
./gradlew assembleDebug

# The APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk

# Install directly to a USB-connected phone:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### Method 3: Cloud Build via GitHub Actions (Zero Local Tools Needed)
Push this repository to GitHub and add `.github/workflows/build-apk.yml`:

```yaml
name: Build IQ BOT PRO APK
on: [push, workflow_dispatch]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Grant execute permission for gradlew
        run: chmod +x android/gradlew || true
      - name: Build Debug APK
        run: |
          cd android
          gradle assembleDebug --stacktrace
      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: IQ-BOT-PRO-Debug-APK
          path: android/app/build/outputs/apk/debug/*.apk
```
This produces an instantly downloadable APK directly in your GitHub Actions Artifacts tab!

---

## 🔌 Connecting your Python Trading Bot

1. Run the Python bridge on your machine or VPS:
   ```bash
   pip install websockets
   python bridge/iq_bot_bridge.py
   ```
2. In the **IQ BOT PRO** Android app, go to **Settings** (الإعدادات).
3. Enter the local IP address of your machine running the Python bridge:
   `ws://192.168.1.XX:8765`
4. Tap **Connect Bridge**. The status on the Dashboard will turn **Connected** (Green).
5. Tap **START BOT** to trigger Keltner15 trading on 15M candles!
