# Strategy-game gameplay review and polish

Date: 2026-10-26. Scope: Prism Protocol, Starship Survival, Micro Tower Command, Pulse Reactor, their focused tests. Evidence is source inspection plus real JVM execution of the engines through `apply`, `render`, `encode`, and `restore`. No phone, emulator, notification-shade screenshot, accessibility-device, or OEM rendering evidence is claimed.

## Outcome and checks

- **39 focused tests pass**: the four existing named test classes plus 13 new tests in `StrategyPolishTest.kt`. Final independent compilation and JUnit run: `OK (39 tests)`, 0.457 seconds test execution, Kotlin 2.2.10/JVM target 17 on the installed JDK.
- All games publish objective progress and earned, comparable record candidates. Unavailable visible controls carry explanations; Pulse's four pulse buttons are always legal during play, so its disabled map is correctly empty.
- Prism retains the original three indexed puzzles/save geometry and adds two validated multi-flip puzzles. Starship has event-specific risk and honest repair/shield consequences. Tower reach, costs and imminent enemies are visible, and brute waves require layered/upgraded defence. Pulse re-shows mistakes and saves the future generator seed.
- The requested Gradle focused invocation was attempted once. It hit a Kotlin incremental-output `FileNotFoundException` for `GamePresentationCatalog.class`, then fallback compilation exceeded the 120-second tool timeout. Another Gradle compilation was running in this shared workspace. This is not a successful Gradle/integration check. Verification therefore compiled the owned engines/tests independently in the approved temporary directory. The lead owns full Gradle integration.
- The independent focused run was repeated after correcting cross-puzzle record comparability and fatal-sector progress; both runs passed 39 tests. No shared source/build/controller/store/UI files were edited here.

## Prism Protocol

### Baseline and decisions

The player selects a numbered mirror and flips it, watching a deterministic beam terminate at a target, wall, edge or loop. A flip costs one move; selection is free. Original puzzle minima were only **1, 2, 1 flips**, with limits 3, 4, 3. Source/target/blocker geometry was not all available in essential rendered state. That made the first puzzles simultaneously shallow for a solver and difficult to reconstruct from text. Both rotation directions have the same optical effect for a flat mirror.

### Improvements

1. Append **Deep Relay** (minimum four flips, limit six) and **Return Transmission** (minimum six, limit eight). Preserve all original puzzle indexes. Exhaustive enumeration proves the minima, instead of merely checking one authored solution.
2. Expose source position/direction, target position, numbered beam route, blocker coordinates, remaining flips, selection-disabled reasons, and explicit next/retry actions. Reject off-board mirror geometry. Progress measures the actual target connection, not moves spent. Record is **Protocols solved**, avoiding comparisons between a one-flip tutorial and a six-flip relay. Existing public `bestMoves` and saved fields remain compatible; its cross-level value is not offered as a personal-record metric.

### Failure, difficulty, replay, feedback and limits

- Failure: spending the last flip without linking the target. A correct last flip wins. Cycles terminate safely through repeated `(cell,direction)` detection.
- Difficulty: all five fixed levels tested. The original three are onboarding; the new pair requires planning four/six necessary changes. Each new level allows two spare flips.
- Replay: win advances/wraps the authored campaign; loss retries the same puzzle. Original v1 saves retain their geometry. Best-move validation now survives wrapping from a longer new puzzle to the first tutorial.
- Visual feedback: mirrors show orientation, coordinates and selection; essential text shows route/terminal status. This is a textual beam representation, not a rendered board or animation.
- Action count: two main controls plus 3–6 mirror cells; one mirror change requires two taps. Optimal five-level play uses 28 gameplay taps plus four NEXT taps. The new puzzles require eight and twelve gameplay taps.
- Notification limits: reconstructing a 7×7 route from text can be demanding; coordinates and blocker lists may wrap. Both flip-arrow controls remain optically redundant for compatibility. No device fit claim.

### Ten player questions

