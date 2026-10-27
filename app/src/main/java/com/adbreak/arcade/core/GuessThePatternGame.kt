package com.adbreak.arcade.core

import org.json.JSONObject

class GuessThePatternGame : ArcadeGame {
    override val id = "guess_the_pattern"
    override val title = "Guess the Pattern"
    override val description = "Spot the rule in a short sequence and choose what comes next."

    private data class Puzzle(val prompt: String, val choices: List<String>, val answerIndex: Int, val rule: String)

    private val puzzles = listOf(
        Puzzle("2, 4, 6, ? · same step", listOf("7", "8", "9", "10"), 1, "Add 2 each time: 6 + 2 = 8"),
        Puzzle("A, C, E, ? · same step", listOf("F", "G", "H", "I"), 1, "Skip one letter each time: E, (F), G"),
        Puzzle("▲, ▲▲, ▲▲▲, ?", listOf("▲", "▲▲", "▲▲▲▲", "●"), 2, "Add one triangle: next has four"),
        Puzzle("1, 1, 2, 3, ? · sum previous two", listOf("4", "5", "6", "7"), 1, "Add the previous two: 2 + 3 = 5")
    )
    private var puzzleIndex = 0
    private var startIndex = 0
    private var bounded = true
    private var attempts = 0
    private var score = 0
    private var streak = 0
    private var best = 0
    private var feedback = "Find the next item"

    override fun reset() {
        startIndex = (startIndex + 1) % puzzles.size
        puzzleIndex = startIndex
        bounded = true
        attempts = 0
        score = 0
        streak = 0
        best = 0
        feedback = "Find the next item"
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
            feedback = "Correct — ${puzzle.rule}"
        } else {
            streak = 0
            feedback = "Not quite — ${puzzle.rule}"
        }
        puzzleIndex = (puzzleIndex + 1) % puzzles.size
        return true
    }

    private fun isOver() = bounded && attempts >= puzzles.size

    override fun render(): GameViewState {
        val puzzle = puzzles[puzzleIndex]
        return GameViewState(
            title = title,
            subtitle = if (isOver()) "Deck complete · $score/${puzzles.size} correct" else puzzle.prompt,
            scoreLine = "Correct $score/$attempts  ·  Streak $streak",
            buttons = if (isOver()) listOf("NEW DECK") else puzzle.choices.drop(startIndex % 4) + puzzle.choices.take(startIndex % 4),
            buttonActions = if (isOver()) listOf(GameAction.NEW_GAME) else listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
            feedback = feedback,
            isTerminal = isOver(),
            phase = if (!isOver()) GamePhase.PLAYING else if (score == puzzles.size) GamePhase.WON else GamePhase.DRAW,
            progress = if (bounded) GameProgress(attempts, puzzles.size, "Patterns answered") else null,
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
