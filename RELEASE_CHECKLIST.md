# AdBreak Arcade 1.5.0 release checklist

## 1.5.0 release candidate (2026-11-02)

- [x] Android library search by name/mechanic and horizontally scrollable family filters.
- [x] Favorites and recent sections remain grouped when the shelf is unfiltered.
- [x] Version code `6`, version name `1.5.0`, and compile/target SDK 36 are aligned.
- [x] Website release links, inventory copy, and testing instructions point to v1.5.0.
- [x] Tag-triggered GitHub workflow builds checks, debug APK, unsigned release APK,
  SHA-256 checksums, and publishes a prerelease with explicit testing notes.

## Second-pass local candidate (2026-10-26)

- [x] All 25 games reviewed/simulated; per-game changes and rankings recorded in
  `reports/GAMEPLAY_POLISH.md` and its linked scope reports.
- [x] 171 JVM tests, debug lint/check, debug APK and instrumentation APK builds.
- [x] Debug APK metadata, size, SHA-256 and debug signature verified; exact values
  are in `reports/GAMEPLAY_POLISH.md`.
- [x] Source color contrast measured; limitations in `reports/notification-polish-qa.md`.
- [ ] Device execution: `connectedDebugAndroidTest` reported no connected devices.
- [ ] Actual screenshots, OEM layouts, font scaling and TalkBack review.

## Automated checks

- [x] Twenty-five games are registered and have versioned state restoration,
  including Cyber Heist, Pocket Dungeon, Neon Tactics, Cipher Vault, Prism
  Protocol, Starship Survival, Micro Tower Command, and Pulse Reactor.
- [x] Notification channel, stable notification ID, immutable action intents,
  session IDs, and render revisions are implemented.
- [x] Local game state, scores, recent games, and onboarding use
  `SharedPreferences` with JSON validation.
- [x] Main lobby, library, scores, settings, onboarding, and notification
  permission paths are implemented.
- [x] Supported range is API 26 minimum with compile and target API 36.
- [x] JVM tests cover legal, invalid, terminal, replay, scoring, generation,
  and restoration behavior for the target games.

## Device checks still required

- [ ] Fresh install and first-launch notification permission on API 33+.
- [ ] Notification creation, every visible game action, stale/repeated action,
  pause/reset/stop, process recreation, and game switching.
- [ ] Compact and expanded rendering on API 26, API 33, API 35/36, dark mode,
  large font scale, and TalkBack.
- [ ] Install and launch smoke test with `adb` on a connected device/emulator.

## Artifact and publishing

- [ ] Record the final debug APK path, byte size, SHA-256, and `apksigner`
  verification output.
- [ ] If publishing a release APK, configure protected production signing outside
  the repository and verify the signed artifact separately.
- [ ] Review `git diff --check`, secrets, generated outputs, and the complete diff.
- [x] Create and publish the v1.5.0 testing release only after local validation.
