# AdBreak Arcade

AdBreak Arcade is a native Kotlin Android mini-game app for playing short,
offline games from the expanded notification shade while another app remains in
the foreground. It never detects, skips, clicks, blocks, or manipulates ads.
Optional Smart App Triggers only observe foreground transitions for apps the user
explicitly selects.

## Community and legal

- Source code license: [Apache License 2.0](LICENSE)
- Contribution guide: [CONTRIBUTING.md](CONTRIBUTING.md)
- Community standards: [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- Security reporting: [SECURITY.md](SECURITY.md)

## What is implemented

- Seventeen playable games: Tap Counter, Quick Math, Tic-Tac-Toe, Higher or
  Lower, Memory Challenge, Word Scramble, Number Sequence, Rock Paper Scissors,
  Odd One Out, Emoji Decode, Trivia Blitz, Mastermind Mini, Nim, Connect Three,
  Guess the Pattern, Anagram Challenge, and Lights Out Mini.
- Shared game interface, registry, action model, JSON state versioning, and
  local `SharedPreferences` persistence.
- Compact collapsed notification plus a separate expanded `RemoteViews` layout.
- Explicit immutable `PendingIntent` actions handled by a non-exported receiver.
- Start, pause, resume, reset, stop, game selection, notification permission
  handling, and notification/channel availability checks.
- Dark-mode-aware app and notification colors, Android 15 edge-to-edge insets,
  and app-owned launcher/status-bar icons.
- JVM tests for core game rules, state restoration, malformed state rejection,
  score/streak behavior, Tic-Tac-Toe lines/AI blocking, notification-sized
  action mapping, registry reachability, and Smart App Trigger polling guards.
- A lightweight Android instrumentation smoke test for notification channel
  creation.
- Optional Smart App Triggers settings for selecting launchable apps, granting
  Usage access, choosing a game, configuring cooldown, and manually testing the
  notification.
- Local arcade progression: first-play XP, level progress, recent games,
  favorites, per-game play/action/completion counts, and an onboarding guide.
- Session-safe notification actions include a render revision so an older
  rendered button cannot mutate a newer state in the same session.
- Compact notifications keep the collapsed view to title/status/score, while
  grid games hide verbose copy in the expanded layout to protect readability.

## Supported Android range

- Minimum SDK: Android 8.0 / API 26.
- Compile and target SDK: Android 16 / API 36.
- Android Gradle Plugin: 9.2.1. Gradle wrapper: 9.4.1. CI uses Temurin JDK
  17; JDK 25.0.1 was verified for the local build in this workspace. Use a JDK
  version supported by Gradle 9.4.1; “17 or newer” does not mean every future
  JDK version is supported.

## Build prerequisites

Install or configure:

- JDK 17 for the reproducible CI toolchain, or another JDK version supported by
  Gradle 9.4.1. JDK 25.0.1 was verified for the local build.
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

## Smart App Triggers

Smart App Triggers are disabled by default. Open **Settings → Configure Smart
App Triggers**, read the explanation, grant Android **Usage access**, select
launchable apps, choose the game and cooldown, then save the selection.

The feature observes only foreground transitions for packages explicitly chosen
by the user. It does not inspect app content or detect, skip, click, block, or
manipulate advertisements. Selected package names and local duplicate/cooldown
history stay in app preferences.

Android does not provide a guaranteed immediate callback for every arbitrary
foreground-app transition to an ordinary app. This implementation performs a
best-effort lifecycle poll when AdBreak Arcade resumes or when the user uses the
manual test action; it does not force the notification shade open or expanded.
OEM background restrictions can further delay or prevent a poll.

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

The app requests `POST_NOTIFICATIONS` for the game notification and
`PACKAGE_USAGE_STATS` only for the opt-in Smart App Triggers feature. It has no
network permission, account system, analytics, advertising SDK, contacts,
location, camera, microphone, accessibility service, overlay, or
notification-listener access.

Without Smart App Triggers, it stores local game preferences and state. When
Smart App Triggers are enabled, it additionally stores selected package names,
scan/trigger timestamps, and duplicate-event history locally. No usage history
is uploaded and no app content is inspected.

## Known platform limitations

Android controls notification sizing, decoration, shade dismissal, and media
focus. `RemoteViews` is intentionally used instead of a WebView, Canvas, Unity,
or unrestricted custom view. As a result, every game uses compact text and
explicit button actions, and notification appearance can vary across Android
versions and manufacturers.

The release build is configured for minification but is not signed with a
production keystore. Configure protected release signing before distribution.

## Release status

CI validates the debug build and uploads a debug APK. The release build uses
minification and resource shrinking, but production release signing is not
configured in this repository. An `app-release-unsigned.apk` is not a
distributable release; configure a protected keystore outside the repository
before publishing.

## Project website

The static project website lives in `website/` and is deployed to GitHub Pages
from `main` by `.github/workflows/deploy-pages.yml`.

From the website directory:

```powershell
npm ci
npm run typecheck
npm test
npm run build
npm run dev
```

The Vite project uses `/adbreak-arcade/` as its production base path. The
expected published site is:

```text
https://rakshit-737.github.io/adbreak-arcade/
```

The website labels the current `v1.2.0` debug APK as a testing artifact. It
does not describe the unsigned release build as production-ready, and it does
not claim that Smart App Triggers detect advertisements or force open the
notification shade.
