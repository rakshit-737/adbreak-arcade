# AdBreak Arcade — second-pass gameplay and polish report

## Outcome

All **25 registered games were inspected and simulated**, with focused improvements
to every model. The registry remains at 25. This is a stronger, more honest arcade,
but an uneven portfolio: deduction, tactical/resource choices and spatial puzzles
have real decisions; several small quiz decks and chance/counter games still have
a low replay ceiling. Compiling is not treated as evidence of good gameplay.

**Final integration:** 171 JVM tests, zero failures/errors/skips; debug lint/check,
debug APK and instrumentation APK builds pass. Lint has **93 warnings**, not zero.
On-device tests could not execute: **No connected devices!** No screenshot, OEM
layout, TalkBack, installation or media-continuity verification is claimed.

At review completion, changes were local and uncommitted. The polish pass did not
publish a release or change the game inventory, stack, application ID or version.

## Baseline and review evidence

Starting branch: `chore/github-project-hygiene`; clean worktree at `5826389`.
The baseline `testDebugUnitTest assembleDebug` invocation passed using cached
outputs. The integrated suite was subsequently rebuilt from `clean` and executed.

Detailed reports record **each game's original loop, meaningful decisions,
failure, difficulty, replay incentive, visual feedback, action counts, notification
constraints, tests and bugs**, then answer all ten player-experience questions:

- [Original seven](gameplay-originals.md)
- [Ten shelf games](gameplay-shelf.md)
- [Four priority campaigns](gameplay-campaigns.md)
- [Four puzzle/strategy games](gameplay-strategy.md)
- [Shared notification presentation and visual QA](notification-polish-qa.md)

Four bounded model/test owners completed those independent scopes. The lead
reviewed their diffs, fixed integration defects, implemented shared rendering and
persistence, and ran the final repository checks. Early parallel test attempts
encountered partial source/incremental-output conflicts; those are documented in
the scope reports and superseded by the final clean integration run.

## Per-game changes

Action counts below mean gameplay inputs, excluding optional menu navigation,
pause/replay and reading history. Variable/legacy behavior is detailed in the scope
reports. All games also receive shared compact-objective, progress, record,
restoration and unavailable-control handling where applicable.

