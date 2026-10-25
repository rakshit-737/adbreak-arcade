package com.adbreak.arcade.core

import org.json.JSONObject
import java.util.LinkedHashSet
import java.util.Random

class NumberSequenceGame(private val random: Random = Random()) : ArcadeGame {
    override val id = "number_sequence"
    override val title = "Number Sequence"
    override val description = "Spot the rule and choose the missing number."

    private var start = 2
    private var step = 3
    private var answer = 11
    private var geometric = false
    private var sequence = "2, 5, 8, ?, 14"
    var score = 0
        private set
    var streak = 0
        private set
    var best = 0
        private set
    private var incorrect = 0
    private var feedback = "Choose the missing term"

    private val difficulty: Int
        get() = (1 + score / 4).coerceAtMost(8)

    init { newQuestion() }

    private fun newQuestion() {
        val level = difficulty
        geometric = random.nextBoolean()
        if (geometric) {
            start = random.nextInt(level + 2) + 1
            step = random.nextInt(level.coerceAtMost(3) + 2) + 2
            answer = start * step * step * step
            sequence = "$start, ${start * step}, ${start * step * step}, ?, ${start * step * step * step * step}"
        } else {
            start = random.nextInt(level * 2 + 6) + 1
            step = random.nextInt(level + 3) + 2
            answer = start + step * 3
            sequence = "$start, ${start + step}, ${start + step * 2}, ?, ${start + step * 4}"
        }
    }

    private fun choices(): List<Int> {
        val candidates = if (geometric) {
            listOf(answer, answer + step, answer / step, answer + 1, answer + step * 2)
        } else {
            listOf(answer, answer + 1, answer - step, answer + step, answer - 1)
        }
        return LinkedHashSet(candidates).toList().take(4)
    }

    override fun reset() {
        score = 0
        streak = 0
        incorrect = 0
        feedback = "Choose the missing term"
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
        val options = choices()
        if (index !in options.indices) return false
        if (options[index] == answer) {
            score++
            streak++
            best = maxOf(best, streak)
            feedback = "Correct — the pattern continues"
        } else {
            incorrect++
            streak = 0
            feedback = "Not quite — the missing term was $answer"
        }
        newQuestion()
        return true
    }

    override fun render() = GameViewState(
        title,
        sequence,
        "Score $score  ·  Streak $streak  ·  Best $best",
        choices().map { it.toString() },
        listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
        feedback = feedback
    )

    override fun encode() = JSONObject().apply {
        put("v", 2)
        put("start", start)
        put("step", step)
        put("answer", answer)
        put("geometric", geometric)
        put("sequence", sequence)
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

        val restoredStart = json.optInt("start", -1)
        val restoredStep = json.optInt("step", -1)
        val restoredAnswer = json.optInt("answer", -1)
        val restoredGeometric = json.optBoolean("geometric")
        val restoredSequence = json.optString("sequence")
        val restoredScore = json.optInt("score", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredIncorrect = json.optInt("incorrect", 0)
        val restoredFeedback = if (version == 1) "Choose the missing term" else json.optString("feedback")
        if (restoredStart < 1 || restoredStep < 2 || restoredAnswer < 1 || restoredSequence.isBlank() ||
            restoredScore !in 0..1_000_000 || restoredStreak !in 0..1_000_000 ||
            restoredBest !in restoredStreak..1_000_000 || restoredIncorrect !in 0..1_000_000 ||
            restoredFeedback.isBlank()) return false

        val expectedSequence = if (restoredGeometric) {
            "$restoredStart, ${restoredStart * restoredStep}, ${restoredStart * restoredStep * restoredStep}, ?, ${restoredStart * restoredStep * restoredStep * restoredStep * restoredStep}"
        } else {
            "$restoredStart, ${restoredStart + restoredStep}, ${restoredStart + restoredStep * 2}, ?, ${restoredStart + restoredStep * 4}"
        }
        val expectedAnswer = if (restoredGeometric) {
            restoredStart * restoredStep * restoredStep * restoredStep
        } else {
            restoredStart + restoredStep * 3
        }
        if (restoredSequence != expectedSequence || restoredAnswer != expectedAnswer) return false
        val restoredChoices = if (restoredGeometric) {
            LinkedHashSet(listOf(restoredAnswer, restoredAnswer + restoredStep, restoredAnswer / restoredStep, restoredAnswer + 1, restoredAnswer + restoredStep * 2)).size
        } else {
            LinkedHashSet(listOf(restoredAnswer, restoredAnswer + 1, restoredAnswer - restoredStep, restoredAnswer + restoredStep, restoredAnswer - 1)).size
        }
        if (restoredChoices < 4) return false

        start = restoredStart
        step = restoredStep
        answer = restoredAnswer
        geometric = restoredGeometric
        sequence = restoredSequence
        score = restoredScore
        streak = restoredStreak
        best = restoredBest
        incorrect = restoredIncorrect
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
