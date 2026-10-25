package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.Random

class WordScrambleGame(private val random: Random = Random()) : ArcadeGame {
    override val id = "word_scramble"
    override val title = "Word Scramble"
    override val description = "Pick the word that unlocks the scrambled letters."

    private val words = listOf(
        "orbit", "violet", "screen", "arcade", "puzzle", "signal", "planet", "button", "stream", "rocket",
        "pixel", "focus", "shadow", "player"
    )
    private var word = "orbit"
    private var scramble = "tibor"
    private var choices = listOf<String>()
    var score = 0
        private set
    var streak = 0
        private set
    var best = 0
        private set
    private var incorrect = 0
    private var feedback = "Unscramble the letters"

    private val difficulty: Int
        get() = (1 + score / 3).coerceAtMost(5)

    init { newWord() }

    private fun newWord() {
        val eligible = words.filter { it.length >= 4 + difficulty.coerceAtMost(2) }
        word = (eligible.ifEmpty { words })[random.nextInt((eligible.ifEmpty { words }).size)]
        var shuffled = word.toList().shuffled(random).joinToString("")
        if (shuffled == word) shuffled = word.reversed()
        scramble = shuffled

        val distractors = words.filter { it != word }.sortedWith(compareBy<String> { kotlin.math.abs(it.length - word.length) }.thenBy { it })
        choices = (listOf(word) + distractors.take(3)).shuffled(random)
    }

    override fun reset() {
        score = 0
        streak = 0
        incorrect = 0
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
        if (choices[index] == word) {
            score++
            streak++
            best = maxOf(best, streak)
            feedback = "Correct — keep the streak alive"
        } else {
            incorrect++
            streak = 0
            feedback = "Not quite — the word was $word"
        }
        newWord()
        return true
    }

    override fun render() = GameViewState(
        title,
        "Unscramble: $scramble",
        "Score $score  ·  Streak $streak  ·  Best $best",
        choices,
        listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
        feedback = feedback
    )

    override fun encode() = JSONObject().apply {
        put("v", 2)
        put("word", word)
        put("scramble", scramble)
        put("choices", JSONArray(choices))
        put("score", score)
        put("streak", streak)
        put("best", best)
        put("incorrect", incorrect)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..2) return false

        val restoredWord = json.optString("word")
        val restoredScramble = json.optString("scramble")
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
        val restoredFeedback = if (version == 1) "Unscramble the letters" else json.optString("feedback")

        if (restoredWord !in words || restoredScramble.isBlank() || restoredScramble == restoredWord ||
            restoredScramble.toList().sorted() != restoredWord.toList().sorted() || restoredChoices.size != 4 ||
            restoredChoices.toSet().size != 4 || restoredChoices.any { it !in words } || restoredWord !in restoredChoices ||
            restoredScore !in 0..1_000_000 || restoredStreak !in 0..1_000_000 ||
            restoredBest !in restoredStreak..1_000_000 || restoredIncorrect !in 0..1_000_000 ||
            restoredFeedback.isBlank()) return false

        word = restoredWord
        scramble = restoredScramble
        choices = restoredChoices
        score = restoredScore
        streak = restoredStreak
        best = restoredBest
        incorrect = restoredIncorrect
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
