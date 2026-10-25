package com.adbreak.arcade.core

import org.json.JSONObject
import java.util.Random

class WordScrambleGame(private val random: Random = Random(4)) : ArcadeGame {
    override val id = "word_scramble"
    override val title = "Word Scramble"
    override val description = "Pick the word that unlocks the scrambled letters."

    private val words = listOf("orbit", "violet", "screen", "arcade", "puzzle", "signal", "planet", "button", "stream", "rocket")
    private var word = "orbit"
    private var scramble = "tibor"
    private var choices = listOf<String>()
    var score = 0
        private set
    var streak = 0
        private set
    var best = 0
        private set

    init { newWord() }

    private fun newWord() {
        word = words[random.nextInt(words.size)]
        scramble = word.toList().shuffled(random).joinToString("").let { if (it == word) it.reversed() else it }
        val answerIndex = words.indexOf(word)
        choices = listOf(word, words[(answerIndex + 1) % words.size], words[(answerIndex + 2) % words.size], words[(answerIndex + 3) % words.size]).shuffled(random)
    }

    override fun reset() { score = 0; streak = 0; newWord() }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) { reset(); return true }
        val index = when (action) { GameAction.PRIMARY -> 0; GameAction.SECONDARY -> 1; GameAction.CELL_1 -> 2; GameAction.CELL_2 -> 3; else -> -1 }
        if (index !in choices.indices) return false
        if (choices[index] == word) { score++; streak++; best = maxOf(best, streak) } else streak = 0
        newWord()
        return true
    }

    override fun render() = GameViewState(
        title,
        "Unscramble: $scramble",
        "Score $score  ·  Streak $streak  ·  Best $best",
        choices,
        listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2)
    )

    override fun encode() = JSONObject().apply {
        put("v", 1); put("word", word); put("scramble", scramble); put("choices", choices.joinToString("|"))
        put("score", score); put("streak", streak); put("best", best)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val j = JSONObject(encoded)
        if (j.optInt("v", -1) != 1) return false
        val restoredWord = j.optString("word"); val restoredScramble = j.optString("scramble")
        val restoredChoices = j.optString("choices").split("|")
        val restoredScore = j.optInt("score", -1); val restoredStreak = j.optInt("streak", -1); val restoredBest = j.optInt("best", -1)
        if (restoredWord !in words || restoredScramble.isBlank() || restoredChoices.size != 4 || restoredChoices.toSet().size != 4 || restoredWord !in restoredChoices || restoredScore < 0 || restoredStreak < 0 || restoredBest < restoredStreak) return false
        word = restoredWord; scramble = restoredScramble; choices = restoredChoices; score = restoredScore; streak = restoredStreak; best = restoredBest
        true
    }.getOrDefault(false)
}
