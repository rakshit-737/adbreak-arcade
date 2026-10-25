package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.Random

class QuickMathGame(private val random: Random = Random()) : ArcadeGame {
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
    var score = 0
        private set
    private var left = 0
    private var right = 0
    private var operation = "+"
    private var answer = 0
    private var prompt = ""
    private var choices = listOf<Int>()
    private var feedback = "Choose an answer"

    private val difficulty: Int
        get() = (1 + correct / 5).coerceAtMost(8)

    init { newQuestion() }

    private fun newQuestion() {
        val upper = 7 + difficulty * 4
        var a = random.nextInt(upper) + 2
        var b = random.nextInt(upper) + 2
        when (random.nextInt(4)) {
            0 -> {
                operation = "+"
                left = a
                right = b
                answer = a + b
            }
            1 -> {
                operation = "×"
                left = a
                right = b
                answer = a * b
            }
            2 -> {
                operation = "−"
                if (a < b) {
                    val temp = a
                    a = b
                    b = temp
                }
                left = a
                right = b
                answer = a - b
            }
            else -> {
                operation = "÷"
                val divisor = random.nextInt(3 + difficulty) + 2
                val quotient = random.nextInt(6 + difficulty) + 2
                left = divisor * quotient
                right = divisor
                answer = quotient
            }
        }
        prompt = "$left $operation $right"

        val distractors = linkedSetOf(answer)
        var offset = 1
        while (distractors.size < 4) {
            val candidate = answer + if (offset % 2 == 0) offset / 2 else -((offset + 1) / 2)
            if (candidate >= 0) distractors += candidate
            offset++
        }
        choices = distractors.shuffled(random)
    }

    override fun reset() {
        correct = 0
        incorrect = 0
        streak = 0
        score = 0
        feedback = "Choose an answer"
        newQuestion()
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
        if (choices[index] == answer) {
            correct++
            streak++
            best = maxOf(best, streak)
            score += difficulty
            feedback = "Correct — +$difficulty"
        } else {
            incorrect++
            streak = 0
            feedback = "Not quite — the answer was $answer"
        }
        newQuestion()
        return true
    }

    override fun render() = GameViewState(
        title,
        "What is $prompt?",
        "Score $score  ·  Correct $correct  ·  Streak $streak  ·  Best $best",
        choices.map { it.toString() },
        listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
        feedback = feedback
    )

    override fun encode() = JSONObject().apply {
        put("v", 2)
        put("correct", correct)
        put("incorrect", incorrect)
        put("streak", streak)
        put("best", best)
        put("score", score)
        put("left", left)
        put("right", right)
        put("operation", operation)
        put("answer", answer)
        put("prompt", prompt)
        put("choices", JSONArray(choices))
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..2) return false

        val restoredCorrect = json.optInt("correct", -1)
        val restoredIncorrect = json.optInt("incorrect", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredScore = json.optInt("score", restoredCorrect)
        val restoredAnswer = json.optInt("answer", Int.MIN_VALUE)
        val restoredPrompt = json.optString("prompt")
        val restoredChoices = if (version == 1) {
            json.optString("choices").split(",").map { it.toIntOrNull() ?: return false }
        } else {
            val array = json.optJSONArray("choices") ?: return false
            List(array.length()) { array.optInt(it, Int.MIN_VALUE) }
        }

        val parsed = Regex("(-?\\d+)\\s*([+×−÷])\\s*(-?\\d+)").matchEntire(restoredPrompt) ?: return false
        val parsedLeft = parsed.groupValues[1].toIntOrNull() ?: return false
        val parsedOperation = parsed.groupValues[2]
        val parsedRight = parsed.groupValues[3].toIntOrNull() ?: return false
        val expectedAnswer = when (parsedOperation) {
            "+" -> parsedLeft + parsedRight
            "×" -> parsedLeft * parsedRight
            "−" -> parsedLeft - parsedRight
            "÷" -> if (parsedRight != 0 && parsedLeft % parsedRight == 0) parsedLeft / parsedRight else return false
            else -> return false
        }

        if (restoredCorrect !in 0..1_000_000 || restoredIncorrect !in 0..1_000_000 ||
            restoredStreak !in 0..1_000_000 || restoredBest !in restoredStreak..1_000_000 ||
            restoredScore !in restoredCorrect..8_000_000 || restoredChoices.size != 4 ||
            restoredChoices.toSet().size != 4 || restoredAnswer !in restoredChoices ||
            restoredAnswer != expectedAnswer || restoredPrompt.isBlank()) return false

        if (version == 2) {
            val restoredLeft = json.optInt("left", Int.MIN_VALUE)
            val restoredRight = json.optInt("right", Int.MIN_VALUE)
            val restoredOperation = json.optString("operation")
            if (restoredLeft != parsedLeft || restoredRight != parsedRight || restoredOperation != parsedOperation) return false
        }
        val restoredFeedback = if (version == 1) "Choose an answer" else json.optString("feedback")
        if (restoredFeedback.isBlank()) return false

        correct = restoredCorrect
        incorrect = restoredIncorrect
        streak = restoredStreak
        best = restoredBest
        score = restoredScore
        left = parsedLeft
        right = parsedRight
        operation = parsedOperation
        answer = restoredAnswer
        prompt = restoredPrompt
        choices = restoredChoices
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
