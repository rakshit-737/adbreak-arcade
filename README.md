# AdBreak Arcade

AdBreak Arcade is a native Kotlin Android mini-game app for playing short,
offline games from the expanded notification shade while another app remains in
the foreground. It never detects, skips, clicks, blocks, or manipulates ads.

## What is implemented

- Seven playable games: Tap Counter, Quick Math, Tic-Tac-Toe, Higher or Lower,
  Memory Challenge, Word Scramble, and Number Sequence.
- Shared game interface, registry, action model, JSON state versioning, and
  local `SharedPreferences` persistence.
- Compact collapsed notification plus a separate expanded `RemoteViews` layout.
- Explicit immutable `PendingIntent` actions handled by a non-exported receiver.
- Start, pause, resume, reset, stop, game selection, notification permission
  handling, and notification/channel availability checks.
- Dark-mode-aware app and notification colors, Android 15 edge-to-edge insets,
  and app-owned launcher/status-bar icons.
- JVM tests for core game rules, state restoration, malformed state rejection,
  score/streak behavior, Tic-Tac-Toe lines/AI blocking, and notification-sized
  action mapping.
- A lightweight Android instrumentation smoke test for notification channel
  creation.

## Supported Android range

- Minimum SDK: Android 8.0 / API 26.
- Compile and target SDK: Android 16 / API 36.
- The project was built in this workspace with Gradle 9.4.1 and JDK 25.0.1.
  A current Android Studio installation with a supported JDK is recommended.

## Build prerequisites

Install or configure:

- JDK 17 or newer compatible with the Android Gradle Plugin. JDK 25.0.1 was
  used for the verified local build.
- Android SDK packages:
  - `platforms;android-36`
  - `build-tools;36.0.0`
  - `platform-tools` for `adb` installation/device checks

Android Studio can create `local.properties` automatically. From a terminal,
create it manually with a machine-specific SDK path, for example on Windows:

```properties
sdk.dir=D\:\\Android\\Sdk
```

Do not commit `local.properties` or any signing key.

## Build and test

From the repository root on Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat check
.\gradlew.bat assembleDebug
```

On Linux or macOS:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew check
./gradlew assembleDebug
```

The installable debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The debug build is signed automatically with the standard Android debug key.
You can verify it on Windows with:

```powershell
& "$env:ANDROID_SDK_ROOT\build-tools\36.0.0\apksigner.bat" verify --verbose `
  .\app\build\outputs\apk\debug\app-debug.apk
```

## Install on a phone

1. Enable Developer options and USB debugging on the Android phone.
2. Connect the phone and accept the debugging authorization prompt.
3. Confirm the device is visible:

```powershell
adb devices
```

4. Install or update the debug APK:

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Without USB debugging, transfer `app-debug.apk` to the phone using a cable,
cloud drive, or messaging app, open it with a file manager, and allow installs
from that source when Android asks. The debug APK is intended for testing, not
for Play Store distribution.

## How to use the notification game

1. Open AdBreak Arcade and choose a game from **Library**.
2. Allow notification permission on Android 13/API 33 and newer.
3. Tap **Start notification**.
4. Open another app, pull down the notification shade, and expand AdBreak
   Arcade.
5. Play with the visible notification buttons. Moves are handled by the
   receiver without intentionally launching the main Activity.
6. Use **Pause**, **Reset**, or **Stop** from the notification when needed.

If notifications are denied, the app remains usable for browsing the game
library and settings, but notification-native gameplay is unavailable until the
permission and channel are enabled.

## Device-testing checklist

The local build and JVM tests are verified. This workspace did not have a
connected emulator or physical Android device, so the following still require
manual validation:

- Android 8/API 26 notification rendering.
- Android 13/API 33 runtime notification permission denial and re-grant.
- Android 15/API 35 and Android 16/API 36 edge-to-edge behavior.
- Collapsed versus expanded notification clipping on OEM System UI.
- Dark mode, large font scale, TalkBack, and gesture/three-button navigation.
- Tic-Tac-Toe 3×3 notification controls and notification shade behavior while
  video playback continues.
- Process recreation while a game is active or paused.

Run instrumentation tests when a device or emulator is attached:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## GitHub: create the repository and push

These commands assume GitHub CLI is installed and authenticated. They create a
new public repository named `adbreak-arcade` under `rakshit-737`, commit the
project, and push the `main` branch. Review `git status` before committing.

```powershell
gh auth login
git init -b main
git add .
git diff --cached --check
git commit -m "feat: build AdBreak Arcade notification mini-games"
gh repo create rakshit-737/adbreak-arcade --public --source=. --remote=origin --push
```

If the GitHub repository already exists:

```powershell
git init -b main
git remote add origin https://github.com/rakshit-737/adbreak-arcade.git
git add .
git diff --cached --check
git commit -m "feat: build AdBreak Arcade notification mini-games"
git push -u origin main
```

## Privacy and permissions

The app has no network permission, account system, analytics, advertising SDK,
contacts, location, camera, microphone, accessibility service, overlay, or
notification-listener access. It stores only local game preferences and state.

## Known platform limitations

Android controls notification sizing, decoration, shade dismissal, and media
focus. `RemoteViews` is intentionally used instead of a WebView, Canvas, Unity,
or unrestricted custom view. As a result, every game uses compact text and
explicit button actions, and notification appearance can vary across Android
versions and manufacturers.

The release build is configured for minification but is not signed with a
production keystore. Configure protected release signing before distribution.