| Game | Weakness → implemented mechanics and visual feedback | Typical completion inputs | New tests/evidence | Remaining issue |
|---|---|---:|---|---|
| Tap Counter | Fixed 50-point ceiling advertised as beatable → honest 20-tap sprint, combo consequences, target bar and completion-only record | 20 | Exact clear/ceiling, terminal freeze, 120-tap legacy migration | Counter, not strategic play; little replay variation |
| Quick Math | Endless stream → ten-question sets, seven-right clear, correctness record, equation explanation and terminal result | 10 | 400 generated questions; arithmetic/unique options; pass boundary and restore | Nearby-number distractors; scores compare practice sets rather than equal exams |
| Tic-Tac-Toe | Perfect AI made wins unattainable; draws unrewarded → +1 for defensive draw, unbeaten streak, AI-cell feedback, retained final board, disabled occupied cells | 3–5 | Legal draw search, repeated draw rewards, abandonment breaks streak, terminal board guards | Fixed expert opponent; no novice mode |
| Higher or Lower | Five tries could not cover 50 numbers; ordinary narrowing broke streak → six tries above 31, round-based streak, duplicate guesses disabled, explicit eliminated range | 1–6 | All 245 secrets over seven range sizes solved by MID; migration/duplicates | Repeated midpoint remains a dominant strategy |
| Memory Challenge | 5,150-input endpoint; hidden failed pattern → five patterns of length 2–6, re-study after error, varied replay and fewest-mistakes record | 20 perfect | Full clear, three-error loss, no retry points, legacy sequence, deterministic replay | Recall rather than strategy; old long saves remain awkward |
| Word Scramble | Endless and predictable distractors → ten-word sets, plausible shared-letter options, clear pass/result, earned correctness record | 10 | 400 scrambles; unique correct anagram, anti-repeat, boundary/migration | Thirty English words; limited advanced vocabulary |
| Number Sequence | Correct answer always PRIMARY → saved shuffled options, ten-puzzle sets, rule explanations, result and progress | 10 | 400 prompts across five families/all answer slots; fixed-button exploit regression | Other mathematical interpretations of finite sequences remain possible |
| Rock Paper Scissors | Claimed strategy against randomness → honest five-throw match with precommitted saved opponent plan and relationship feedback | 5 | All nine outcomes, future-plan restoration, terminal/replay | Chance game; no action has an advantage |
| Odd One Out | Five endlessly repeated cards without rationale → bounded deck, rotated choices, explicit category contrast and result | 5 | Every card at every replay offset, all-wrong run, invalid restore | Tiny bank; elementary categories |
| Emoji Decode | Ambiguous/reversed clues → corrected left-to-right compounds, text decomposition, four-clue decks and rotation | 4 | All clue/offset solutions, errors and terminal restore | Four English compounds; emoji rendering varies |
| Trivia Blitz | Endless deck, ambiguous plant question, fragile migration → eight-question results, factual explanations, photosynthesis wording and stable legacy cursor | 8 | All eight offsets; old-save second restoration; answer variety | Eight facts; long explanations need device review |
| Mastermind Mini | Replayed the same code; lost clues; duplicate attempts → rotating codes, retained clue notebook, tried-guess disabling and fewest-guesses record | 1–4 | 4,096 clue comparisons; four replay sets; duplicate/history/legacy loss | Four candidates can be brute-forced; short history may truncate |
| Nim | Same opening and unclear win rule → 13/14/15 openings, multiples-of-four teaching, explicit replies and disabled oversized takes | 3–4 | Every take at piles 1–15; optimal wins for all openings | A learned invariant solves the game |
| Connect Three | Same AI fallback, opaque replies → three opening preferences, drop/block/threat feedback, disabled full columns and board-derived results | ≤5 | All 19,683 board winner states; reachable branches/three preferences | Board is still compressed text; small fixed arena |
| Guess the Pattern | Fibonacci card wrongly accepted 4 → accepts 5, states intended rule, four-card results and rotated options | 4 | Every card/offset, explicit Fibonacci regression, terminal/migration | Four elementary rules; limited replay depth |
| Anagram Challenge | Arbitrary distractors and erased prefix → clued words, remaining-letter choices, prefix retained after mistakes, three-error budget and word results | 5–7 win | Every word, error recovery, hidden-action rejection, loss/replay | Five guided English words; not free text entry |
| Lights Out Mini | One unexplained board → orthogonal/reversible explanation, exact distance hint, six distinct par-three puzzles, separate Retry/New Puzzle | ≥3 | Independent solver over 512 states; all 511 live boards × nine presses; all starts | No move cap by design; dense grid requires expanded view |
| Cyber Heist | Hidden costs/random early difficulty → readable prices/alarm warnings, ordered mission tiers, competing clean/cache extraction bonuses | 9 | Clean and cache clears across 128 seeds each; migration/resources | Fixed stage vocabulary; seeded loot affects cross-run scores |
| Pocket Dungeon | Infinite guard points and dominant room choices → finite damage scoring, heavy-strike parry/counter, risky escape relic, ward/heal choice, costly scouting | 12 tested attack route | Full relic/rest/ward runs; bounded guards; telegraph/resource/restore tests | Fixed map; later scouting is a weak option; primary-only route forgiving |
| Neon Tactics | Unlimited move/defend scoring and dead drone blocking → finite accomplishment scoring, one-time data detour, health/speed extraction bonus, clear threats/legal cells | 2 fast; 11 tested detour | 2,139 reachable identities; 1,500 post-drone actions earn no points; migration | Fixed encounter and easy direct exit; optimal score not proven |
| Cipher Vault | Huge code spaces with too few tries; clues disappeared → capped 3–4 digit tiers, proportional budgets, saved/browsable clues, backspace, known-position separation and completion streak | 4 minimum; ≤41 in tested hard policy | 601,117 clue comparisons; 64 beginner and 512 hardest suffix solutions; hints/history/restore | Long hard sessions; legacy eight-digit vaults retain difficulty; middle tiers not exhaustively optimized |
| Prism Protocol | Mostly one-flip puzzles and hidden geometry → two harder four-/six-flip relays, source/target/beam/blocker text, disabled unselected flips, next/retry and target progress | 2–12 per level; 28 + 4 next taps across five | All 160 orientations; exact minima; every solution and move-limit loss; restore | Fixed levels, redundant flip directions, demanding coordinate reading |
| Starship Survival | Always-scan/evade wins and free-passage repair → hazard-specific tradeoffs, shield overflow, repair faces danger, per-choice exact HP forecast/cost and BREACH warning | 6 | All 2,572 terminal routes; 1,782 wins; forecast equals actual HP delta; best tested escape 57 | Fixed route eventually memorized; five-resource planning |
| Micro Tower Command | Overbroad Dart range and hidden costs → localized coverage, Frost/upgrade synergy, stronger brutes, pad cost/range and next-wave information | 16–17 tested wins | Lone towers lose at all pads; two perfect defences; turn-by-turn restore; stalled-loss regression | Fixed waves; lengthy pad labels; Runner differs by health, not speed |
| Pulse Reactor | Mistakes left pattern hidden; future RNG lost → re-study same pattern, saved run seed, exact combo/error feedback, earned streak record | 75 perfect; 3-error loss | All ten levels ×32 seeds; future-pattern equivalence; retry-score bounds; migration | Twelve-symbol final recall; long for one short break |

