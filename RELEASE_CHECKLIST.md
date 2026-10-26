# AdBreak Arcade 1.3.0 release checklist

## Automated checks

- [x] Seven target games are registered and have versioned state restoration.
- [x] Notification channel, stable notification ID, immutable action intents,
  session IDs, and render revisions are implemented.
- [x] Local game state, scores, recent games, and onboarding use
  `SharedPreferences` with JSON validation.
- [x] Main lobby, library, scores, settings, onboarding, and notification
  permission paths are implemented.
- [x] Supported range is API 26 minimum with compile/target API 36.
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
- [ ] Create a release commit/tag only with explicit authorization.
