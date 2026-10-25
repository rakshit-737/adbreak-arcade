package com.adbreak.arcade.core

import org.json.JSONObject

class AnagramChallengeGame : ArcadeGame {
    override val id = "anagram_challenge"
    override val title = "Anagram Challenge"
    override val description = "Build the hidden word one letter at a time from a compact anagram clue."

    private data class Puzzle(val word: String, val scramble: String)

    private val puzzles = listOf(
        Puzzle("SPARK", "KSRAP"),
        Puzzle("FRAME", "MEARF"),
        Puzzle("LIGHT", "GHTLI"),
        Puzzle("OCEAN", "NCEAO"),
        Puzzle("BRISK", "KSBIR")
    )
    private var puzzleIndex = 0
    private var progress = ""
    private var attempts = 0
    private var score = 0
    private var streak = 0
    private var best = 0
    private var feedback = "Choose the first letter"

    override fun reset() {
        puzzleIndex = 0
        progress = ""
        attempts = 0
        score = 0
        streak = 0
        best = 0
        feedback = "Choose the first letter"
    }

    private fun choices(): List<String> {
        val word = puzzles[puzzleIndex].word
        val correct = word[progress.length]
        val letters = mutableListOf(correct.toString())
        var cursor = (puzzleIndex * 7 + progress.length * 3) % 26
        while (letters.size < 4) {
            val candidate = ('A'.code + cursor % 26).toChar()
            if (candidate != correct && candidate.toString() !in letters) letters += candidate.toString()
            cursor++
        }
        val rotation = (puzzleIndex + progress.length) % letters.size
        return letters.drop(rotation) + letters.take(rotation)
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
        val selected = choices()[choiceIndex][0]
        val word = puzzles[puzzleIndex].word
        attempts++
        if (selected == word[progress.length]) {
            progress += selected
            if (progress == word) {
                score++
                streak++
                best = maxOf(best, streak)
                feedback = "Solved $word"
                puzzleIndex = (puzzleIndex + 1) % puzzles.size
                progress = ""
            } else {
                feedback = "Built $progress"
            }
        } else {
            streak = 0
            progress = ""
            feedback = "Reset — start the word again"
        }
        return true
    }

    override fun render(): GameViewState {
        val puzzle = puzzles[puzzleIndex]
        return GameViewState(
            title = title,
            subtitle = "Anagram: ${puzzle.scramble}  ·  Built: ${progress.ifBlank { "—" }}",
            scoreLine = "Score $score  ·  Streak $streak  ·  Best $best",
            buttons = choices(),
            buttonActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
            feedback = feedback
        )
    }

    override fun encode() = JSONObject().apply {
        put("v", 1)
        put("puzzleIndex", puzzleIndex)
        put("progress", progress)
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
        val restoredProgress = json.optString("progress")
        val restoredAttempts = json.optInt("attempts", -1)
        val restoredScore = json.optInt("score", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredFeedback = json.optString("feedback")
        val word = puzzles.getOrNull(restoredPuzzleIndex)?.word
        if (word == null || restoredPuzzleIndex != restoredScore % puzzles.size ||
            restoredProgress.length >= word.length ||
            restoredProgress.any { it !in 'A'..'Z' } || !word.startsWith(restoredProgress) ||
            restoredAttempts < 0 || restoredScore < 0 || restoredScore > restoredAttempts ||
            restoredStreak < 0 || restoredStreak > restoredScore || restoredBest < restoredStreak ||
            restoredBest > restoredScore || restoredFeedback.isBlank()
        ) return false
        puzzleIndex = restoredPuzzleIndex
        progress = restoredProgress
        attempts = restoredAttempts
        score = restoredScore
        streak = restoredStreak
        best = restoredBest
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