## Rankings and overall assessment

Scores are editorial judgments from inspected rules and simulations: **1 weak,
5 strong**. They are not player-study results or device-rendering scores. Order is
depth first, then replay, clarity and notification suitability; equal profiles tie.
The individual columns expose the ranking in each requested dimension.

| Rank | Game | Depth | Clarity | Replay | Notification suitability |
|---:|---|---:|---:|---:|---:|
| 1= | Cipher Vault | 4 | 4 | 4 | 3 |
| 1= | Lights Out Mini | 4 | 4 | 4 | 3 |
| 3 | Tic-Tac-Toe | 4 | 4 | 3 | 3 |
| 4= | Quick Math | 3 | 4 | 4 | 4 |
| 4= | Memory Challenge | 3 | 4 | 4 | 4 |
| 4= | Number Sequence | 3 | 4 | 4 | 4 |
| 7 | Nim | 3 | 5 | 3 | 5 |
| 8 | Neon Tactics | 3 | 4 | 3 | 4 |
| 9= | Mastermind Mini | 3 | 4 | 3 | 3 |
| 9= | Connect Three | 3 | 4 | 3 | 3 |
| 9= | Cyber Heist | 3 | 4 | 3 | 3 |
| 9= | Starship Survival | 3 | 4 | 3 | 3 |
| 9= | Micro Tower Command | 3 | 4 | 3 | 3 |
| 14 | Prism Protocol | 3 | 3 | 3 | 3 |
| 15 | Pocket Dungeon | 3 | 4 | 2 | 4 |
| 16 | Pulse Reactor | 2 | 4 | 4 | 4 |
| 17 | Higher or Lower | 2 | 5 | 3 | 5 |
| 18= | Trivia Blitz | 2 | 5 | 3 | 4 |
| 18= | Anagram Challenge | 2 | 5 | 3 | 4 |
| 20 | Word Scramble | 2 | 4 | 3 | 4 |
| 21= | Odd One Out | 2 | 5 | 2 | 4 |
| 21= | Guess the Pattern | 2 | 5 | 2 | 4 |
| 23= | Tap Counter | 1 | 5 | 2 | 5 |
| 23= | Rock Paper Scissors | 1 | 5 | 2 | 5 |
| 25 | Emoji Decode | 1 | 4 | 2 | 3 |

- **Depth leaders:** Cipher Vault and Lights Out combine inference with reusable
  problem spaces. Tic-Tac-Toe has genuine defensive reasoning but a fixed perfect
  opponent limits discovery. The campaign/resource games now offer defensible
  tradeoffs rather than reward loops, but remain small authored encounters.
- **Clarity leaders:** Nim, Higher/Lower, short clued decks and the explicitly
  simple counter/chance games. Clarity is not the same as depth.
- **Replay leaders:** Cipher, Lights Out and the seeded arithmetic/sequence/memory
  games. Fixed maps, tiny content banks and bounded record ceilings cap others.
- **Notification-fit leaders:** Nim and Higher/Lower offer few short choices.
  Tap/RPS are mechanically simple to render; their depth stays low. Dense boards,
  coordinate puzzles and notebooks trade compactness for better decisions.

The greatest remaining quality gap is **device usability**, followed by content
variety and long-term balance. It would be inaccurate to call every game a deep
strategy title or this build visually certified. The strongest feasible direction
is to foreground a smaller set of deduction/tactics/resource games while keeping
the simpler diversions honestly labeled; no inventory changes were made here.

