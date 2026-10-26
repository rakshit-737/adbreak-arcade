# Shelf games: model audit and focused polish

## Scope and evidence

Changed only the ten assigned game models, `app/src/test/java/com/adbreak/arcade/core/ShelfGamesPolishTest.kt`, and this report. This is source inspection and deterministic JVM simulation, not device, screenshot, notification-shade, accessibility, or media-playback evidence. Shared controller/store/UI integration belongs to the lead session.

The two priorities were (1) understandable, fair decisions and consequences and (2) a useful ending/replay instead of an endless identical loop. Existing public constructors/actions and accepted save versions remain available. Added save fields are optional when reading old saves. Legacy endless quiz/RPS/anagram sessions continue until New Game, rather than retroactively declaring a result from unlimited counters. New sessions have bounded, comparable records. Trivia's four-card migration now also survives a second save/restore without losing its pending question.

`GameProgress` describes an objective or completed actions, never elapsed time. Records appear only at a comparable result; lower-is-better is explicit for guesses, mistakes, and solved moves. Disabled-action reasons are supplied where a visible action is actually unavailable: repeated Mastermind guesses, oversized Nim takes, and full Connect Three columns. Other controls are legal while visible; terminal choices are replaced by replay controls.

## Player questions used below

1. **Objective:** What am I trying to accomplish?
2. **Consequences:** What happens when I choose an action?
3. **Meaningful:** Does choosing differently matter?
4. **Fair:** Is the answer/opponent fair and explainable?
5. **Difficulty:** What skill or challenge is required?
6. **Recovery:** Can I recover from an error?
7. **Completion:** How and when does the session end?
8. **Replay:** What changes on another run?
9. **Collapse/refresh:** Is the same state preserved without timing pressure?
10. **Notification-only:** Are the decisions and feedback feasible inside the notification?

## Rock Paper Scissors

**Baseline:** Endless single throws against random moves, cumulative wins/losses/draws, and an inaccurate suggestion that the random opponent could be outsmarted. No match result.

**Improvements:** Five-throw matches with a committed, persisted random plan; honest chance-game wording plus the winning relationship in feedback. A draw consumes one throw. Record: wins out of five, explicitly a chance result rather than a strategy rating.

1. Objective: win more throws than the arcade within five throws.
2. Consequences: one tap resolves one locked throw and updates the three outcome counters.
3. Meaningful: the selected move determines the relationship, but no choice has an advantage against uniform randomness.
4. Fair: the arcade plan is drawn before input, not in response to the player's move. Feedback says, for example, “Paper covers Rock.”
5. Difficulty: low; learning the cyclic rule, not discovering a hidden opponent strategy.
6. Recovery: a loss does not end the match; remaining throws can change the overall result.
7. Completion: exactly five gameplay taps, then won/lost/tied and a New Match action. Further throw actions do nothing.
8. Replay: a new random plan; all five pending throws persist during the current match.
9. Collapse/refresh: no clock. A restored game with a different random source produces the same remaining match outcomes. The next match may legitimately draw a new plan.
10. Notification-only: three short buttons, counters, throw progress, and one relationship sentence. Suitable in principle; no on-device verification.

**Tests/bugs:** All nine player/opponent pairs, five-throw terminal scoring, no mutation after completion, restoration with different RNG, and replay reset. Remaining limitation: intentionally shallow chance play.

## Odd One Out

**Baseline:** Five fixed cards repeated forever; a wrong answer named the odd item without explaining the category.

**Improvements:** One five-card scored deck with rotated starting card/answer positions on replay; every result states the category contrast (plant/animals, shape/colors, odd/even, month/weekdays, number/shapes).

1. Objective: identify the item outside the common category on all five cards.
2. Consequences: each answer earns zero or one point, resets/extends the streak, explains the grouping, and advances.
3. Meaningful: category recognition changes the score; the outcome is not dependent on speed.
4. Fair: the intended rule is stated after both correct and incorrect answers. Alternative abstract classifications remain possible in any odd-one-out task.
5. Difficulty: elementary categorization; no artificial escalation.
6. Recovery: a mistake costs one point, not the remaining deck. Next card remains playable.
7. Completion: five taps, result out of five; perfect is WON, otherwise completed DRAW rather than a false claim of victory.
8. Replay: rotated card start and answer positions. Content remains five cards, so memorization is still a ceiling.
9. Collapse/refresh: card, offset, counters, feedback, and final result round-trip exactly; legacy unlimited runs can finish on their own terms or start a new bounded deck.
10. Notification-only: four short labels and a one-sentence explanation. Progress is “Items answered.” Category feedback may need expanded layout.