1. **Is the objective clear?** Yes: connect the shown source/direction to the shown target; progress becomes 1/1 only on connection.
2. **Can I predict consequences?** The mirror orientation, coordinates, route and move budget are available before acting. Both arrows toggle the same optical orientation.
3. **Are choices meaningful?** Mirror choice and order matter under a move cap; new levels demonstrably need four/six flips. Direction choice itself is redundant.
4. **Is failure fair?** No randomness or hidden clock; blockers and the final-flip warning are explicit. Off-board geometry is invalid.
5. **Does difficulty progress?** After introductory one-/two-flip boards, the relay puzzles increase the minimum work.
6. **Can I recover?** Wrong flips can be reversed while moves remain; retries restore the same puzzle without erasing completed-protocol count.
7. **Is completion clear?** WON/LOST, target progress, and NEXT/RETRY are distinct; terminal mirror controls are disabled.
8. **Why replay?** Advance through the campaign and improve route planning; fixed layouts have finite discovery value.
9. **Does collapse/refresh preserve play?** Every authored solution is restored after selection and continued equivalently; no clock advances. Actual shade collapse was not tested.
10. **Can I play notification-only?** All required geometry is in `GameViewState`, within action-count limits; compact-device readability remains unverified.

**Tests:** all five exact/minimum solutions; all 160 orientation configurations; bounded unique beam states; loop/block/edge/target behavior; malformed/off-board geometry; selection/clockwise/counterclockwise persistence; loss/retry; terminal scoring lockout; longer-puzzle-to-tutorial restore.

**Remaining issues:** fixed repeatable layouts; redundant flip directions; source/route text may need visual board treatment in a future UI task. Ratings (1–5): **depth 3, clarity 3, replay 3, notification suitability 3**.

## Starship Survival

### Baseline and decisions

Six deterministic hazards, with navigate/repair/scan/evade resolving one sector each. Five resources existed, but scanning every sector escaped unharmed, evading every sector also preserved hull, repairing advanced the route without facing the hazard, and shield depletion never overflowed into hull. These safe repeated-button routes undermined route planning. Costs were mostly reported after spending them.

### Improvements

1. Give scan/evade event-specific damage; repair restores hull/shields then faces the navigation hazard. Full-health/full-shield repair is disabled without spending a turn. Uncovered shield damage now hurts hull. Repeated navigate, scan or evade all lose, while alternating **Evade, Scan, Evade, Scan, Evade, Scan** escapes at full hull with score **45**.
2. Show action costs and exact hull deltas in each two-line control (for example `NAV -1HP / 2F1S`); fatal actions explicitly say `BREACH`. Keep the sector in the subtitle and scarce resources before score. Add last-jump, affordability reasons, crossed-sector progress and escape-only score records. Fatal attempts do not count as crossed sectors. Reject impossible score totals in snapshots. Lead integration moved risk forecasts from the crowded subtitle to the actual choices, corrected clamped/overflow damage feedback, and tests compare every legal forecast with its resolved hull delta.

### Failure, difficulty, replay, feedback and limits

- Failure: hull breach or no affordable action before escape. Repair can save a damaged ship but no longer grants free passage.
- Difficulty: one fixed six-sector setting, all six hazards explored. The perfect-hull route sacrifices score; exhaustive exploration found the highest escape score **57**, using **Navigate ×4, Evade, Repair**. The maximum terminal score including losses was 59; losing scores are not record candidates.
- Replay: same six hazards; optimizing damage versus points supplies a concrete replay goal. No procedural route generation is claimed. Valid v1 saves remain accepted, with subsequent actions using the improved rules.
- Feedback: sector index/name, exact hull risk and resource totals are persistent; action resolution names costs/outcomes. Escaped/lost states expose replay.
- Action count: four controls; six accepted decisions per successful run. Pause/replay are separate lifecycle actions.
- Notification limits: five resources plus four risk previews create dense text. F/E/S abbreviations correspond to Fuel/Energy/Supplies in the score line; device wrapping and label fit are not verified.

### Ten player questions

1. **Is the objective clear?** Survive six sectors; progress counts safely crossed sectors and the final sector says Last jump.
2. **Can I predict consequences?** Costs and current hull deltas are shown before a choice, including shield-overflow and repair effects.
3. **Are choices meaningful?** Full-hull escape scores 45; the highest-score escape scores 57 and takes greater risk. Repetition is not a universal winning tactic.
4. **Is failure fair?** Hazards are deterministic and current hull damage is visible. No off-screen timer or random hit is used.
5. **Does difficulty progress?** Hazard-specific counters and late resource pressure matter; there is one authored route, not selectable modes.
6. **Can I recover?** Repair heals before damage, but costs energy/supplies and still crosses the hazard. Full resources cannot be farmed through a no-op repair.
7. **Is completion clear?** Escape/Hull breach/Resources exhausted, terminal phase, progress and replay are explicit. Only a completed escape submits its score.
8. **Why replay?** Learn the safe route, then trade hull/resource margins for the proven higher-score route.
9. **Does collapse/refresh preserve play?** Each tested safe-route decision continues identically from a saved snapshot; exhaustive branches also restore before acting. No timer exists.
10. **Can I play notification-only?** Four choices and all risk/cost data are supplied to the shared view state; real-device text density still needs integration review.

