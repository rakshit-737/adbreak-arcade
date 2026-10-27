# Original seven games: model polish and simulation

## Scope and evidence

Owned changes: `app/src/main/java/com/adbreak/arcade/core/{TapCounterGame,QuickMathGame,TicTacToeGame,HigherLowerGame,MemoryChallengeGame,WordScrambleGame,NumberSequenceGame}.kt`, the new `app/src/test/java/com/adbreak/arcade/core/OriginalGamesPolishTest.kt`, and this report. No shared action/controller/store/UI/registry/build files or shared tests were edited.

**Evidence is source inspection and JVM simulation, not device play.** No emulator, physical phone, screen reader, large-font, dark-mode, OEM notification, media-continuity, or collapsed-shade rendering claims are made.

### Actual checks

One focused Gradle invocation was made:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.adbreak.arcade.core.OriginalGamesPolishTest --tests com.adbreak.arcade.core.TapCounterGameTest --tests com.adbreak.arcade.core.ExistingGamesImprovementTest --tests com.adbreak.arcade.core.GameplayExcellenceTest --tests com.adbreak.arcade.core.ArcadeGamesTest
```

Production Kotlin compiled successfully. Test compilation stopped in the concurrently edited, unowned `CampaignPolishTest.kt`: unresolved `stage` references at lines 243/246/257/276/280/283 and a missing closing brace at line 291, as observed at that invocation. Gradle unit tests therefore **did not execute**. Integration rerun belongs to the lead; this report does not claim a passing Gradle build.

To avoid another whole-project build, the five selected test sources were compiled directly with the cached Kotlin 2.2.10 `org.jetbrains.kotlin.cli.jvm.K2JVMCompiler`, using `-no-stdlib -no-reflect -jvm-target 17`, the Gradle-produced production classes, Kotlin stdlib, JSON, JUnit, and Hamcrest. Output was isolated under the approved local temporary `opencode/original-games-polish-tests` directory. This caught and fixed one new test's cross-module nullable smart cast. The first direct JUnit invocation needed the generated `R.jar` for the existing presentation-catalog assertion; adding that runtime dependency resolved the harness-only failure.

Final direct test command (classpath assembled from those discovered local artifacts):

```powershell
java -cp $classpath org.junit.runner.JUnitCore com.adbreak.arcade.core.OriginalGamesPolishTest com.adbreak.arcade.core.TapCounterGameTest com.adbreak.arcade.core.ExistingGamesImprovementTest com.adbreak.arcade.core.GameplayExcellenceTest com.adbreak.arcade.core.ArcadeGamesTest
```

**Result: `OK (45 tests)`, 0 failures, 0.624 seconds JUnit execution.** This includes 16 new tests and the existing original-game regressions. `git diff --check` also passed before the test run. No full build, lint, release, instrumentation, or installation check was run by this owner.

### Shared view-state contract

- Every owned model supplies discrete, honestly labeled `GameProgress`; no wall-clock timer or background callback was added.
- `GameRecord` is offered only at an earned terminal result: completed sprint points, ten-answer set correctness, unbeaten boards, round win streak, or fewest mistakes in a completed memory run. Existing public score/best properties remain available.
- Occupied/final Tic-Tac-Toe cells and duplicate Higher/Lower guesses stay visible with `disabledActions` reasons, and model application rejects them. Other games have no unavailable visible answer controls: terminal choices are replaced by their sole replay action, so their disabled map remains empty rather than inventing unavailable controls.
- Random injection is retained. Repeated render does not consume randomness; generated answer ordering is saved. Identical injected seeds and action paths reproduce quiz sets and memory replays. As before, the RNG stream itself is not serialized: restoration preserves the current board/prompt, not an identical future random stream across process recreation.

## 1. Tap Counter

**Baseline loop and decisions:** tap 20 times, automatically increasing the multiplier after each group of five; no strategic choice, failure, or timing test. All normal completed sprints score 50. Replay repeated the same sprint while incorrectly inviting the player to beat a fixed ceiling. One visible action; 20 accepted taps plus one replay action. Existing tests covered increment/reset, persistence, malformed input, milestone feedback, and terminal rejection. Legacy unlimited scores above 20 were rejected during migration.

**Implemented:** truthful completion/replay and combo copy, concise taps/score, structured target progress and completion-only records; legacy unlimited counts are capped only for target progress while their score/best are preserved. Legacy states without a complete modern streak do not offer a sprint record. No artificial strategy or timer was added.

**Ten player questions:**
1. Objective clear? Yes: reach 20 taps; visible current/target.
2. Consequences clear? Each tap earns the displayed combo points and advances the target.
3. Meaningful choices? Minimal; this remains a relaxing counter, not a strategy game.
4. Fair? Yes; no reaction-time, randomness, or hidden failure requirement.
5. Difficulty? Flat; combo growth rewards continuation, not increasing skill.
6. Recoverability? No loss state; reset starts a fresh sprint and preserves best.
7. Completion? Exactly 20 taps, persistent terminal feedback, later taps rejected.
8. Replay reason? A fresh short interaction; copy no longer promises an impossible higher normal score.
9. Collapse/refresh? Score, taps, completion, and feedback round-trip; time does not progress the game.
10. Notification-only? One button and compact essentials fit the model contract; actual OEM fit remains untested.

**Tests/remaining:** new 50-point/20-tap clear, no post-terminal scoring, earned-record/reset checks, and migration of a legacy 120-tap save. Remaining limitation: depth and replay variety intentionally stay low; the fixed score record is a completion metric with a ceiling.

## 2. Quick Math

**Baseline loop and decisions:** endlessly select one of four arithmetic answers; correct responses increase score/streak and demonstrated performance raises operand sizes. Wrong responses show the answer and reset streak, but there was no run result or stopping point. Four controls, unbounded action count. Existing tests covered exact division, unique options, invalid actions, feedback/accuracy, and restore validation.

**Implemented:** ten-answer sets, seven-right clear condition, stable final result and replay, progress and correctness-per-ten record. A mistake still explains the equation. Scoring and adaptive question generation remain compatible. Old endless saves keep their prompt/options/stats, finish a fresh ten-answer continuation, and remain record-ineligible until replay.

**Ten player questions:**
1. Objective clear? Solve the prompt and get seven of ten right; both goal and question count are visible.
2. Consequences clear? Correct gives points; wrong reveals the equation and consumes one question.
3. Meaningful choices? Yes, four arithmetic alternatives; choosing accurately controls the result.
4. Fair? Exact division, nonnegative subtraction, unique choices, and untimed input are preserved.
5. Difficulty? Accuracy/streak and solved count raise level; a fresh set resets it. New sets usually reach levels 1–4.
6. Recoverability? A single miss does not end the set; three misses can still clear it.
7. Completion? Always ten answers; seven or more is WON, otherwise LOST with a practice-complete summary.
8. Replay reason? New random equations and improving a bounded correctness record.
9. Collapse/refresh? Prompt, order, counters, result, and record eligibility persist; refresh does not advance the question.
10. Notification-only? Four numeric controls and a short equation; no keyboard or timer needed. Larger operands and font scale still require device checks.

**Tests/remaining:** 400 generated questions across four operations, exact arithmetic/division, uniqueness, both pass-boundary sides, terminal rejection, migration, replay, and equal-seed determinism. Remaining: nearby-number distractors can be predictable; adaptive/random sets are comparable practice scores, not identical competitive exams. Correctness is bounded; legacy score totals are not treated as new records.

## 3. Tic-Tac-Toe

**Baseline loop and decisions:** choose an empty cell; minimax Arcade responds. Blocking, forks, and center/corner choices matter, but beating perfect play is not achievable from a fresh board. Only wins scored, so successful defense/draws gave nothing; the final board disappeared. Nine cells; normally 3–5 player actions plus replay. Existing tests covered every win line, occupied rejection, immediate AI blocking, draws, terminal restore, and impossible turn counts.

**Implemented:** explain and award the achievable draw (+1), track unbeaten completed boards including draws, and break that streak when a partially played board is abandoned. Win scoring stays +3 and minimax is preserved. Show the final board, AI cell feedback, and visible disabled occupied/terminal cells with reasons. Progress measures filled cells, explicitly not a win probability.

**Ten player questions:**
1. Objective clear? Block O's three; a draw earns one. The description explicitly identifies perfect Arcade.
2. Consequences clear? Board marks, the AI's last cell, line-loss feedback, and draw reward are visible.
3. Meaningful choices? Yes; move placement and defensive reading determine draw versus loss.
4. Fair? Deterministic perfect play; a win is not advertised as the expected fresh-board goal.
5. Difficulty? Fixed expert opponent; no novice mode added.
6. Recoverability? Threats can be blocked; a missed forced loss ends that board, then replay is immediate.
7. Completion? Three in a row or full-board draw; terminal board remains inspectable.
8. Replay reason? Maintain an unbeaten streak and practice defense; the fixed AI can eventually be memorized.
9. Collapse/refresh? Board, result, streak, disabled cells, and score persist; refresh never awards another point.
10. Notification-only? Nine grid controls and terminal replay fit the existing grid/action slots. Actual grid size, final-board visibility, and large fonts need device evidence.

**Tests/remaining:** search the legal move tree for a five-move draw, replay it twice, verify scores/streak, retain all nine terminal cells, reject every terminal cell, and verify partial-reset streak breaking with round-trip checks. Existing defensive AI tests pass. Remaining: an expert fixed AI and deterministic replies limit novice accessibility and long-term variety; exhaustive fresh-board unbeatable-AI proof was not newly added.

## 4. Higher or Lower

**Baseline loop and decisions:** guess midpoint, low edge, or high edge and narrow the range. Wins grew the range from 20 to 50, but five guesses cannot guarantee success above 31 candidates. Any non-winning narrowing guess also reset a supposedly round-based win streak. Duplicate guesses remained enabled when the range became small. Three controls, 1–5 guesses, one replay action. Existing tests covered correct/miss terminal states, attempt exhaustion, restore, and replay.

**Implemented:** six guesses for ranges above 31, five otherwise; ordinary narrowing preserves round win streak, while a loss or abandoned attempted round breaks it. Disable duplicate edge guesses with a MID explanation and reject those actions without mutation. Show remaining tries, narrowing feedback, candidate-elimination progress, and a completed-win streak record. Active legacy large-range saves receive their missing extra guess; prior terminal outcomes stay terminal.

**Ten player questions:**
1. Objective clear? Find the secret inside the displayed range within the shown tries.
2. Consequences clear? MID is the safe narrowing choice; edges risk using more tries. Feedback says what was ruled out.
3. Meaningful choices? Some risk/efficiency choice exists, but MID is the dominant reliable policy.
4. Fair? Every generated secret can now be found by repeated MID within the budget, including 50.
5. Difficulty? Range grows in steps of five to 50; the extra guess preserves solvability rather than manufacturing difficulty.
6. Recoverability? Wrong guesses narrow the range and preserve the win streak; running out or abandoning a tried round breaks it.
7. Completion? Correct guess or zero tries; no scoring after terminal. New rounds take at most six guesses.
8. Replay reason? New secrets, growing ranges, and consecutive successful rounds.
9. Collapse/refresh? Secret, narrowed range, budget, feedback, streak, and duplicate-control reasons round-trip.
10. Notification-only? Three short labeled choices, current range, and tries; no typed numeric entry or live timer.

**Tests/remaining:** all **245 secrets** over range maxima 20/25/30/35/40/45/50 succeed with MID, with restore checks between guesses; ten consecutive wins preserve the streak; a partial reset breaks it; duplicate controls reject without mutation; large v3 ranges migrate fairly. Remaining: MID can be pressed repeatedly to guarantee a clear, so strategic depth is intrinsically limited. Streak measures completing rounds, not a difficult high-score skill; it cannot be preserved by abandoning a failed attempt.

## 5. Memory Challenge

**Baseline loop and decisions:** study a growing pattern then reproduce symbols with four buttons. The pattern hid after the first input; a miss cleared input but did not reveal it again. Three tries were replenished each level. The nominal endpoint at level 100 needed **5,150** correct inputs and a 101-symbol pattern, unsuitable for a short notification run. Every opening was `1 2`. Existing tests covered hiding after input, partial restore, level scoring, three-life failure, and replay.

**Implemented:** five-pattern runs (lengths 2–6, **20** correct inputs), varied seeded replay openings, and re-study after a mistake with the expected symbol explained. Keep the initial `1 2` tutorial opening for existing controls/tests. Show pattern goal, partial input count, and tries; award a lower-is-better mistakes record only on a completed standard run. Legacy levels above five keep their full sequence and can finish their current level; legacy continuations do not claim short-run records.

**Ten player questions:**
1. Objective clear? Clear five patterns; current pattern and entered-symbol count are visible.
2. Consequences clear? A miss loses one of three tries and explains the needed symbol; completing the pattern advances it.
3. Meaningful choices? Recall rather than strategy: choose the correct next symbol from four.
4. Fair? No timed disappearance; the pattern hides only after input, and mistakes permit re-study.
5. Difficulty? Grows from two to six symbols, a realistic notification-sized span.
6. Recoverability? Two misses per pattern are recoverable, then tries refill on advancement; the third miss loses the run.
7. Completion? Five patterns win; three misses in one pattern lose. Perfect clear needs 20 actions, with an upper bound of 60 for a standard run.
8. Replay reason? New seeded patterns/openings and fewer mistakes in a completed clear.
9. Collapse/refresh? Current pattern, hidden/revealed state, entered prefix, tries, and target persist; refreshing does not reveal a hidden pattern.
10. Notification-only? Four one-character choices and at most six symbols in new runs; old long saves can still exceed practical notification width.

**Tests/remaining:** full 20-input clear with zero-mistake record; one deliberate miss per level with re-study, no score farming, and five-mistake record; three-miss failure; terminal refresh/replay; varied deterministic openings; nine-symbol legacy pattern preserved; invalid lives/target rejected. Remaining: initial tutorial pattern is fixed, trial-and-error re-study is intentionally forgiving, and unusually long legacy saves remain visually awkward until replay. No device demonstration of study/hide transitions was performed.

## 6. Word Scramble

**Baseline loop and decisions:** choose the word matching a curated anagram from four options; correct responses increased score and streak, wrong responses revealed the word. Endless play, no completion. Distractors were the same alphabetically first same-length words, allowing recognition shortcuts. Difficulty increased word length only from five to six characters, despite higher level labels. Four controls; previously unbounded answer count. Existing tests covered anagram validity, visible mapping, no immediate repeats, and malformed saves.

**Implemented:** ten-word sets with seven-right clear, completion-only solved-per-ten record, progress/replay, and concise essentials. Distractors now prefer same length and shared letters, with seeded random tie order instead of alphabetical fixed distractors. Preserve one correct curated answer and immediate-repeat avoidance. Migrate old prompts/stats into an unranked ten-answer continuation, enabling records after a fresh replay.

**Ten player questions:**
1. Objective clear? Unscramble the shown letters; solve seven of ten.
2. Consequences clear? Correct awards points; wrong reveals the target and consumes one answer.
3. Meaningful choices? Letter recognition distinguishes four plausible same-length choices.
4. Fair? One matching anagram, four unique curated words, no timer; the displayed scramble differs from its answer.
5. Difficulty? Moves toward six-letter words; misleading high level labels are omitted. Vocabulary depth remains modest.
6. Recoverability? Up to three misses can still clear the set; replay starts a fresh set.
7. Completion? Ten answers always end the set, with a seven-right success threshold.
8. Replay reason? New scrambles, shuffled distractor ties, and a bounded correctness record.
9. Collapse/refresh? Exact word, scramble, options, progress, and outcome persist; no refresh reshuffling.
10. Notification-only? Four words up to six characters and short instructions; English vocabulary and font-scale fit still need device/usability review.

**Tests/remaining:** 400 generated scrambles, matching letter multiset, scramble inequality, no immediate repeated word, unique controls, pass/fail boundary, stale terminal rejection, migration, and seeded replay. Remaining: only 30 English words and no true advanced word-length progression; repeated play can memorize the pool. No claim of multilingual support or a validated learning curriculum.

## 7. Number Sequence

**Baseline loop and decisions:** infer a missing fourth term from arithmetic, geometric, alternating, square, or mixed rules. Correct answer was **always the first choice**, so repeatedly pressing PRIMARY earned endless points without solving. Difficulty grew parameters with solved count, but the round never finished. Four choices; unbounded action count. Existing tests checked a seeded correct missing term, rule restore validation, unique options, supported families, and invalid actions.

**Implemented:** generate and save a randomized answer order once per prompt using forward Fisher–Yates; rendering/restoration no longer change it. Ten-answer sets, seven-right clear, terminal replay/progress, and solved-per-ten earned records. Keep rule explanations after success or error. Legacy saves retain their original first-correct mapping for the currently displayed prompt, then use randomized future prompts; legacy continuations cannot submit a new bounded record.

**Ten player questions:**
1. Objective clear? Find the missing term and solve seven of ten; pattern and question progress are visible.
2. Consequences clear? Correct awards points and explains the rule; wrong reveals both missing value and rule.
3. Meaningful choices? Yes after shuffling: selecting a position alone is no longer a guaranteed solution.
4. Fair? Four distinct positive options include the validated answer. Inference uses the advertised five rule families.
5. Difficulty? Numeric parameters increase with solved questions; a short normal set reaches about levels 1–3.
6. Recoverability? Mistakes teach the rule and still allow a seven-right clear if at most three occur.
7. Completion? Exactly ten answers, then success/practice result and replay; terminal inputs cannot farm score.
8. Replay reason? Different families/numbers/order and a correctness record rather than endless automatic scoring.
9. Collapse/refresh? Sequence, answer order, result, and progress are persisted; render does not reroll controls.
10. Notification-only? Four numeric choices and five sequence positions; large numbers or verbose rule explanations still need real notification checks.

**Tests/remaining:** 400 generated prompts cover all five families and all four correct-answer positions; first-button-only play is not perfect; seeded existing first-question scoring still passes without a seed-specific special case; terminal/migration/order round-trips and duplicate serialized choices are checked. Remaining: finite sequences can support mathematical rules outside the curated families, and difficulty is parameter-based rather than independently balanced per family. No adaptive hint control was added.

## Ratings after changes (1 = weak, 5 = strong)

These are source/model judgments, not player-study or device ratings.

| Game | Depth | Clarity | Replay | Notification suitability |
|---|---:|---:|---:|---:|
| Tap Counter | 1 | 5 | 2 | 5 |
| Quick Math | 3 | 4 | 4 | 4 |
| Tic-Tac-Toe | 4 | 4 | 3 | 3 |
| Higher or Lower | 2 | 5 | 3 | 5 |
| Memory Challenge | 3 | 4 | 4 | 4 |
| Word Scramble | 2 | 4 | 3 | 4 |
| Number Sequence | 3 | 4 | 4 | 4 |

## Integration and remaining work

1. Lead must run integration after the concurrent test-source edits finish. The one Gradle attempt here was blocked; isolated JVM success does not replace Android resource/lint/build checks.
2. Verify shared rendering consumes `progress`, `record`, and `disabledActions`, including lower-is-better **zero** memory records, final Tic-Tac-Toe grid plus replay, and duplicated Higher/Lower controls. Model-side terminal and disabled-action guards already work independently.
3. Device checks remain necessary for collapsed/expanded text, grid legibility, large fonts, TalkBack reasons, and notification-only interaction. All seven games are event-driven; none need elapsed-time callbacks or intentionally launch an Activity, but system shade/media behavior was not tested.
4. Saved-state versions: Tap stays v3 with repaired v1/v2 migration; Tic-Tac-Toe advances to v3 accepting v1/v2; the other five advance to v4 accepting v1–v3. Existing public API names and action mappings remain intact. New quiz/memory continuations explicitly exclude incomparable legacy records.
5. Remaining design limits are documented per game: deterministic Tap ceiling, dominant MID strategy, fixed perfect Tic-Tac-Toe opponent, small English word pool, and limited short-set difficulty range. No unsupported claim of deep strategy, identical competitive scoring conditions, or device evidence is made.
