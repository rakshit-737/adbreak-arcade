# Notification presentation and reliability review

## Evidence boundary

Review of the actual Kotlin/RemoteViews/XML implementation. `adb devices -l`
returned an empty device list. No emulator executable was found in the workspace
SDK. There are **no device screenshots or visually verified OEM layouts** in this
report. Static contrast calculations, JVM rules/layout-policy tests and Android
instrumentation compilation are distinct from on-device QA.

## Baseline defects and implemented fixes

| Defect | Implementation |
|---|---|
| First randomized puzzle was not persisted; the first action could load a different puzzle | `ArcadeStore.loadGame` saves the initial snapshot before returning it; unreadable snapshots are retained locally and recovery is explained |
| Per-controller locks did not protect receivers and Activity instances from each other | One shared session lock covers action validation, mutation, persistence and rendering |
| State/token and render revision were written in separate operations | Accepted state, stats, record, token and revision commit together; start/stop also commit one session snapshot |
| Refresh invalidated buttons and caused redundant notification updates | Refresh preserves revision; an unchanged active notification is not reposted |
| Compact layout had four text rows and mostly generic instructions | Two rows: game/outcome and current objective, result or restoration/record notice; full semantics remain accessible |
| Dense boards stacked a top button row over a 3×3 grid | Board auxiliary moves use native notification actions; Menu/details hosts restart, pause, stop and expanded state on every game |
| Nine-cell boards grow too tall with large text | At font scale ≥1.3, show one three-cell row; Menu selects other rows with original action identities, no game turn or XP |
| Four long answers shared one narrow row | Two-column answer controls, 48dp minimum target height, two-line labels |
| Essential resources were a large one-line score heading | Smaller resource text, up to two lines for non-board/detail surfaces; game models supply genre-specific essentials |
| Winning and losing shared a generic “Game over” state | Complete/Try again/Draw labels, actual model feedback and semantic success/error colors |
| Invalid cells looked actionable | Model-provided unavailable actions are disabled with explanatory accessibility labels; stale/unsupported broadcasts remain no-ops |
| Best records could disappear when the model was reset | Separately persisted, typed per-game records; ties and incompatible units never award a new record; explicit history removal clears them |
| Dark motif text had poor contrast on violet | Dedicated `arcade_on_violet` day/night foreground |

## Honest visual feedback

Only supported `RemoteViews` operations are used: text, visibility, enabled state,
content descriptions, text colors/sizes, immutable click intents and a determinate
native `ProgressBar`. Progress means an actual discrete objective, not a timer.
The objective/count remains in game state and the bar's accessibility description;
board Menu/details includes the objective label and count.

There is no redraw loop, notification-driven animation or timing-based gameplay.
Native lobby cards show the selected run's actual objective/progress and personal
record. Scores distinguish the current run summary from the persistent record.
This pass deliberately uses state changes rather than decorative animation.

## State-feedback policy across all games

- **Initial/replay:** model explains the current problem and usable controls.
- **Accepted/mistake/near completion:** model updates the consequence and objective.
- **Invalid:** unavailable visible controls carry reasons; illegal inputs leave the
  model unchanged. Missing/stale/duplicated envelopes never advance a game or award
  progression. They do not trigger a misleading “move accepted” notification.
- **Win/loss:** explicit phase, actual result, replay route; native Replay dispatches
  `NEW_GAME` so campaign progression is preserved instead of forcing `RESET`.
- **Record:** atomic comparison and saved “Personal best” notice; later renders do
  not award it again.
- **Restoration:** “Saved session restored”; failed restoration starts a fresh
  round with an explicit unreadable-state notice and preserves the rejected bytes
  privately in preferences for recovery.
- **Collapsed/refreshed:** collapsed state is a summary, not a playable board.
  Refresh leaves the exact saved puzzle, action revision and progress intact.

## Static contrast measurements

WCAG sRGB relative luminance, computed from actual day/night XML colors. Ratios
are against `notification_surface`, except the motif foreground against violet.
These are source measurements, not screenshots.

| Role | Day | Night |
|---|---:|---:|
| Primary text | 16.31:1 | 14.46:1 |
| Secondary / disabled text | 6.35:1 | 9.30:1 |
| Action accent | 6.26:1 | 9.06:1 |
| Success | 6.36:1 | 10.23:1 |
| Failure | 6.45:1 | 9.55:1 |
| Family accents (minimum) | 5.89:1 | 9.44:1 |
| Motif on violet | 4.92:1 | 7.70:1 |

## Device QA still required

API 26, 33 and 35/36: compact/expanded notification, long translated text, font
scales 1.0/1.3/2.0, light/dark, TalkBack focus and disabled explanations. Inspect
board Menu/details and row selection, solved/failed states, replay, process kill
and restoration. Confirm native action labels and custom-height clipping in
actual System UI. Extreme font scaling and OEM decoration may still truncate
content; native row paging is implemented but not visually certified here.

See `GAMEPLAY_POLISH.md` for final commands, counts, artifact and per-game ranking.