**Tests:** exhaustive public-action tree with **1,782 winning paths and 790 losing paths** (terminal paths stop early); all live states have an enabled affordable action; action/disabled-map agreement; finite scoring; terminal no-ops; full-hull repair rejection; impossible-score rejection; safe route and three repeat-action losses; restore continuation, pause, replay and malformed-state tests.

**Intentional expectation change:** existing all-scan success tests now use the hazard-aware route. All-scan failure is explicitly checked, rather than silently weakening success assertions. Reckless-navigation testing stops when a real earlier terminal loss occurs.

**Remaining issues:** fixed hazard order eventually memorizes; essential state is dense; hull-risk preview is exact but shield/resource future planning still requires the player to track their totals. Ratings: **depth 3, clarity 4, replay 3, notification suitability 3**.

## Micro Tower Command

### Baseline and decisions

Build or upgrade on four even-numbered pads beside the path 1→3→5→7→9; toggle Dart/Frost, then end turns to spawn, fire and move enemies. Placement and upgrades spend gold but do not advance enemies. The baseline had effectively broad Dart reach (six half-steps at level one), six-HP brutes and hidden range/cost information. Tower placement therefore mattered less than the visible board implied. Path cells were presented like buttons even though they could never build.

### Improvements

1. Localize level-one Dart reach to its adjacent path cells, with upgrades extending coverage; Frost starts with wider coverage and its surviving target stops for a turn. Raise newly spawned brutes from six to nine HP to require overlapping fire, slowing, or upgrades. A lone unupgraded Dart on **any** build pad now loses; two distinct planned defences win without base damage.
2. Each build/upgrade pad shows gold cost and exact covered path span; maxed/unaffordable/path/terminal cells are visibly disabled with reasons. Essential text includes selected-type cost, damage/upgrade behavior, queue/active counts, next enemy HP and final-wave warning. Progress counts cleared waves and record counts base HP saved only after victory.

### Failure, difficulty, replay, feedback and limits

- Failure: six base HP exhausted by leaks (brutes deal two, others one). No-build strategy loses in **14 turns**. Targeting remains deterministic: furthest enemy, then oldest ID.
- Difficulty: all three escalating waves are tested; no separate user difficulty setting exists. Existing saved live brutes retain their saved health; subsequent spawns use nine HP. The v1 schema remains readable.
- Replay: same wave schedule, with choice of distributed Dart coverage or upgraded Dart/Frost support. Full-health victory gives a clear optimization goal.
- Visual feedback: cell labels distinguish path/enemy/tower, upgrade costs and spans. Brackets `[1–5]` mean path cells 1,3,5, not every numbered cell. The main subtitle explains brackets and the odd-cell route.
- Action count: two main controls plus nine cells (five disabled path cells). Three-Dart perfect win: three builds + **14 END TURN** = **17 gameplay actions**. Upgraded Dart + Frost perfect win: build, upgrade, toggle, build + **12 END TURN** = **16 actions**.
- Notification limits: nine cells and range/cost text need the expanded surface and may be paged by shared UI. This report verifies state/control counts, not OEM fit. There is no time pressure while choosing builds.

### Ten player questions

1. **Is the objective clear?** Protect the six-HP base through three waves; cleared-wave progress and final-wave status are explicit.
2. **Can I predict consequences?** Pad costs and actual firing spans are shown, along with next spawn HP; END TURN resolves spawn→fire→move.
3. **Are choices meaningful?** Localized Dart damage versus wider Frost stopping, upgraded versus distributed coverage, and gold timing produce different successful defences.
4. **Is failure fair?** Enemy order/movement/targeting are deterministic, and the next spawn/active board are visible. No notification dwell time advances enemies.
5. **Does difficulty progress?** Three waves add nine-HP brutes; the tests demonstrate that a cheap unattended lone Dart is insufficient.
6. **Can I recover?** Kill rewards fund repairs to coverage through additional towers/upgrades between turns; leaked base health cannot heal. A loss offers a fresh replay.
7. **Is completion clear?** Victory/Defeat and REPLAY are explicit; all board actions disable at terminal, and a full clear shows 3/3.
8. **Why replay?** Compare layered three-Dart and upgraded-Dart/Frost defences; optimize base saved and understand coverage.
9. **Does collapse/refresh preserve play?** The complete three-wave win is restored and continued equivalently on every END TURN, including live enemy positions and pending spawns.
10. **Can I play notification-only?** All build/turn controls, costs and range spans are represented in game state; nine-cell readability still needs device verification.

