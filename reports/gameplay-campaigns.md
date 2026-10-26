# Campaign gameplay audit and improvements

Scope: `CyberHeistGame.kt`, `PocketDungeonGame.kt`, `NeonTacticsGame.kt`,
`CipherVaultGame.kt`, their tests, and this report. Model simulations only; no
emulator, physical-device, System UI, or media-continuity claims.

## Evidence and checks

Baseline observations below are traces of the original model rules, not claims
of a device playtest. Updated behavior was exercised by real Kotlin model tests.

One focused Gradle invocation completed successfully on 2026-10-26:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.adbreak.arcade.core.CyberHeistGameTest" --tests "com.adbreak.arcade.core.PocketDungeonGameTest" --tests "com.adbreak.arcade.core.NeonTacticsGameTest" --tests "com.adbreak.arcade.core.CipherVaultGameTest" --tests "com.adbreak.arcade.core.CampaignPolishTest"
```

**37 tests, zero failures/errors/skips**: 5 Cyber, 5 Dungeon, 5 Neon, 6 Cipher,
16 cross-campaign simulations. `git diff --check` passed for owned source/tests.
No full-suite, lint, APK, or device result is claimed by this task.

Measured simulation coverage:

- Neon: **2,139** reachable state identities through turn 10, including **233**
  winning and **41** losing terminal states. Each exposed action was checked
  against its disabled reason, every accepted successor restored, and ADVANCE
  never dealt surprise damage. A separate **1,500-action** post-drone
  defend/movement loop earned zero additional points.
- Heist: safe and all-cache strategies both completed in **9 decisions** across
  **128 seeds each**; restored mission-boundary states matched.
- Dungeon: complete 12-action relic/rest, safe/rest, and relic/ward routes;
  heavy attack versus parry, finite guarding, resource boundaries, restoration,
  terminal freeze, and replay.
- Cipher: every one of **64 beginner codes** solved without hints in at most
  **5 guesses**; all **512 hardest-level suffixes** with a known first position
  solved in at most **8 guesses** by a clue-driven minimax policy. The latter
  fixes the known prefix to `1`; symbol renaming covers other prefix values.
  This is an explicit strategy/coverage claim, not a claim that every human
  guessing policy wins.
- Clue evaluator: independent overlap-count oracle over **531,441** pairs of
  three-digit codes using digits 1–9; **4,140** canonical equality patterns
  covering every duplicate/distinct-symbol relationship for four-position
  secret/guess pairs; **65,536** binary pairs at the legacy eight-position
  length. Invalid lengths/symbol domains are rejected. The full 9^16 legacy
  pair space was not enumerated.
- All four games: version-1 migration, malformed/unsupported versions,
  atomic rejection, and new mechanic fields. Existing loss/paused/replay tests
  remain in place. Only intentional Cipher control expectations changed:
  disabled primary remains visible, and beginner input is limited to 1–4.

## Cyber Heist

### Baseline loop and diagnosis

Three missions × recon, breach, extract. Four choices per stage spend stealth,
tools, and intel; security reaching the mission limit loses immediately. Mission
completion replenishes S+2/T+1/I+1. The original nine-button safe route was
passive scan → social loop → clean exit repeated. Prices and alarm deltas were
hidden, mission difficulty/security were independently randomized, and score
was the only reason to try another route. A player could accidentally choose a
lethal action without seeing its cost.

### Implemented improvements

1. **Optional contracts and resource planning.** Deep scan maps a side cache;
   mapped DATA DUMP spends an extra intel and raises alarm by 3 rather than 1,
   paying +50 only on successful extraction. Finishing a mission at zero alarm
   pays +25. Recovering all caches requires preserving intel: the tested route
   uses zero-day on the first breach and social loop thereafter, ending with
   zero tools/intel. Clean and cache goals create different resource plans.
2. **Readable, paced choices.** Every label shows S/T/I costs and alarm change;
   lethal choices say `!LOCKDOWN`. Unaffordable choices remain visible with a
   reason. Fresh campaigns have predictable limits 6/7/8 and reward tiers
   1/2/3. Progress shows payload stages plus earned cache/clean counts. Resource
   counts lead the score line; record is the finite earned heist score.

Version 2 saves the optional-objective fields. Version 1 preserves its generated
missions/resources/score and starts without a retroactive cache/clean bonus.
Ordinary unmapped DATA DUMP retains its old costs, so legacy extraction states
do not become unaffordable merely because of migration.

### Ten player questions

| Question | Answer after the change |
|---|---|
| 1. What is my objective? | Secure three payloads in nine stages; stage/progress are explicit. |
| 2. Can I predict consequences? | S/T/I prices and alarm deltas are visible; reaching the limit explicitly warns of lockdown. Resupply is reported after a mission. |
| 3. Are choices meaningful? | Clean bonuses compete with risky mapped-cache exfils and the campaign's limited intel/tools. |
| 4. Is failure fair? | No hidden hit roll. Lockdown and lack of a viable next route explain losses. Resource planning beyond the current stage still matters. |
| 5. Does difficulty progress sensibly? | Fresh limits/reward tiers are ordered; carried resource depletion supplies pressure rather than a random early hard mission. |
| 6. Can I recover from a mistake? | Clean exit lowers alarm; mission boundaries resupply. A terminal lockdown cannot be undone. |
| 7. Is completion clear? | Campaign-clear phase, 9/9 progress, earned score, and REPLAY. |
| 8. Why replay? | Try clean versus cache plans and improve earned score; target names/loot vary by seed. |
| 9. What survives collapse/refresh? | Full mission set, resources, stage, alarm, optional objectives, pause, and feedback are serialized. Rendering does not advance a stage. |
| 10. Can I play notification-only? | Four discrete choices; no timer or keyboard. Compact cost labels need actual System UI readability testing. |

**Actions:** 9 accepted decisions to clear; replay/pause/resume are separate.
**Feedback:** resource hierarchy, security threshold, stage, completion bar,
optional-goal text, disabled reasons, and explicit terminal phase.
**Remaining:** fixed stage vocabulary limits long-term replay; seeded loot adds
luck to cross-seed score comparisons; future-route affordability is not fully
forecast. Long four-choice labels may truncate on narrow notification layouts.
**Ratings (1–5):** depth 3, clarity 4, replay 3, notification suitability 3.

## Pocket Dungeon

### Baseline loop and diagnosis

Five fixed rooms: goblin, cache, chapel, wraith, dragon. Enter combat, then attack,
guard, or drink. The primary-only route won in twelve accepted actions. Scout
was a free weakened enemy plus bonus score; rest and chest rewards dominated
their alternatives. Every enemy replied with constant damage. Most importantly,
guarding the goblin blocked all damage **and awarded +2 score forever**; potion
use also awarded points independently of progress.

### Implemented improvements

1. **Deterministic combat timing and finite scoring.** Two ordinary strikes are
   followed by a visible heavy strike (+3 incoming damage). Guarding the heavy
   fully parries and counters for 2 enemy HP; attacking is faster but exposes
   the player to the announced damage. Damage points count actual finite enemy
   HP removed, including counters; guard and potion clicks grant no points.
2. **Resource/optional-objective choices.** The cache relic costs 3 HP and pays
   +40 only on escape, with a safe alternative. Prayer gives +4 HP and a
   three-damage ward instead of rest's +8 HP. Scouting consumes a potion to
   weaken the enemy by 2. Potions and unavailable scouts remain visible with
   reasons; a lethal chest trap is disabled.

Version 2 persists combat beat, ward, and relic. Legacy states keep rooms,
enemies, health/resources and terminal status; the beat begins at an ordinary
strike. Old potentially farmed scores are conservatively rebuilt from finite
gold/enemy/completion achievements rather than imported as new records.

### Ten player questions

| Question | Answer after the change |
|---|---|
| 1. What is my objective? | Clear five rooms and defeat the dragon; extracting the relic is optional. |
| 2. Can I predict consequences? | HP, potions, ward, enemy HP and next strike/heavy damage are explicit. Room labels state the immediate resource trade. |
| 3. Are choices meaningful? | Attack for speed versus parry for safety; relic versus health; restoration versus protection; scouting consumes healing stock. |
| 4. Is failure fair? | Enemy attacks are deterministic and announced; potion use still permits the announced reply. No hidden combat roll. |
| 5. Does difficulty progress sensibly? | Enemy HP/base damage increase 5/2 → 8/3 → 12/4, while the same three-beat rule holds. |
| 6. Can I recover from a mistake? | Potions, chapel, and ward provide recovery; mistimed healing can be offset by the announced enemy hit. Defeat requires replay. |
| 7. Is completion clear? | 5/5 rooms, dragon defeated, relic result, final score and REPLAY. |
| 8. Why replay? | Optimize safety, speed, potion usage and optional relic completion. The map/enemies remain fixed. |
| 9. What survives collapse/refresh? | Health, combat beat, ward, relic, enemy state, progress, pause and feedback. No time-based enemy attacks occur. |
| 10. Can I play notification-only? | Two room choices or three combat controls; strike information is in subtitle rather than only transient feedback. Device typography remains unverified. |

**Actions:** tested primary route 12; guarding the opening goblin now ends after
9 guards plus entry (10), never an unbounded scoring loop. Relic/rest scored
291 and ended at 9 HP; safe/rest scored 245; relic/prayer ended at 11 HP.
**Feedback:** enemy telegraph, health-first resource line, visible potion count,
ward, objective progress and exact room rewards.
**Remaining:** primary attacks still provide a forgiving winning route; the
fixed map reduces surprise. Scouting's two-HP reduction only removes a whole
attack against the goblin under the current four-damage attack, so it is a weak
choice against later enemies. Gold is a reward rather than a spendable resource.
**Ratings (1–5):** depth 3, clarity 4, replay 2, notification suitability 4.

## Neon Tactics

### Baseline loop and diagnosis

3×3 board, central obstacle, player at 1, drone at 9, extraction at 7. A drone
turn follows each valid move/attack/defend. The original model already captured
STRIKE versus ADVANCE before the player's action, allowing fair evasion. The
core could be reached in two moves. However every move awarded +10 and every
defend +2, including after the drone was dead; best score was consequently an
unbounded patience record. A dead drone also permanently blocked its cell.

### Implemented improvements

1. **Accomplishment scoring fixes the exploit at its cause.** Movement and
   defending award zero. The drone's two finite HP pay 15 each; optional DATA
   pays 20 once. Extracting pays 25, plus 2 per surviving HP and a decreasing
   fast-extraction bonus (2 per unused turn below ten). Waiting cannot improve
   the completion bonus. Scores remain at most 107; best score tracks only
   finite earned points.
2. **Optional salvage route and readable board.** DATA at cell 3 makes a detour
   compete with the immediate core escape. HP loss, drone elimination, data and
   speed create different outcomes. A disabled drone's cell is traversable.
   Blocked/current/nonadjacent/finished cells remain visible with explanations;
   ADVANCE explicitly states it moves one cell without hitting.

Version 2 persists the one-time DATA flag and validates its earned-score
formula. Version 1 retains encounter state and win/loss counts, rebuilding
score/best from its finite drone/extraction achievements; historical farmed
best scores are intentionally not retained as legitimate records.

### Ten player questions

| Question | Answer after the change |
|---|---|
| 1. What is my objective? | Reach CORE 7; DATA 3 and disabling the drone are optional score goals. |
| 2. Can I predict consequences? | Current HP/drone HP, turn, board labels and STRIKE/ADVANCE telegraph are persistent. Invalid cells explain why. |
| 3. Are choices meaningful? | Take the safe two-turn escape or detour for data/combat rewards at a health/speed cost. |
| 4. Is failure fair? | An announced advance cannot suddenly hit. A strike can be evaded by moving out of adjacency; defending reduces it by one. |
| 5. Does difficulty progress sensibly? | One fixed, short encounter; pursuit naturally creates adjacency pressure. It does not pretend to be an escalating campaign. |
| 6. Can I recover from a mistake? | Reposition, evade or reduce a strike; no healing. Core extraction can end an encounter before another reply. |
| 7. Is completion clear? | CORE secured or signal lost, final phase/score, and REPLAY; terminal board actions are disabled. |
| 8. Why replay? | Improve the combination of data recovery, drone damage, remaining HP and speed. No repetitive action can add score. |
| 9. What survives collapse/refresh? | Board, HP, score, data, turn and terminal status persist; the next telegraph is derived from the same restored positions. |
| 10. Can I play notification-only? | Nine numbered cells and one defend control, no gesture or timer. Real-device board paging/label legibility requires integration testing. |

**Actions:** fast clear 2, score 53, full HP. Tested drone-plus-data route 11,
score 83, 4 HP. These are example strategies, not an optimal-score proof.
**Feedback:** persistent board, objective/data hierarchy, clear telegraph,
damage/miss messages, discrete core progress, explicit disabled reasons.
**Remaining:** two-move completion is intentionally easy; the fixed board has
limited discovery after its tradeoffs are learned. Defending a disabled drone
is harmless and still consumes a turn, but grants no points. A maximum-score
policy was not exhaustively optimized.
**Ratings (1–5):** depth 3, clarity 4, replay 3, notification suitability 4.

## Cipher Vault

### Baseline loop and diagnosis

Choose digits with nine cell actions, submit a full draft, get exact/misplaced
counts, optionally reveal a position. The original game started at four digits
from nine symbols and grew to eight while retaining six attempts and reducing
hints. At eight digits that is 43,046,721 possible codes before information.
Each keypress replaced the last clue; there was no saved guess history, and a
full draft could not be edited. A wrong intermediate guess also reset the
round-win streak, effectively making it a first-guess streak despite the label.

### Implemented improvements

1. **Persistent, usable deduction notebook.** Every submitted guess is saved;
   clues are reproducible from those guesses. The selected clue is the FIRST
   status line and remains there during typing/hints. On an empty draft, the
   ninth cell becomes `CLUE ›` and cycles the log for free. During typing it is
   backspace, including when the draft is full. Known positions stay in the
   subtitle; draft positions are separate, avoiding a hint being mistaken for
   an entered digit. Full history also remains in feedback text. A candidate
   count uses only hints and previously earned clues. Disabled input/hint/clear
   controls stay visible with reasons.
2. **Bounded difficulty and correct completion rewards.** Fresh levels use
   3/3/3/4/4 positions, alphabets 1–4 through 1–8, 6/6/7/7/8 attempts, and
   3/2/2/1/1 hints. Unused hints/attempts reward stronger deduction. The record
   is a legitimately completed-vault streak; wrong guesses within a successful
   vault do not reset it, while loss or abandoning a used vault does.

Version 2 persists rules profile, full history, history cursor and a legacy
history offset. Version 1 keeps its original secret/length/digit-9 action,
attempt budget, draft, hints and outcomes until the next vault. Its earlier
guesses cannot be reconstructed from the old save and are explicitly marked
unavailable. New guesses are recorded, and NEW VAULT adopts the modern rules.
The original public `evaluateClues` API still accepts repeated digits 1–9 at any
equal nonzero length.

### Ten player questions

| Question | Answer after the change |
|---|---|
| 1. What is my objective? | Match the stated number of positions using the stated digit range; repeats are allowed. |
| 2. Can I predict consequences? | A full SUBMIT spends one attempt, HINT spends one reveal, and edit/history browsing spends neither. Exact/misplaced meanings are explained. |
| 3. Are choices meaningful? | Information-rich probes versus likely solutions, and spending a hint versus preserving its score bonus. Candidate count makes progress visible. |
| 4. Is failure fair? | Multiplicity-aware clues are exhaustively cross-checked. Guesses and hints persist. Hardest-level simulations solve within the actual eight-attempt budget. |
| 5. Does difficulty progress sensibly? | Code length is capped at four and bigger alphabets gain attempts. Middle-level optimal difficulty has not been exhaustively measured. |
| 6. Can I recover from a mistake? | Backspace edits even a full draft; hints and the notebook support correction. A submitted wrong guess remains spent and informative. |
| 7. Is completion clear? | Opened/sealed phase, revealed secret, score, completed-vault streak and NEW VAULT. |
| 8. Why replay? | New seeded codes, gradually larger information spaces, a completion streak and efficiency bonuses. |
| 9. What survives collapse/refresh? | Code, entered draft, guesses, selected history entry, revealed positions, attempts, budgets and streak. Rendering itself consumes no resource. |
| 10. Can I play notification-only? | Digit cells, SUBMIT/CLEAR, HINT and contextual EDIT/CLUE require no keyboard. One selected clue fits the first status line; all history is browsable with an empty draft. Legacy digit-9 rounds lack this contextual ninth-cell paging. |

**Actions:** minimum successful fresh guess is 4 presses at three digits, 5 at
four. Simulated beginner deduction required at most 5 guesses = 20 presses
without hints/edits. The hardest hinted policy required at most 8 guesses =
41 presses including its one hint. Browsing/editing adds actions but no attempts.
**Feedback:** selected historical clue first, persistent known pattern,
candidate count, separate draft, remaining attempts/hints, progress measured by
best exact positions, and explicit terminal secret.
**Remaining:** a four-digit eight-symbol code can still demand a long ad-break
session and a deliberate strategy. Legacy eight-digit vaults retain their old
difficulty for compatibility; missing legacy clues cannot be recovered. Full
history does not fit a single notification status line; paging is therefore
part of the model flow, and actual layout/accessibility remains to be tested.
**Ratings (1–5):** depth 4, clarity 4, replay 4, notification suitability 3.

## Integration notes

- All four models populate the shared optional `progress`, `record` and
  `disabledActions` fields. No shared interfaces, persistence/controller/UI,
  registry, build files, or another game's files were edited by this task.
- Relevant original named suites are unchanged except the two documented
  Cipher expectations. New tests live in `core/CampaignPolishTest.kt`.
- Intentional version-1 score migrations in Neon/Dungeon remove non-comparable
  farmed points. They preserve encounter progress rather than resetting runs.
- Notification status can be clamped to one line for boards. Cipher therefore
  puts its selected clue first; costs/resources and extra report text may
  still truncate depending on the lead's final shared layout. This report
  establishes model correctness and limitations, not physical-device usability.
- Lead should integrate with its complete project suite and notification
  presentation checks. No commit or push was made.
