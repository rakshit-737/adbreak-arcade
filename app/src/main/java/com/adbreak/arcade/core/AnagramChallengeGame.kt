package com.adbreak.arcade.core

import org.json.JSONObject

class AnagramChallengeGame : ArcadeGame {
    override val id = "anagram_challenge"
    override val title = "Anagram Challenge"
    override val description = "Build the hidden word one letter at a time from a compact anagram clue."

    private data class Puzzle(val word: String, val scramble: String, val clue: String)

    private val puzzles = listOf(
        Puzzle("SPARK", "KSRAP", "A tiny flash from a fire"),
        Puzzle("FRAME", "MEARF", "A border around a picture"),
        Puzzle("LIGHT", "GHTLI", "The opposite of dark"),
        Puzzle("OCEAN", "NCEAO", "A vast body of salt water"),
        Puzzle("BRISK", "KSBIR", "Quick and energetic")
    )
    private var puzzleIndex = 0
    private var progress = ""
    private var attempts = 0
    private var score = 0
    private var streak = 0
    private var best = 0
    private var mistakes = 0
    private var bounded = true
    private var feedback = "Build the word that fits the clue · 3 mistakes end the round"

    override fun reset() {
        puzzleIndex = (puzzleIndex + 1) % puzzles.size
        progress = ""
        attempts = 0
        score = 0
        streak = 0
        best = 0
        mistakes = 0
        bounded = true
        feedback = "Build the word that fits the clue · 3 mistakes end the round"
    }

    private fun choices(): List<String> {
        val word = puzzles[puzzleIndex].word
        val correct = word[progress.length]
        // Distractors also come from the unused letters, so the clue matters.
        val letters = (listOf(correct) + word.drop(progress.length).filter { it != correct }.take(3).toList()).map { it.toString() }
        val rotation = (puzzleIndex + progress.length) % letters.size
        return letters.drop(rotation) + letters.take(rotation)
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
        val selected = choices().getOrNull(choiceIndex)?.first() ?: return false
        val word = puzzles[puzzleIndex].word
        attempts++
        if (selected == word[progress.length]) {
            progress += selected
            if (progress == word) {
                score++
                streak++
                best = maxOf(best, streak)
                feedback = "Solved $word · $mistakes mistakes in $attempts taps"
                if (!bounded) {
                    puzzleIndex = (puzzleIndex + 1) % puzzles.size
                    progress = ""
                }
            } else {
                feedback = "Built $progress"
            }
        } else {
            streak = 0
            mistakes++
            feedback = if (isOver()) "Answer: $word — ${puzzles[puzzleIndex].clue}" else "$selected is not next · ${if (bounded) "${3 - mistakes} mistakes left · " else ""}built letters kept"
        }
        return true
    }

    private fun isOver() = bounded && (progress == puzzles[puzzleIndex].word || mistakes >= 3)

    override fun render(): GameViewState {
        val puzzle = puzzles[puzzleIndex]
        val actions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2)
        val offered = if (isOver()) listOf("NEW WORD") else choices()
        return GameViewState(
            title = title,
            subtitle = "${puzzle.scramble} · ${puzzle.clue}",
            scoreLine = "Built: ${progress.padEnd(puzzle.word.length, '_')}  ·  Mistakes $mistakes${if (bounded) "/3" else ""}",
            buttons = offered,
            buttonActions = if (isOver()) listOf(GameAction.NEW_GAME) else actions.take(offered.size),
            feedback = feedback,
            isTerminal = isOver(),
            phase = if (!isOver()) GamePhase.PLAYING else if (progress == puzzle.word) GamePhase.WON else GamePhase.LOST,
            progress = GameProgress(progress.length, puzzle.word.length, "Letters built"),
            record = if (isOver() && progress == puzzle.word) GameRecord(mistakes, "Mistakes / five-letter word", higherIsBetter = false) else null
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
        put("mistakes", mistakes)
        put("bounded", bounded)
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
        val restoredMistakes = json.optInt("mistakes", 0)
        val restoredBounded = json.optBoolean("bounded", false)
        val word = puzzles.getOrNull(restoredPuzzleIndex)?.word
        if (word == null || (!restoredBounded && restoredPuzzleIndex != restoredScore % puzzles.size) ||
            restoredProgress.length > word.length || (!restoredBounded && restoredProgress.length == word.length) ||
            restoredProgress.any { it !in 'A'..'Z' } || !word.startsWith(restoredProgress) ||
            restoredAttempts < 0 || restoredScore < 0 || restoredScore > restoredAttempts ||
            restoredStreak < 0 || restoredStreak > restoredScore || restoredBest < restoredStreak ||
            restoredBest > restoredScore || restoredFeedback.isBlank() || restoredMistakes < 0 ||
            (restoredBounded && (restoredMistakes > 3 || restoredScore !in 0..1 ||
                (restoredScore == 1) != (restoredProgress == word) ||
                (restoredScore == 1 && restoredMistakes == 3) ||
                restoredAttempts != restoredProgress.length + restoredMistakes))
        ) return false
        puzzleIndex = restoredPuzzleIndex
        progress = restoredProgress
        attempts = restoredAttempts
        score = restoredScore
        streak = restoredStreak
        best = restoredBest
        mistakes = restoredMistakes
        bounded = restoredBounded
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