**Tests:** complete no-build loss; lone-Dart loss at all four pads; full-health three-Dart win; full-health upgraded-Dart/Frost win; exact near-versus-distant first-turn firing; upgrade range/cost/max bounds; earned-gold behavior; disabled cells; replay; every-turn save equivalence; original malformed and terminal fixture tests.

**Intentional expectation change:** the original initial-board assertion now includes costs and ranges. Starting resources, public actions, costs, turn order and existing public getters remain compatible.

**Remaining issues:** fixed wave order; no selling or base healing; RUNNER has the same movement speed as GRUNT (its existing distinction is low health); lengthy cell labels need device review. Lead integration fixed the inherited 10,000-turn terminal restore inconsistency and made rejected unaffordable/maxed upgrades fully mutation-free, with regression checks. Ratings: **depth 3, clarity 4, replay 3, notification suitability 3**.

## Pulse Reactor

### Baseline and decisions

Memorize a random 1–4 pattern; the first tap hides it. Repeat it to score and advance through ten levels, with lengths 3–12, streak multipliers and three mistakes allowed per level. A mistake clears input and streak. Baseline mistakes left the pattern hidden, forcing retries without an opportunity to relearn; restore also saved the current pattern but not the generator for future levels.

### Improvements

1. A non-terminal mistake re-shows the **same** pattern, names its mismatch position, shows retries, and resets streak. Correctly entered partial patterns remain hidden. First-tap hiding, last-pulse and final-completion semantics are explicit; progress measures completed patterns and record uses genuinely earned best streak.
2. Version **2** saves a per-run seed and deterministically generates subsequent level patterns. V1 migration retains its current sequence/input and derives a stable continuation seed; legacy empty hidden retries become visible. Unachievable score/streak values are rejected atomically, preventing multiplier overflow/inflated records.

### Failure, difficulty, replay, feedback and limits

- Failure: three mismatches within the current level; no timer. Retry budget resets after clearing a level. Winning requires all ten patterns.
- Difficulty: all ten lengths tested across **32 seeds**; lengths escalate from 3 to 12. The core challenge is recall, not strategic branching.
- Replay: NEW RUN generates a fresh seed/pattern set, resets run score/level and preserves earned lifetime best streak. No fixed seed is advertised to the player.
- Visual feedback: visible memorization pattern, first-tap instruction, hidden partial-input position, remaining mistakes, last-pulse cue and stable/overloaded terminal text. Input buttons remain four short digits.
- Action count: four controls. Perfect ten-level run takes **75 pulse taps**, scores **1,446**, and reaches best streak 10. Two relearning mistakes on every level take 95 taps, score **495**, and reach streak 1. Three mistakes at level one lose in three taps.
- Notification limits: twelve digits fit conceptually into a short text pattern, but accessibility/font-scale/OEM line breaking was not device-tested. Long uninterrupted recall may be demanding; there is no clock-based punishment for interruption.

### Ten player questions

1. **Is the objective clear?** Complete ten patterns; level/10 and completed-pattern progress are explicit.
2. **Can I predict consequences?** The first tap hides the pattern, a mismatch consumes one retry and breaks the combo, and the third mismatch overloads the reactor.
3. **Are choices meaningful?** The task rewards accurate recall; it is not a branching-strategy game. Mistakes have a score cost through streak reset.
4. **Is failure fair?** No timed input or reaction requirement; each recoverable mistake re-exposes the same pattern instead of forcing blind retries.
5. **Does difficulty progress?** Length increases every level from three to twelve; all generated symbols remain 1–4.
6. **Can I recover?** Two non-terminal mistakes allow relearning; the next successful level replenishes its retry budget. Completed score survives mistakes.
7. **Is completion clear?** Stabilized/Overloaded, WON/LOST, final 10/10 progress and NEW RUN are distinct. Terminal pulses cannot rescore.
8. **Why replay?** Fresh seeded patterns and a best-streak goal, with a perfect-run score distinct from mistake-heavy completion.
9. **Does collapse/refresh preserve play?** Partial input, current visibility, mistake recovery and every future pattern in the same run survive restore. V1 never had its original future RNG saved, so migration cannot recreate that unavailable history.
10. **Can I play notification-only?** Four numeric controls plus pattern/status text are sufficient in the model; actual notification visibility, paging and screen-reader behavior remain integration work.