**Tests/bugs:** Every card correctly answered at every replay offset, all-wrong completion, terminal immutability, score/restore/legacy checks, and invalid offsets rejected atomically.

## Emoji Decode

**Baseline:** Four endlessly repeated emoji cards; the honey/bee clue order was reversed, the sunflower clue already contained a sunflower, and “Warm puppy” competed with the intended hot-dog reading.

**Improvements:** Fairer left-to-right compounds with explicit decomposition; a four-clue scored deck and replay rotations. Honey precedes bee, generic flower follows sun, and the competing literal hot-dog distractor was removed.

1. Objective: combine the two symbols into the named phrase.
2. Consequences: answer once, see the word decomposition, then receive the next clue.
3. Meaningful: phrase recognition determines the point; positions rotate between runs.
4. Fair: “Hot + dog,” “Sun + flower,” “Ice + tea,” and “Honey + bee” explain the intended interpretation.
5. Difficulty: easy English compound recognition; emoji literacy is a real dependency.
6. Recovery: a wrong answer loses one point and reveals the translation, with no full-run reset.
7. Completion: four taps; correct count out of four, with a replay button.
8. Replay: different start/positions, not an unlimited new vocabulary bank.
9. Collapse/refresh: the same clue and controls survive serialization; no timer or delayed animation changes the answer.
10. Notification-only: four choices and two symbols fit conceptually, but font-specific emoji rendering and truncation must be checked on device. Decomposition feedback gives a text explanation after the choice.

**Tests/bugs:** All four intended phrases at all four offsets, wrong-answer results, stable terminal restore, and atomic invalid-state rejection. Remaining: small English-only bank and platform emoji appearance.

## Trivia Blitz

**Baseline:** Eight fixed facts repeated without an end. “What gas do plants absorb?” was ambiguous because plants also respire. Legacy four-card saves migrated once but their cursor could fail the next restore.

**Improvements:** Eight-question results with replay rotations; factual rationale and a photosynthesis-specific question. Migrated cursors preserve the pending question and become internally consistent in the eight-card deck.

1. Objective: answer as many of the eight questions correctly as possible.
2. Consequences: zero/one point, explanation, then the next question.
3. Meaningful: knowledge changes the result; the correct option is not fixed to the first button.
4. Fair: the gas question now names photosynthesis, and each answer includes a short fact or definition.
5. Difficulty: accessible general knowledge; no time pressure and no difficulty selector.
6. Recovery: a wrong answer does not stop the remaining questions.
7. Completion: eight gameplay taps, score out of eight, then New Deck. This is the longest quiz in this group.
8. Replay: order start and choices rotate; the same eight facts still limit long-term replay.
9. Collapse/refresh: question, score, offset, and feedback persist. The v1-to-v2 migration is checked before and after another answer and save.
10. Notification-only: four choices; prompt length and explanatory feedback are the main layout risks. Expanded notification testing remains necessary.

**Tests/bugs:** Every question at all eight offsets, all-wrong scoring, photosynthesis wording, answer-position variety (existing test), and repeated legacy migration. Record is correct answers out of eight, not a lifetime count.

## Mastermind Mini

**Baseline:** Choose one of four three-digit codes, receive exact/misplaced counts, and stop after a solution or four attempts. New Game always returned to code 123. Previous clues disappeared, repeated guesses consumed attempts, and solving advanced the internal code before the terminal screen.

**Improvements:** Rotate codes on replay while retaining the current code through its result; retain compact clue history and disable already-tried guesses with a reason. Exact/misplaced terminology is explained up front. Record: guesses to crack, lower is better; old cumulative sessions are excluded from that record.

1. Objective: identify which of the four codes is secret using as few guesses as possible.
2. Consequences: an untried choice adds an exact-position and wrong-position clue; duplicate guesses do not consume attempts.
3. Meaningful: use prior clues to eliminate candidates and improve guess count instead of forgetting the previous result.
4. Fair: multiset scoring correctly counts repeated digits only as many times as present. The answer is among the four options.
5. Difficulty: light deduction. Four distinct guesses guarantee a solution, so the meaningful objective is efficiency, not survival.
6. Recovery: wrong guesses remain visible as evidence; they no longer waste the same attempt repeatedly.
7. Completion: one to four accepted guesses. A pre-existing nearly exhausted legacy session can still lose and reveal the code. New rounds cannot lose if all distinct candidates are eventually tried.
8. Replay: four code/option sets rotate; the terminal result no longer silently points at the next set. Memorizing the small bank remains possible.
9. Collapse/refresh: guessed mask, history, attempt count, outcome, and replay cursor are restored; legacy missing masks are tolerated.
10. Notification-only: four fixed buttons, disabled tried options, one current clue, and a compact `code: E/M` history line. History may truncate at larger font sizes; actual readability is unverified.

