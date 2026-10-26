# Contributing to AdBreak Arcade

Thanks for your interest in improving AdBreak Arcade. Contributions should
keep the app useful, offline-first, notification-safe, and respectful of the
Android platform.

Please read the [Code of Conduct](CODE_OF_CONDUCT.md) before participating.

## Before you start

- Search existing issues and pull requests before opening a duplicate.
- For security vulnerabilities, follow [SECURITY.md](SECURITY.md) instead of
  opening a public issue.
- Keep changes focused. Avoid unrelated formatting churn or broad rewrites.
- Do not commit credentials, signing keys, private payloads, local machine
  configuration, or generated build output.

## Project constraints

The Android app intentionally preserves its current architecture:

- Kotlin, Android Views/XML, and Gradle Kotlin DSL.
- The existing `ArcadeGame` abstraction and `GameRegistry`.
- `NotificationCompat`, `RemoteViews`, immutable `PendingIntent` actions, and
  local `SharedPreferences` persistence where appropriate.
- Minimum SDK/API 26 and target/compile settings should not change without a
  concrete technical reason and verification.

Do not add an account system, cloud backend, analytics tracker, advertising
SDK, network dependency, accessibility service, overlay, notification listener,
or unnecessary foreground service merely to add gameplay or progression.

The app must not claim to detect, skip, block, click, or manipulate ads, force
the notification shade open, or guarantee uninterrupted media playback.

## Development setup

Install JDK 17 or another version supported by the Gradle wrapper, plus the
Android SDK packages documented in the README. From the repository root on
Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat check
.\gradlew.bat assembleDebug
```

For a release-oriented validation pass:

```powershell
.\gradlew.bat --no-daemon check lintDebug lintRelease assembleDebug assembleRelease bundleRelease assembleDebugAndroidTest --console=plain --stacktrace
```

If an emulator or physical device is available, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Device-specific notification rendering, TalkBack behavior, OEM clipping,
permission flows, and background timing should be described honestly when
they have not been tested.

## Making changes

1. Create a focused branch from the current base branch, for example
   `feat/short-description`, `fix/short-description`, or `docs/short-description`.
2. Read the affected code, callers, resources, and tests before editing.
3. Keep game rules separate from notification rendering and Activity UI.
4. Add or update tests for new behavior, persistence, malformed state, and
   important edge cases.
5. Preserve saved user state unless a deliberate migration or reset is part
   of the change.
6. Update user-facing documentation when behavior, permissions, limitations,
   or release instructions change.
7. Run the narrowest relevant checks, then the broader checks when practical.
8. Review `git diff --check`, the final diff, and `git status` before opening a
   pull request.

## Commits and pull requests

Use concise conventional commit messages where practical:

```text
feat: add a new offline game
fix: reject stale notification actions
test: cover state migration
docs: clarify device testing limits
```

Pull requests should explain:

- What changed and why.
- Which files or user flows are affected.
- What checks were run and their actual result.
- Whether device testing was performed; do not claim it when no device was
  available.
- Any remaining limitations, migration concerns, or follow-up work.

Screenshots or recordings are useful for UI changes, but do not include
personal data, credentials, or private notification content.

## License

By intentionally submitting a contribution for inclusion in this repository,
you agree that it may be distributed under the [Apache License 2.0](LICENSE),
unless you and the maintainers have made a separate written agreement.