**Tests:** full wins through every level for 32 seeded runs; generated symbol/length/variation constraints; partial-input restore through future generations; visible retry restoration; first-tap hiding; perfect score/streak; two mistakes per level without extra points; three-mistake loss; terminal no-rescore; replay/best preservation; V1 hidden-mistake migration; impossible score/streak rejection.

**Remaining issues:** no explicit preview/confirm phase (first tap still hides); recall lengths reach twelve; lifetime best streak caps at ten; migration can preserve current V1 state but not a generator that V1 never stored. Ratings: **depth 2, clarity 4, replay 4, notification suitability 4**.

## Reproducible verification and integration handoff

Attempted repository check (blocked before tests by shared incremental compilation/output failure):

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.adbreak.arcade.core.PrismProtocolGameTest" --tests "com.adbreak.arcade.core.StarshipSurvivalGameTest" --tests "com.adbreak.arcade.core.MicroTowerCommandGameTest" --tests "com.adbreak.arcade.core.PulseReactorGameTest" --tests "com.adbreak.arcade.core.StrategyPolishTest"
```

Successful fallback used the installed Gradle-cache Kotlin compiler and existing JUnit/JSON dependencies, compiling only `GameAction.kt`, the four owned engines, the four named tests and `StrategyPolishTest.kt` to `%LOCALAPPDATA%\Temp\opencode\strategy-polish-classes`. No build configuration changes were made. Equivalent command setup:

```powershell
$cache = Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1'
$compilerPaths = @('org.jetbrains.kotlin\kotlin-compiler-embeddable\2.2.10', 'org.jetbrains.kotlin\kotlin-stdlib\2.2.10', 'org.jetbrains.kotlin\kotlin-script-runtime\2.2.10', 'org.jetbrains.kotlin\kotlin-reflect\1.6.10', 'org.jetbrains.kotlinx\kotlinx-coroutines-core-jvm\1.8.0', 'org.jetbrains\annotations\13.0')
$testPaths = @('junit\junit\4.13.2', 'org.hamcrest\hamcrest-core', 'org.json\json\20250517', 'org.jetbrains.kotlin\kotlin-stdlib\2.2.10', 'org.jetbrains\annotations\13.0')
$compilerJars = @($compilerPaths | ForEach-Object { (Get-ChildItem (Join-Path $cache $_) -Filter '*.jar' -Recurse | Select-Object -First 1).FullName })
$testJars = @($testPaths | ForEach-Object { (Get-ChildItem (Join-Path $cache $_) -Filter '*.jar' -Recurse | Select-Object -First 1).FullName })
$out = Join-Path $env:LOCALAPPDATA 'Temp\opencode\strategy-polish-classes'
New-Item -ItemType Directory -Force $out | Out-Null
$main = 'app\src\main\java\com\adbreak\arcade\core'
$tests = 'app\src\test\java\com\adbreak\arcade\core'
$engines = @('PrismProtocolGame', 'StarshipSurvivalGame', 'MicroTowerCommandGame', 'PulseReactorGame')
$sources = @((Join-Path $main 'GameAction.kt')) + @($engines | ForEach-Object { Join-Path $main "${_}.kt" }) + @($engines | ForEach-Object { Join-Path $tests "${_}Test.kt" }) + @((Join-Path $tests 'StrategyPolishTest.kt'))
& java -cp ($compilerJars -join ';') org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 17 -classpath ($testJars -join ';') -d $out @sources
if ($LASTEXITCODE -ne 0) { throw 'Focused compilation failed' }
$classes = @($engines | ForEach-Object { "com.adbreak.arcade.core.${_}Test" }) + @('com.adbreak.arcade.core.StrategyPolishTest')
& java -cp ((@($out) + $testJars) -join ';') org.junit.runner.JUnitCore @classes
```

Lead integration: rerun the repository test suite/build once shared source work is settled; verify actual expanded/collapsed layouts and disabled-control affordances on Android. Core unit results do not establish those device/UI properties.