**Tests/bugs:** Exhaustive 64 secrets × 64 guesses, including repeated digits; all four replay sets solved after three incorrect distinct guesses; duplicate rejection without mutation; history/result restore; and a legacy last-attempt loss. Fixed always-restart-first-code and premature puzzle advancement.

## Nim

**Baseline:** Thirteen stones every round, take 1–3, perfect modular reply from the arcade. The last-stone win rule and strategy were easy to miss; impossible takes disappeared from the controls.

**Improvements:** Explicit last-stone/multiples-of-four teaching and tactical result feedback; cycle through 13, 14, and 15 stones, each initially winnable. All three takes stay visible, with unavailable takes disabled and explained.

1. Objective: take the final stone yourself.
2. Consequences: remove 1–3 stones, then the arcade immediately removes 1–3 unless you already won.
3. Meaningful: leaving a multiple of four gives control; the wrong take can hand control to the arcade.
4. Fair: every offered opening is a winning position with correct play. The arcade follows the same last-stone rule and its count is explained.
5. Difficulty: deterministic arithmetic strategy, learnable from the initial tip and replies. No hidden luck.
6. Recovery: against the optimal reply, a mistake that hands over control cannot generally be recovered. Feedback explains why and New Round offers another opening.
7. Completion: normal 13–15-stone games finish in three or four player turns, with win/loss from stored outcome counts rather than matching feedback text.
8. Replay: the opening take changes with 13/14/15 stones. After learning the invariant, the game remains mechanically simple.
9. Collapse/refresh: starting pile, remaining stones, turn count, result, and hint persist; old saves default to the original thirteen-stone start.
10. Notification-only: three short buttons, one pile count, removal progress, and reply text. No board interpretation or timer needed; model suitability is high.

**Tests/bugs:** Every take on each pile from 1 through 15, oversized-take reasons and no mutation, independent last-stone/reply expectations, restore, and optimal wins from all three openings. Record is binary round won, avoiding a misleading fastest-move comparison between different pile sizes.

## Connect Three

**Baseline:** Gravity-based 3×3 board against win/block/center-priority AI. Three column actions, board compressed into one text line, deterministic identical replay, and no disabled reason for a full column. Terminal phase depended on feedback wording.

**Improvements:** Rotate fallback column preferences for replay; explain arcade drops/blocks and immediate threats, expose full-column reasons, and derive result from the board. Added an explicit replay button at the result.

1. Objective: connect three X tokens in a row, column, or diagonal before O does.
2. Consequences: the token lands in the lowest empty position of the selected column; the arcade replies unless the player has ended the game.
3. Meaningful: blocks, forks, gravity, and column capacity matter. Full columns cannot consume a turn.
4. Fair: the AI uses legal gravity moves and checks its immediate win before blocking. It is a simple tactical opponent, not advertised as unbeatable.
5. Difficulty: small tactical search; fallback preferences add variation without changing the rules.
6. Recovery: no undo. Before the next move, feedback can identify an immediate arcade threat. Losing positions may already be forced.
7. Completion: at most five player actions; X win, O win, or a full-board draw. Record is result value (win 2, draw 1, loss 0).
8. Replay: three fallback orderings produce different continuations; urgent wins and blocks retain precedence.
9. Collapse/refresh: exact board, player-move count, AI preference, and terminal status persist. Model results are board-derived even if wording changes.
10. Notification-only: three column buttons, `row / row / row` board text, tactical feedback. The compressed board is still the main usability weakness and requires real expanded-notification validation. Progress means X tokens in the best unblocked line and can decrease after an opponent block.

**Tests/bugs:** Winner detection on all 19,683 ternary boards, invalid winner marks, traversal of every reachable player branch for all three AI preferences, gravity/turn legality, disabled full columns, terminal immutability, and restore. Existing gravity validation is retained; exhaustive reachability validation for arbitrary externally fabricated save boards is not added.

