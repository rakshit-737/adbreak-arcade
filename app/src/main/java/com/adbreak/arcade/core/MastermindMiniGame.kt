package com.adbreak.arcade.core

import org.json.JSONObject

class MastermindMiniGame : ArcadeGame {
    override val id = "mastermind_mini"
    override val title = "Mastermind Mini"
    override val description = "Choose a three-digit code and use exact or misplaced hints to improve."

    private data class Puzzle(val secret: String, val choices: List<String>)

    private val puzzles = listOf(
        Puzzle("123", listOf("123", "132", "214", "431")),
        Puzzle("241", listOf("241", "214", "324", "111")),
        Puzzle("314", listOf("134", "314", "341", "412")),
        Puzzle("432", listOf("342", "423", "432", "111"))
    )
    private var puzzleIndex = 0
    private var attempts = 0
    private var solved = 0
    private var streak = 0
    private var best = 0
    private var over = false
    private var won = false
    private var comparable = true
    private var guessed = 0
    private var feedback = "Exact = right position · misplaced = right digit, wrong position"

    override fun reset() {
        puzzleIndex = (puzzleIndex + 1) % puzzles.size
        attempts = 0
        solved = 0
        streak = 0
        best = 0
        over = false
        won = false
        comparable = true
        guessed = 0
        feedback = "Exact = right position · misplaced = right digit, wrong position"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        if (over) return false
        val choiceIndex = when (action) {
            GameAction.PRIMARY -> 0
            GameAction.SECONDARY -> 1
            GameAction.CELL_1 -> 2
            GameAction.CELL_2 -> 3
            else -> return false
        }
        if (guessed and (1 shl choiceIndex) != 0) return false
        val puzzle = puzzles[puzzleIndex]
        val guess = puzzle.choices[choiceIndex]
        val (exact, misplaced) = match(puzzle.secret, guess)
        guessed = guessed or (1 shl choiceIndex)
        attempts++
        if (exact == puzzle.secret.length) {
            solved++
            streak++
            best = maxOf(best, streak)
            feedback = "Solved ${puzzle.secret} in $attempts guesses — 3 exact"
            over = true
            won = true
        } else {
            streak = 0
            feedback = "$guess: $exact exact · $misplaced misplaced"
            if (attempts >= 4) {
                feedback += " · Code was ${puzzle.secret}"
                over = true
            }
        }
        return true
    }

    override fun render(): GameViewState {
        val puzzle = puzzles[puzzleIndex]
        val actions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2)
        val history = puzzle.choices.indices.filter { guessed and (1 shl it) != 0 }
        val bestExact = history.maxOfOrNull { match(puzzle.secret, puzzle.choices[it]).first } ?: 0
        return GameViewState(
            title = title,
            subtitle = if (over) "Round over — start a new code" else "Crack one of four codes · Attempt ${attempts + 1}/4",
            scoreLine = if (history.isEmpty()) "Use as few guesses as possible" else history.joinToString(" · ") {
                val (exact, misplaced) = match(puzzle.secret, puzzle.choices[it])
                "${puzzle.choices[it]}: ${exact}E/${misplaced}M"
            },
            buttons = if (over) listOf("NEW GAME") else puzzle.choices,
            buttonActions = if (over) listOf(GameAction.NEW_GAME) else actions,
            feedback = feedback,
            isTerminal = over,
            phase = if (!over) GamePhase.PLAYING else if (won) GamePhase.WON else GamePhase.LOST,
            progress = GameProgress(if (won) 3 else bestExact, 3, "Exact positions in best guess"),
            record = if (won && comparable) GameRecord(attempts, "Guesses to crack", higherIsBetter = false) else null,
            disabledActions = if (over) emptyMap() else history.associate { actions[it] to "Already tried — use its exact/misplaced clue" }
        )
    }

    override fun encode() = JSONObject().apply {
        put("v", 2)
        put("puzzleIndex", puzzleIndex)
        put("attempts", attempts)
        put("solved", solved)
        put("streak", streak)
        put("best", best)
        put("over", over)
        put("won", won)
        put("comparable", comparable)
        put("guessed", guessed)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..2) return false
        val restoredPuzzleIndex = json.optInt("puzzleIndex", -1)
        val restoredAttempts = json.optInt("attempts", -1)
        val restoredSolved = json.optInt("solved", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredOver = if (version >= 2) json.optBoolean("over") else false
        val restoredFeedback = json.optString("feedback")
        val restoredGuessed = json.optInt("guessed", 0)
        val restoredWon = json.optBoolean("won", restoredOver && restoredFeedback.startsWith("Solved"))
        val restoredComparable = json.optBoolean("comparable", false)
        if (restoredPuzzleIndex !in puzzles.indices || restoredAttempts < 0 ||
            (version == 1 && restoredPuzzleIndex != restoredAttempts % puzzles.size) ||
            restoredSolved < 0 || restoredSolved > restoredAttempts || restoredStreak < 0 ||
            restoredStreak > restoredSolved || restoredBest < restoredStreak ||
            restoredBest > restoredSolved || restoredFeedback.isBlank() || restoredGuessed !in 0..15 ||
            Integer.bitCount(restoredGuessed) > restoredAttempts || (restoredWon && !restoredOver) ||
            (restoredComparable && (restoredAttempts > 4 || (restoredWon && restoredAttempts == 0)))
        ) return false
        puzzleIndex = restoredPuzzleIndex
        attempts = restoredAttempts
        solved = restoredSolved
        streak = restoredStreak
        best = restoredBest
        over = restoredOver
        won = restoredWon
        comparable = restoredComparable
        guessed = restoredGuessed
        feedback = restoredFeedback
        true
    }.getOrDefault(false)

    companion object {
        internal fun match(secret: String, guess: String): Pair<Int, Int> {
            val exact = secret.indices.count { it < guess.length && secret[it] == guess[it] }
            val common = secret.toSet().sumOf { digit ->
                minOf(secret.count { it == digit }, guess.count { it == digit })
            }
            return exact to common - exact
        }
    }
}
