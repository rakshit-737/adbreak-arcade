package com.adbreak.arcade.core

import org.json.JSONObject

class EmojiDecodeGame : ArcadeGame {
    override val id = "emoji_decode"
    override val title = "Emoji Decode"
    override val description = "Translate a tiny emoji clue into the phrase it represents."

    private data class Puzzle(val clue: String, val choices: List<String>, val answerIndex: Int, val explanation: String)

    private val puzzles = listOf(
        Puzzle("🔥 + 🐶", listOf("Hot dog", "Fire drill", "Doghouse", "Raincoat"), 0, "Hot + dog = Hot dog"),
        Puzzle("🌞 + 🌼", listOf("Yellow jacket", "Sunflower", "Daylight", "Flower shop"), 1, "Sun + flower = Sunflower"),
        Puzzle("🧊 + 🍵", listOf("Cool beans", "Tea party", "Iced tea", "Cold front"), 2, "Ice + tea = Iced tea"),
        Puzzle("🍯 + 🐝", listOf("Busy day", "Honey bee", "Sweet tooth", "Bee line"), 1, "Honey + bee = Honey bee")
    )
    private var puzzleIndex = 0
    private var startIndex = 0
    private var bounded = true
    private var attempts = 0
    private var score = 0
    private var streak = 0
    private var best = 0
    private var feedback = "Decode the emoji clue"

    override fun reset() {
        startIndex = (startIndex + 1) % puzzles.size
        puzzleIndex = startIndex
        bounded = true
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
        if (isOver()) return false
        val choiceIndex = when (action) {
            GameAction.PRIMARY -> 0
            GameAction.SECONDARY -> 1
            GameAction.CELL_1 -> 2
            GameAction.CELL_2 -> 3
            else -> return false
        }
        val puzzle = puzzles[puzzleIndex]
        attempts++
        if ((choiceIndex + startIndex) % 4 == puzzle.answerIndex) {
            score++
            streak++
            best = maxOf(best, streak)
            feedback = "Correct — ${puzzle.explanation}"
        } else {
            streak = 0
            feedback = "Not quite — ${puzzle.explanation}"
        }
        puzzleIndex = (puzzleIndex + 1) % puzzles.size
        return true
    }

    private fun isOver() = bounded && attempts >= puzzles.size

    override fun render(): GameViewState {
        val puzzle = puzzles[puzzleIndex]
        return GameViewState(
            title = title,
            subtitle = if (isOver()) "Deck complete · $score/${puzzles.size} correct" else "Read left to right: ${puzzle.clue}",
            scoreLine = "Correct $score/$attempts  ·  Streak $streak",
            buttons = if (isOver()) listOf("NEW DECK") else puzzle.choices.drop(startIndex % 4) + puzzle.choices.take(startIndex % 4),
            buttonActions = if (isOver()) listOf(GameAction.NEW_GAME) else listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
            feedback = feedback,
            isTerminal = isOver(),
            phase = if (!isOver()) GamePhase.PLAYING else if (score == puzzles.size) GamePhase.WON else GamePhase.DRAW,
            progress = if (bounded) GameProgress(attempts, puzzles.size, "Clues answered") else null,
            record = if (isOver()) GameRecord(score, "Correct / ${puzzles.size}") else null
        )
    }

    override fun encode() = JSONObject().apply {
        put("v", 1)
        put("puzzleIndex", puzzleIndex)
        put("startIndex", startIndex)
        put("bounded", bounded)
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
        val restoredStart = json.optInt("startIndex", 0)
        val restoredBounded = json.optBoolean("bounded", false)
        if (restoredPuzzleIndex !in puzzles.indices || restoredAttempts < 0 ||
            restoredStart !in puzzles.indices ||
            restoredPuzzleIndex != (restoredStart + restoredAttempts % puzzles.size) % puzzles.size ||
            (restoredBounded && restoredAttempts > puzzles.size) ||
            restoredScore < 0 || restoredScore > restoredAttempts || restoredStreak < 0 ||
            restoredStreak > restoredScore || restoredBest < restoredStreak ||
            restoredBest > restoredScore || restoredFeedback.isBlank()
        ) return false
        puzzleIndex = restoredPuzzleIndex
        startIndex = restoredStart
        bounded = restoredBounded
        attempts = restoredAttempts
        score = restoredScore
        streak = restoredStreak
        best = restoredBest
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