## Guess the Pattern

**Baseline:** Four fixed sequences repeated forever. The Fibonacci-like `1, 1, 2, 3` card incorrectly accepted 4 instead of 5, and short sequences had no stated intended rule.

**Improvements:** Correct Fibonacci scoring and explicit rule context/explanations; bounded four-pattern decks with rotated card start and answer positions.

1. Objective: select the next item under the indicated sequence rule.
2. Consequences: receive zero/one point, see the calculation/rule, and advance once.
3. Meaningful: recognizing the step or previous-two sum changes the answer.
4. Fair: the sum rule now accepts 5 and explains `2 + 3 = 5`; “same step” narrows otherwise underdetermined number/letter sequences.
5. Difficulty: elementary arithmetic and letter/shape progression; explicit rule cues favor clarity over a misleading puzzle challenge.
6. Recovery: one missed point, then another pattern; no punishment loop.
7. Completion: four taps, correct count out of four, then replay.
8. Replay: reordered start/options; only four underlying rules, so mastery arrives quickly.
9. Collapse/refresh: the pending pattern and scoring survive restore, including old unlimited counters until New Deck.
10. Notification-only: four short choices, compact sequence, one explanation. Repeated triangle symbols may still wrap at large font sizes.

**Tests/bugs:** Correct answer for each card at every offset, all-wrong completion, explicit Fibonacci regression, score/terminal/restore, and atomic offset validation. The incorrect answer was a factual gameplay bug, not an intentional alternate pattern.

## Anagram Challenge

**Baseline:** Five words looped indefinitely. Choices mixed the required letter with unrelated alphabet letters, making many decisions trivial. Wrong letters erased the entire built prefix, and anagrams such as SPARK/PARKS lacked a disambiguating clue.

**Improvements:** Give a semantic clue and use only unused word letters as distractors; keep correct prefix after a mistake, but end one-word rounds after three mistakes or the completed word. Replay advances to the next word.

1. Objective: build the five-letter word matching the scramble and clue.
2. Consequences: the correct letter extends the prefix; a wrong letter adds one mistake without erasing correct work.
3. Meaningful: all displayed choices come from unused letters, so solving the clued anagram matters. The final letter is intentionally a one-button confirmation.
4. Fair: the clue distinguishes the intended word; the required next letter is always offered. At most four letters are shown because of the control budget.
5. Difficulty: low-to-moderate word recognition, with progressive confirmation rather than arbitrary alphabet distractors.
6. Recovery: two mistakes are recoverable and built letters stay. The third ends the round and reveals the clued answer.
7. Completion: a win takes five to seven taps; a loss takes three to seven taps. Record is mistakes per solved five-letter word, lower is better. Failed rounds do not submit a solved-word record.
8. Replay: five different words/clues rotate. Long-term replay remains limited by this bank.
9. Collapse/refresh: prefix, mistakes, attempts, clue index, and result restore exactly. Old saves retain valid prefixes and legacy cumulative counters.
10. Notification-only: one to four single-letter choices, a readable built-prefix line, clue, and error feedback. Four-choice filtering excludes one unused letter at the start but never the correct next letter; this remains a guided construction game rather than a free-text anagram.

**Tests/bugs:** Every word solved after a recoverable mistake, correct unused-letter inventory, one-choice final step and hidden-action rejection, three-mistake answer reveal, legacy prefix restore, terminal immutability, scoring and replay. The first focused test run caught a Kotlin `List<Char> + String` overload producing a multi-letter option; converting the distractor string to a character list fixed it. The previously failing test passed on the focused rerun.

## Lights Out Mini

**Baseline:** One 3×3 starting board replayed forever. Orthogonal toggle mechanics were implicit and feedback only counted moves. Existing tests require Retry/New Game to reproduce the original board and retain lifetime wins.

**Improvements:** Explain self/orthogonal flips and reversibility, plus exact remaining minimum presses; offer a separate New Puzzle action cycling six solvable equal-par boards while Retry preserves the current puzzle.

