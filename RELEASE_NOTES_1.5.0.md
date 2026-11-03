# AdBreak Arcade v1.5.0 — shelf polish and testing release

AdBreak Arcade 1.5.0 makes the 25-game library easier to navigate and makes the
public release surface match the app.

## Highlights

- Search the Android library by game name, mechanic, or family.
- Filter by Pulse, Logic, Words, Board, Code, and Focus without losing favorites
  or recent-game grouping when the filter is cleared.
- Updated the project website, installation copy, and release links to v1.5.0.
- Added a tag-driven GitHub Actions release workflow that runs tests, lint, and
  release assembly, then publishes checksums with the validation artifacts.
- Retains the 25 notification-first offline games and the previous gameplay,
  persistence, accessibility, and stale-action hardening work.

## Validation

- JVM unit tests, lint, check, debug assembly, and minified release assembly run
  in the release workflow.
- The debug APK is signed with the standard Android debug key for device testing.
- The release APK is unsigned because production signing is intentionally not
  configured in the public repository.
- Device/emulator notification rendering and OEM-specific layout behavior remain
  environment-dependent and are not represented as passing checks here.

## Artifacts

- `adbreak-arcade-v1.5.0-debug.apk` — installable testing build.
- `adbreak-arcade-v1.5.0-release-unsigned.apk` — minified validation output,
  not directly installable until signed.
- `SHA256SUMS.txt` — checksums for both artifacts.
