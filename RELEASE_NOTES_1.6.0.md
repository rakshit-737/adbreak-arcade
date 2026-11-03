# AdBreak Arcade v1.6.0 — product and visual redesign

AdBreak Arcade 1.6.0 gives the website and native launcher one recognizable
handheld-arcade identity while keeping the notification-first product intact.

## Highlights

- Rebuilt the website around warm paper, charcoal, muted orange, olive signals,
  editorial typography, and industrial product labels.
- Added a working website simulation of compact and expanded Android notification
  states with Tap Counter, Quick Math, Higher / Lower, reset, and state feedback.
- Reworked the 25-game catalogue into a searchable, filterable editorial shelf
  sourced from the existing content registry.
- Added factual privacy, Smart App Trigger, architecture, download, and release
  sections with clear testing-artifact boundaries.
- Added persisted website theme preference with system preference as the initial
  default, responsive mobile navigation, reduced-motion support, and updated
  metadata/Open Graph artwork.
- Refined Android launcher colors and surfaces, added a branded home masthead,
  and added system/light/dark appearance selection.
- Preserved Kotlin, Views/XML, local SharedPreferences state, all game rules,
  RemoteViews notification layouts, and session-safe action handling.

## Validation

- Website typecheck, Vitest suite, and production build pass locally.
- Android tests, lint, check, debug assembly, and minified release assembly run
  in the GitHub release workflow.
- The debug APK is signed with the standard Android debug key for testing.
- The release APK is unsigned because production signing is intentionally not
  configured in the public repository.
- Device/emulator OEM notification rendering, font scaling, and TalkBack review
  remain environment-dependent.

## Artifacts

- `adbreak-arcade-v1.6.0-debug.apk` — installable testing build.
- `adbreak-arcade-v1.6.0-release-unsigned.apk` — validation output, not directly
  installable until signed.
- `SHA256SUMS.txt` — checksums for both artifacts.
