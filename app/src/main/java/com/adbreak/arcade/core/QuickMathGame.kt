package com.adbreak.arcade.core

import org.json.JSONObject
import java.util.Random

class QuickMathGame(private val random: Random = Random(7)) : ArcadeGame {
    override val id = "quick_math"
    override val title = "Quick Math"
    override val description = "Solve compact arithmetic prompts before the next scene."

    var correct = 0
        private set
    var incorrect = 0
        private set
    var streak = 0
        private set
    var best = 0
        private set
    private var answer = 0
    private var prompt = ""
    private var choices = listOf<Int>()

    init { newQuestion() }

    private fun newQuestion() {
        val a = random.nextInt(9) + 2
        val b = random.nextInt(8) + 2
        when (random.nextInt(4)) {
            0 -> { answer = a + b; prompt = "$a + $b" }
            1 -> { answer = a * b; prompt = "$a × $b" }
            2 -> { answer = a - b; prompt = "$a − $b" }
            else -> {
                val divisor = random.nextInt(7) + 2
                val quotient = random.nextInt(8) + 2
                answer = quotient
                prompt = "${divisor * quotient} ÷ $divisor"
            }
        }
        val distractors = linkedSetOf(answer)
        while (distractors.size < 4) distractors += answer + random.nextInt(11) - 5
        choices = distractors.shuffled(random)
    }

    override fun reset() { correct = 0; incorrect = 0; streak = 0; newQuestion() }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) { reset(); return true }
        val index = when (action) {
            GameAction.PRIMARY -> 0
            GameAction.SECONDARY -> 1
            GameAction.CELL_1 -> 2
            GameAction.CELL_2 -> 3
            else -> -1
        }
        if (index !in choices.indices) return false
        if (choices[index] == answer) { correct++; streak++; best = maxOf(best, streak) }
        else { incorrect++; streak = 0 }
        newQuestion()
        return true
    }

    override fun render() = GameViewState(
        title,
        "What is $prompt?",
        "Correct $correct  ·  Streak $streak  ·  Best $best",
        choices.map { it.toString() },
        listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2)
    )

    override fun encode() = JSONObject().apply {
        put("v", 1); put("correct", correct); put("incorrect", incorrect)
        put("streak", streak); put("best", best); put("answer", answer)
        put("prompt", prompt); put("choices", choices.joinToString(","))
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != 1) return false
        val restoredCorrect = json.optInt("correct", -1)
        val restoredIncorrect = json.optInt("incorrect", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredAnswer = json.optInt("answer", Int.MIN_VALUE)
        val restoredPrompt = json.optString("prompt")
        val restoredChoices = json.optString("choices").split(",").map { it.toIntOrNull() ?: return false }
        if (restoredCorrect < 0 || restoredIncorrect < 0 || restoredStreak < 0 ||
            restoredBest < restoredStreak || restoredChoices.size != 4 ||
            restoredChoices.toSet().size != 4 || restoredAnswer !in restoredChoices ||
            restoredPrompt.isBlank()) return false
        correct = restoredCorrect; incorrect = restoredIncorrect; streak = restoredStreak
        best = restoredBest; answer = restoredAnswer; prompt = restoredPrompt
        choices = restoredChoices
        true
    }.getOrDefault(false)
}