## Shared implementation and interaction verification

- `core/GameAction.kt`: optional labeled discrete progress, typed record candidates
  and unavailable-action reasons. Existing action IDs and pure game interfaces stay.
- `notification/NotificationController.kt`, `NotificationSurface.kt`,
  `ActionEnvelope.kt`: compact objective/result, native board auxiliary actions,
  large-font row navigation, retained final boards/alternate replay controls,
  enabled-state semantics, strict action envelopes and idempotent refresh.
- `data/ArcadeStore.kt`, `PersonalRecord.kt`: first-puzzle persistence, shared
  transaction lock, atomic state/revision/stats/record writes, restore/recovery
  notices, independent higher-/lower-is-better records. No round rewards from
  menu navigation, stale envelopes or duplicate consumed actions.
- `ui/MainActivity.kt`: current-run preview/progress, persistent record lines and
  readable motif foregrounds. Results distinguish current state from personal best.
- `res/layout/notification_game*.xml`, `res/values/notification_styles.xml`,
  day/night `colors.xml`: two-row compact layout, two-column answers, actual
  determinate progress, semantic result colors and tested-source contrast.

All 25 model files changed. New JVM suites contain **70 tests**:
`OriginalGamesPolishTest` (16), `ShelfGamesPolishTest` (18),
`CampaignPolishTest` (16), `StrategyPolishTest` (14),
`notification/NotificationPolishTest` (6). Existing test expectations changed only
for intentionally revised Cipher controls, tower labels and hazard-aware Starship
routes. No existing tests were disabled or removed.

The shared suite walks **80 accepted states per game**, checking control counts,
progress bounds, byte-stable rejected disabled actions, exact saved/rendered state
and continued playable controls. It additionally tests all partial/stale envelope
combinations, record ties/unit mismatches/zero-capable values, board row identities,
compact outcomes and terminal board/alternate-control retention. Focused suites
prove the winning/loss/scoring paths detailed above; random walks alone are not
claimed to prove every game's completeness.

`app/src/androidTest/java/com/adbreak/arcade/NotificationSmokeTest.kt` now covers
initial randomized persistence, repeated/stale actions across controller instances,
atomic revision updates, idempotent show, record reset/clear semantics, unreadable
save recovery, menu navigation without progression and all-game RemoteViews
inflation. **These tests compiled; none ran on a device here.**

## Final validation

Final integrated invocation:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug check assembleDebug assembleDebugAndroidTest --console=plain
git diff --check
```

| Check | Actual result |
|---|---|
| `testDebugUnitTest` | PASS — 171 tests in 21 suites, 0 failures/errors/skips |
| `lintDebug` | PASS — 0 errors, 93 warnings |
| `check` | PASS |
| `assembleDebug` | PASS |
| `assembleDebugAndroidTest` | PASS — test APK compiled/packaged |
| `git diff --check` | PASS |
| `connectedDebugAndroidTest` | BLOCKED/FAILED — `DeviceException: No connected devices!` |
| APK metadata (`aapt dump badging`) | PASS — package/version/SDK values below |
| APK signature (`apksigner verify --verbose`) | PASS — v2 signature, one debug signer |

Warnings include KTX/style recommendations, unused resources, hard-coded English
UI text, dependency/toolchain currency and synchronous preference-write advice.
They were not suppressed to obtain a pass. Localization and on-device performance
remain unverified. This task did not run a release build or website checks because
the changed product surface is the Android debug candidate and repository reports.

### Verified APK

```text
Path: app/build/outputs/apk/debug/app-debug.apk
Bytes: 6,717,146
SHA-256: 36A8C05280D5315F288D42402225DA44C73B8944F274EDC75861F3E2916EEE30
Application ID: com.adbreak.arcade
Version: 1.4.0 (code 5)
Minimum SDK: 26
Target/compile SDK: 36
Signing: local debug certificate; v2 verification passed
```

The APK corresponds to the final integrated source, including lead fixes. It is
not a signed production release. Actual installation, compact/expanded clipping,
font scaling, TalkBack, OEM notification behavior and process-death device tests
remain open. See [the visual QA evidence](notification-polish-qa.md) for the exact
source contrast measurements and device review matrix.
