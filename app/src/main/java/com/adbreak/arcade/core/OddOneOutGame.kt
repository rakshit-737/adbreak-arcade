package com.adbreak.arcade.core

import org.json.JSONObject

class OddOneOutGame : ArcadeGame {
    override val id = "odd_one_out"
    override val title = "Odd One Out"
    override val description = "Find the one item that does not belong with the other three."

    private data class Puzzle(val choices: List<String>, val oddIndex: Int)

    private val puzzles = listOf(
        Puzzle(listOf("CAT", "DOG", "COW", "ROSE"), 3),
        Puzzle(listOf("RED", "BLUE", "CIRCLE", "GREEN"), 2),
        Puzzle(listOf("2", "4", "9", "6"), 2),
        Puzzle(listOf("MONDAY", "MARCH", "FRIDAY", "TUESDAY"), 1),
        Puzzle(listOf("▲", "7", "■", "●"), 1)
    )
    private var puzzleIndex = 0
    private var attempts = 0
    private var score = 0
    private var streak = 0
    private var best = 0
    private var feedback = "Find the odd item"

    override fun reset() {
        puzzleIndex = 0
        attempts = 0
        score = 0
        streak = 0
        best = 0
        feedback = "Find the odd item"
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
        attempts++
        if (choiceIndex == puzzle.oddIndex) {
            score++
            streak++
            best = maxOf(best, streak)
            feedback = "Correct — ${puzzle.choices[puzzle.oddIndex]} was odd"
        } else {
            streak = 0
            feedback = "Not quite — ${puzzle.choices[puzzle.oddIndex]} was odd"
        }
        puzzleIndex = (puzzleIndex + 1) % puzzles.size
        return true
    }

    override fun render(): GameViewState {
        val puzzle = puzzles[puzzleIndex]
        return GameViewState(
            title = title,
            subtitle = "Which item is different?",
            scoreLine = "Score $score  ·  Streak $streak  ·  Best $best",
            buttons = puzzle.choices,
            buttonActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
            feedback = feedback
        )
    }

    override fun encode() = JSONObject().apply {
        put("v", 1)
        put("puzzleIndex", puzzleIndex)
        put("attempts", attempts)
        put("score", score)
        put("streak", streak)
        put("best", best)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != 1) return false
        val restoredPuzzleIndex = json.optInt("puzzleIndex", -1)
        val restoredAttempts = json.optInt("attempts", -1)
        val restoredScore = json.optInt("score", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredFeedback = json.optString("feedback")
        if (restoredPuzzleIndex !in puzzles.indices || restoredAttempts < 0 ||
            restoredPuzzleIndex != restoredAttempts % puzzles.size ||
            restoredScore < 0 || restoredScore > restoredAttempts || restoredStreak < 0 ||
            restoredStreak > restoredScore || restoredBest < restoredStreak ||
            restoredBest > restoredScore || restoredFeedback.isBlank()
        ) return false
        puzzleIndex = restoredPuzzleIndex
        attempts = restoredAttempts
        score = restoredScore
        streak = restoredStreak
        best = restoredBest
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