1. Objective: turn all nine lights off.
2. Consequences: pressing a numbered cell flips itself and its orthogonal neighbors, including currently-off cells.
3. Meaningful: overlapping flips create a real parity puzzle; counting dark cells alone is not a reliable strategy.
4. Fair: every 3×3 light state is solvable, and each curated starting board has optimal distance three. “N presses from dark” is computed exactly over the finite space.
5. Difficulty: spatial/parity reasoning; the distance hint makes progress understandable without giving the next cell away.
6. Recovery: pressing the same cell twice cancels its effect before completion. There is no fail timer or forced loss.
7. Completion: all-off is terminal. Curated boards need at least three presses, but there is no maximum; a move cap would punish experimentation. Record is solved moves on par-three puzzles, lower is better.
8. Replay: Retry repeats the current board; New Puzzle cycles six distinct par-three boards and preserves lifetime wins/best. This preserves existing replay behavior while making variation available.
9. Collapse/refresh: board, moves, counters, selected puzzle, and terminal result persist. No automatic resets or time-based state advances.
10. Notification-only: nine numbered ON/OFF cells plus one New Puzzle action while playing; two replay controls on completion. This is the densest control surface in the group. “Lights off (may decrease)” labels nonmonotonic objective progress honestly. Expanded layout/touch sizing need real-device checks.

**Tests/bugs:** Independent breadth-first solver visits all 512 states and verifies exact distances; all nine presses from each of the 511 nonterminal states verify orthogonal effects and reversibility. All six starts are distinct and exactly par three, with solved scoring, replay/restore, and lifetime-win preservation. Existing original-board solution/replay tests pass in the initial focused run.

## Ratings after changes

1 = weak/very limited, 5 = strong. These are source/model-based judgments, not measured player-study or device ratings. “Notification” is suitability for the available interaction budget, not a verified rendering score.

| Game | Depth | Clarity | Replay | Notification |
| --- | ---: | ---: | ---: | ---: |
| Rock Paper Scissors | 1 | 5 | 2 | 5 |
| Odd One Out | 2 | 5 | 2 | 4 |
| Emoji Decode | 1 | 4 | 2 | 3 |
| Trivia Blitz | 2 | 5 | 3 | 4 |
| Mastermind Mini | 3 | 4 | 3 | 3 |
| Nim | 3 | 5 | 3 | 5 |
| Connect Three | 3 | 4 | 3 | 3 |
| Guess the Pattern | 2 | 5 | 2 | 4 |
| Anagram Challenge | 2 | 5 | 3 | 4 |
| Lights Out Mini | 4 | 4 | 4 | 3 |

## Checks actually run

1. Focused Gradle pass:

   ```powershell
   .\gradlew.bat testDebugUnitTest --tests "com.adbreak.arcade.core.ShelfGamesPolishTest" --tests "com.adbreak.arcade.core.NewArcadeGamesTest" --tests "com.adbreak.arcade.core.UltimateArcadeUpgradeTest" --console=plain
   ```

   **30 tests executed: 29 passed, one failed** (`everyAnagramUsesItsOwnLettersKeepsProgressAndHasABoundedResult`). The failure identified the multi-letter distractor bug described above. All 18 new test methods compiled and ran in this pass; the other two selected classes are existing tests and were not edited.

2. After the one-line Anagram fix, reran only the failing test to verify the cause was fixed:

   ```powershell
   .\gradlew.bat testDebugUnitTest --tests "com.adbreak.arcade.core.ShelfGamesPolishTest.everyAnagramUsesItsOwnLettersKeepsProgressAndHasABoundedResult" --console=plain
   ```

   **BUILD SUCCESSFUL.** The combined 30-test selection was not rerun after this one-line correction. Lead session owns the integration/full-suite run.

3. `git diff --check -- <the ten owned model paths> <ShelfGamesPolishTest.kt>`: passed with no output.

## Remaining issues and integration boundary

- Small static content banks remain intentionally small. Rotations prevent fixed-button repetition but do not create new knowledge or long-term difficulty progression.
- RPS is honest chance play; Mastermind can be brute-forced in four distinct guesses; Nim is solved by a simple invariant. Ratings reflect those limits.
- Lights Out has an unbounded number of moves by design, and quiz rounds inherited from old saves remain unbounded until New Game. They do not emit an invalid bounded-run record.
- Model serialization is tested. Actual notification collapse/reopen, Android process death, pending-intent refresh, store record aggregation, font scaling, talkback, and media continuity require lead/device integration checks.
- Connect Three board text, Mastermind clue history, longer trivia explanations, emoji appearance, and nine-cell Lights Out touch targets are the highest-priority device checks. No claim is made that Android keeps the shade open or the video uninterrupted.
- No shared action/controller/store/UI/registry/build files or other groups' models/tests were edited by this task.
