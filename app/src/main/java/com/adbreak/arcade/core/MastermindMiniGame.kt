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
    private var feedback = "Pick a three-digit code"

    override fun reset() {
        puzzleIndex = 0
        attempts = 0
        solved = 0
        streak = 0
        best = 0
        feedback = "Pick a three-digit code"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        val choiceIndex = when (action) {
            GameAction.PRIMARY -> 0
            GameAction.SECONDARY -> 1
            GameAction.CELL_1 -> 2
            GameAction.CELL_2 -> 3
            else -> return false
        }
        val puzzle = puzzles[puzzleIndex]
        val guess = puzzle.choices[choiceIndex]
        val exact = guess.indices.count { guess[it] == puzzle.secret[it] }
        val common = puzzle.secret.toSet().sumOf { digit ->
            minOf(puzzle.secret.count { it == digit }, guess.count { it == digit })
        }
        val misplaced = common - exact
        attempts++
        if (exact == puzzle.secret.length) {
            solved++
            streak++
            best = maxOf(best, streak)
            feedback = "Solved — 3 exact"
        } else {
            streak = 0
            feedback = "$exact exact · $misplaced misplaced"
        }
        puzzleIndex = (puzzleIndex + 1) % puzzles.size
        return true
    }

    override fun render(): GameViewState {
        val puzzle = puzzles[puzzleIndex]
        return GameViewState(
            title = title,
            subtitle = "Code has 3 digits from 1–4",
            scoreLine = "Solved $solved  ·  Streak $streak  ·  Best $best",
            buttons = puzzle.choices,
            buttonActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
            feedback = feedback
        )
    }

    override fun encode() = JSONObject().apply {
        put("v", 1)
        put("puzzleIndex", puzzleIndex)
        put("attempts", attempts)
        put("solved", solved)
        put("streak", streak)
        put("best", best)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != 1) return false
        val restoredPuzzleIndex = json.optInt("puzzleIndex", -1)
        val restoredAttempts = json.optInt("attempts", -1)
        val restoredSolved = json.optInt("solved", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredFeedback = json.optString("feedback")
        if (restoredPuzzleIndex !in puzzles.indices || restoredAttempts < 0 ||
            restoredPuzzleIndex != restoredAttempts % puzzles.size ||
            restoredSolved < 0 || restoredSolved > restoredAttempts || restoredStreak < 0 ||
            restoredStreak > restoredSolved || restoredBest < restoredStreak ||
            restoredBest > restoredSolved || restoredFeedback.isBlank()
        ) return false
        puzzleIndex = restoredPuzzleIndex
        attempts = restoredAttempts
        solved = restoredSolved
        streak = restoredStreak
        best = restoredBest
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
