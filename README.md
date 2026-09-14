# Sound Router (Split Audio Engine for Android)

Sound Router allows routing music and media audio streams to Bluetooth devices (headphones, car audio, speakers) while keeping phone calls, notifications, and other sounds on the phone's built-in speaker. It also includes per-app routing rules and Bluetooth device management.

---

## 🚀 GitHub Actions Automatic APK Build

This repository includes an automated GitHub Actions CI/CD workflow (`.github/workflows/build-apk.yml`) to compile and package the ready-to-install Android APK automatically upon every push.

### How to Get Your APK:

1. **Push to GitHub**:
   Push your code to your GitHub repository:
   ```bash
   git add .
   git commit -m "Add Sound Router and GitHub Actions APK workflow"
   git push origin main
   ```

2. **Wait for Workflow to Complete**:
   - Go to your repository on GitHub in your browser.
   - Click on the **Actions** tab at the top.
   - Click on the latest workflow run named **"Build Android APK"**.

3. **Download the APK**:
   - Scroll down to the **Artifacts** section at the bottom of the summary page.
   - Click on **`SoundRouter-v1.0-debug-apk`** to download the ZIP file.
   - Extract the ZIP to get `SoundRouter-v1.0-debug.apk`.

4. **Install on Phone**:
   - Transfer `SoundRouter-v1.0-debug.apk` to your Android phone (or download directly from GitHub on your phone browser).
   - Tap on the APK to install.
   - Allow "Install from unknown sources" if prompted by Android.

---

## 🛠️ Tech Stack & Requirements

- **Language**: Kotlin 2.2.10
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Database**: Room Database with KSP
- **Build System**: Gradle 9.3.1 / Android Gradle Plugin 9.1.1
- **Java**: OpenJDK 21 (Temurin)
- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 15 / 16 (API 36)
