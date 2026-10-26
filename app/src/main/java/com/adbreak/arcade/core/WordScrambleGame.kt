package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.Random

class WordScrambleGame(private val random: Random = Random()) : ArcadeGame {
    override val id = "word_scramble"
    override val title = "Word Scramble"
    override val description = "Solve a clean, curated anagram and grow a vocabulary streak."

    private val words = listOf(
        "orbit", "violet", "screen", "arcade", "puzzle", "signal", "planet", "button", "stream", "rocket",
        "pixel", "focus", "shadow", "player", "level", "letter", "coffee", "garden", "bridge", "bright",
        "memory", "number", "reason", "travel", "window", "winter", "simple", "clever", "future", "rhythm"
    )
    private var word = "orbit"
    private var scramble = "tibor"
    private var lastWord = ""
    private var choices = listOf<String>()
    var score = 0
        private set
    var streak = 0
        private set
    var best = 0
        private set
    private var incorrect = 0
    private var solved = 0
    private var attempts = 0
    private var feedback = "Unscramble the letters"

    private val difficulty: Int
        get() = (1 + solved / 3).coerceAtMost(5)

    private val accuracyPercent: Int
        get() = if (attempts == 0) 0 else solved * 100 / attempts

    init { newWord() }

    private fun newWord() {
        val eligible = words.filter { it.length >= 4 + difficulty.coerceAtMost(2) }
        val pool = eligible.ifEmpty { words }
        val withoutPrevious = pool.filter { it != word }
        lastWord = word
        word = (withoutPrevious.ifEmpty { pool })[random.nextInt(withoutPrevious.ifEmpty { pool }.size)]
        scramble = scramble(word)

        val distractors = words.filter { it != word }
            .sortedWith(compareBy<String> { kotlin.math.abs(it.length - word.length) }.thenBy { it })
        choices = (listOf(word) + distractors.take(3)).distinct().shuffled(random)
    }

    private fun scramble(value: String): String {
        if (value.length < 2 || value.toSet().size == 1) return value
        var result = value.toList().shuffled(random).joinToString("")
        if (result == value) result = value.drop(1) + value.first()
        return result
    }

    override fun reset() {
        score = 0
        streak = 0
        incorrect = 0
        solved = 0
        attempts = 0
        feedback = "Unscramble the letters"
        newWord()
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        val index = when (action) {
            GameAction.PRIMARY -> 0
            GameAction.SECONDARY -> 1
            GameAction.CELL_1 -> 2
            GameAction.CELL_2 -> 3
            else -> -1
        }
        if (index !in choices.indices) return false
        attempts++
        if (choices[index] == word) {
            val points = difficulty
            score += points
            solved++
            streak++
            best = maxOf(best, streak)
            feedback = "Correct — $word · +$points"
        } else {
            incorrect++
            streak = 0
            feedback = "Not quite — the word was $word"
        }
        newWord()
        return true
    }

    override fun render() = GameViewState(
        title = title,
        subtitle = "Unscramble: $scramble",
        scoreLine = "Level $difficulty  ·  Score $score  ·  Solved $solved  ·  Accuracy $accuracyPercent%  ·  Streak $streak  ·  Best $best",
        buttons = choices,
        buttonActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
        feedback = feedback,
    )

    override fun encode() = JSONObject().apply {
        put("v", 3)
        put("word", word)
        put("scramble", scramble)
        put("lastWord", lastWord)
        put("choices", JSONArray(choices))
        put("score", score)
        put("streak", streak)
        put("best", best)
        put("incorrect", incorrect)
        put("solved", solved)
        put("attempts", attempts)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..3) return false

        val restoredWord = json.optString("word")
        val restoredScramble = json.optString("scramble")
        val restoredLastWord = if (version >= 3) json.optString("lastWord") else ""
        val restoredChoices = if (version == 1) {
            json.optString("choices").split("|")
        } else {
            val array = json.optJSONArray("choices") ?: return false
            List(array.length()) { array.optString(it) }
        }
        val restoredScore = json.optInt("score", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredIncorrect = json.optInt("incorrect", 0)
        val restoredSolved = if (version >= 3) json.optInt("solved", restoredScore) else restoredScore
        val restoredAttempts = if (version >= 3) json.optInt("attempts", restoredSolved + restoredIncorrect) else restoredSolved + restoredIncorrect
        val restoredFeedback = if (version == 1) "Unscramble the letters" else json.optString("feedback")

        if (restoredWord !in words || restoredScramble.isBlank() || restoredScramble == restoredWord ||
            restoredScramble.toList().sorted() != restoredWord.toList().sorted() || restoredChoices.size != 4 ||
            restoredChoices.toSet().size != 4 || restoredChoices.any { it !in words } || restoredWord !in restoredChoices ||
            restoredScore !in 0..1_000_000 || restoredStreak !in 0..1_000_000 ||
            restoredBest !in restoredStreak..1_000_000 || restoredIncorrect !in 0..1_000_000 ||
            restoredSolved !in 0..1_000_000 || restoredAttempts !in restoredSolved..1_000_000 ||
            restoredFeedback.isBlank()) return false

        word = restoredWord
        scramble = restoredScramble
        lastWord = restoredLastWord
        choices = restoredChoices
        score = restoredScore
        streak = restoredStreak
        best = restoredBest
        incorrect = restoredIncorrect
        solved = restoredSolved
        attempts = restoredAttempts
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
