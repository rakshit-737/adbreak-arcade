package com.adbreak.arcade.core

import org.json.JSONObject

class EmojiDecodeGame : ArcadeGame {
    override val id = "emoji_decode"
    override val title = "Emoji Decode"
    override val description = "Translate a tiny emoji clue into the phrase it represents."

    private data class Puzzle(val clue: String, val choices: List<String>, val answerIndex: Int)

    private val puzzles = listOf(
        Puzzle("🔥 + 🐶", listOf("Hot dog", "Fire drill", "Warm puppy", "Doghouse"), 0),
        Puzzle("🌞 + 🌻", listOf("Yellow jacket", "Sunflower", "Daylight", "Flower shop"), 1),
        Puzzle("🧊 + 🍵", listOf("Cool beans", "Tea party", "Iced tea", "Cold front"), 2),
        Puzzle("🐝 + 🍯", listOf("Busy day", "Honey bee", "Sweet tooth", "Bee line"), 1)
    )
    private var puzzleIndex = 0
    private var attempts = 0
    private var score = 0
    private var streak = 0
    private var best = 0
    private var feedback = "Decode the emoji clue"

    override fun reset() {
        puzzleIndex = 0
        attempts = 0
        score = 0
        streak = 0
        best = 0
        feedback = "Decode the emoji clue"
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
        if (choiceIndex == puzzle.answerIndex) {
            score++
            streak++
            best = maxOf(best, streak)
            feedback = "Correct — ${puzzle.choices[puzzle.answerIndex]}"
        } else {
            streak = 0
            feedback = "Answer: ${puzzle.choices[puzzle.answerIndex]}"
        }
        puzzleIndex = (puzzleIndex + 1) % puzzles.size
        return true
    }

    override fun render(): GameViewState {
        val puzzle = puzzles[puzzleIndex]
        return GameViewState(
            title = title,
            subtitle = "Decode: ${puzzle.clue}",
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
