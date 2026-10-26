package com.adbreak.arcade.core

import org.json.JSONObject
import java.util.LinkedHashSet
import java.util.Random

class NumberSequenceGame(private val random: Random = Random()) : ArcadeGame {
    override val id = "number_sequence"
    override val title = "Number Sequence"
    override val description = "Spot an arithmetic, geometric, alternating, square, or mixed rule."

    private var start = 2
    private var step = 3
    private var secondStep = 2
    private var answer = 11
    private var geometric = false
    private var pattern = "arithmetic"
    private var rule = "Add the same amount"
    private var sequence = "2, 5, 8, ?, 14"
    var score = 0
        private set
    var streak = 0
        private set
    var best = 0
        private set
    private var incorrect = 0
    private var solved = 0
    private var feedback = "Choose the missing term"

    private val difficulty: Int
        get() = (1 + solved / 4).coerceAtMost(8)

    private val accuracyPercent: Int
        get() = if (solved + incorrect == 0) 0 else solved * 100 / (solved + incorrect)

    init { newQuestion() }

    private fun newQuestion() {
        val level = difficulty
        geometric = random.nextBoolean()
        pattern = if (geometric) {
            "geometric"
        } else {
            when (random.nextInt(4)) {
                0 -> "arithmetic"
                1 -> "alternating"
                2 -> "squares"
                else -> "mixed"
            }
        }
        start = when (pattern) {
            "geometric" -> random.nextInt(3) + 1
            "squares" -> random.nextInt(4) + 1
            else -> random.nextInt(level * 2 + 6) + 1
        }
        step = when (pattern) {
            "geometric" -> random.nextInt(level.coerceAtMost(3) + 2) + 2
            "squares" -> 1
            else -> random.nextInt(level + 3) + 2
        }
        secondStep = if (pattern == "alternating") {
            val candidate = random.nextInt(level + 3) + 1
            if (candidate == step) if (candidate == 1) 2 else candidate - 1 else candidate
        } else 2
        val data = patternData(pattern, start, step, secondStep) ?: run {
            pattern = "arithmetic"
            geometric = false
            patternData(pattern, start, step, secondStep)!!
        }
        answer = data.answer
        sequence = data.sequence
        rule = data.rule
    }

    private data class PatternData(val sequence: String, val answer: Int, val rule: String)

    private fun patternData(name: String, first: Int, delta: Int, alternate: Int): PatternData? {
        val values = when (name) {
            "arithmetic" -> (0..4).map { first.toLong() + delta.toLong() * it }
            "geometric" -> (0..4).fold(listOf(first.toLong())) { values, _ ->
                if (values.size == 5) values else values + values.last() * delta
            }
            "alternating" -> listOf(
                first.toLong(),
                first.toLong() + delta,
                first.toLong() + delta - alternate,
                first.toLong() + delta - alternate + delta,
                first.toLong() + delta - alternate + delta - alternate,
            )
            "squares" -> (0..4).map { (first + it).toLong() * (first + it) }
            "mixed" -> (0..4).fold(listOf(first.toLong())) { values, _ ->
                if (values.size == 5) values else values + values.last() * 2L + delta.toLong()
            }
            else -> return null
        }
        if (values.any { it !in 1..Int.MAX_VALUE }) return null
        val ints = values.map { it.toInt() }
        val missing = ints[3]
        val ruleText = when (name) {
            "arithmetic" -> "Add $delta each time"
            "geometric" -> "Multiply by $delta each time"
            "alternating" -> "Add $delta, then subtract $alternate"
            "squares" -> "Consecutive square numbers"
            else -> "Multiply by 2, then add $delta"
        }
        return PatternData("${ints[0]}, ${ints[1]}, ${ints[2]}, ?, ${ints[4]}", missing, ruleText)
    }

    private fun choices(): List<Int> {
        val candidates = when (pattern) {
            "geometric" -> listOf(answer, answer + step, answer / step, answer + 1, answer + step * 2)
            "alternating" -> listOf(answer, answer + 1, answer - secondStep, answer + secondStep, answer - step)
            "squares" -> listOf(answer, answer + 1, answer - 1, answer + 2 * (start + 3) + 1, answer - 2 * (start + 3) + 1)
            "mixed" -> listOf(answer, answer + 1, answer - 1, answer + step, answer - step)
            else -> listOf(answer, answer + 1, answer - step, answer + step, answer - 1)
        }
        val result = LinkedHashSet<Int>()
        candidates.filter { it > 0 }.forEach(result::add)
        var offset = 2
        while (result.size < 4 && offset < 100) {
            result.add((answer + offset).coerceAtLeast(1))
            offset++
        }
        return result.toList().take(4)
    }

    override fun reset() {
        score = 0
        streak = 0
        incorrect = 0
        solved = 0
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
        val oldAnswer = answer
        val oldRule = rule
        if (options[index] == oldAnswer) {
            val points = difficulty
            score += points
            solved++
            streak++
            best = maxOf(best, streak)
            feedback = "Correct — $oldRule · +$points"
        } else {
            incorrect++
            streak = 0
            feedback = "Not quite — $oldAnswer · $oldRule"
        }
        newQuestion()
        return true
    }

    override fun render() = GameViewState(
        title = title,
        subtitle = "Level $difficulty · $sequence",
        scoreLine = "Score $score  ·  Accuracy $accuracyPercent%  ·  Streak $streak  ·  Best $best",
        buttons = choices().map { it.toString() },
        buttonActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
        feedback = feedback,
    )

    override fun encode() = JSONObject().apply {
        put("v", 3)
        put("start", start)
        put("step", step)
        put("secondStep", secondStep)
        put("answer", answer)
        put("geometric", geometric)
        put("pattern", pattern)
        put("rule", rule)
        put("sequence", sequence)
        put("score", score)
        put("streak", streak)
        put("best", best)
        put("incorrect", incorrect)
        put("solved", solved)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..3) return false

        val restoredStart = json.optInt("start", -1)
        val restoredStep = json.optInt("step", -1)
        val restoredSecondStep = json.optInt("secondStep", 2)
        val restoredAnswer = json.optInt("answer", -1)
        val restoredGeometric = json.optBoolean("geometric")
        val restoredPattern = if (version >= 3) json.optString("pattern") else if (restoredGeometric) "geometric" else "arithmetic"
        val restoredSequence = json.optString("sequence")
        val restoredScore = json.optInt("score", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredIncorrect = json.optInt("incorrect", 0)
        val restoredSolved = if (version >= 3) json.optInt("solved", restoredScore) else restoredScore
        val restoredFeedback = if (version == 1) "Choose the missing term" else json.optString("feedback")
        if (restoredPattern !in setOf("arithmetic", "geometric", "alternating", "squares", "mixed") ||
            restoredStart < 1 || restoredStep < 1 || restoredSecondStep < 1 || restoredAnswer < 1 ||
            restoredSequence.isBlank() || restoredScore !in 0..1_000_000 || restoredStreak !in 0..1_000_000 ||
            restoredBest !in restoredStreak..1_000_000 || restoredIncorrect !in 0..1_000_000 ||
            restoredSolved !in 0..1_000_000 || restoredFeedback.isBlank()) return false
        if (version >= 3 && restoredGeometric != (restoredPattern == "geometric")) return false

        val expected = patternData(restoredPattern, restoredStart, restoredStep, restoredSecondStep) ?: return false
        if (restoredSequence != expected.sequence || restoredAnswer != expected.answer) return false
        if (version >= 3 && json.optString("rule") != expected.rule) return false
        if (choicesFor(expected.answer, restoredPattern, restoredStart, restoredStep, restoredSecondStep).size != 4) return false

        start = restoredStart
        step = restoredStep
        secondStep = restoredSecondStep
        answer = restoredAnswer
        geometric = restoredGeometric
        pattern = restoredPattern
        rule = expected.rule
        sequence = restoredSequence
        score = restoredScore
        streak = restoredStreak
        best = restoredBest
        incorrect = restoredIncorrect
        solved = restoredSolved
        feedback = restoredFeedback
        true
    }.getOrDefault(false)

    private fun choicesFor(value: Int, name: String, first: Int, delta: Int, alternate: Int): List<Int> {
        val candidates = when (name) {
            "geometric" -> listOf(value, value + delta, value / delta, value + 1, value + delta * 2)
            "alternating" -> listOf(value, value + 1, value - alternate, value + alternate, value - delta)
            "squares" -> listOf(value, value + 1, value - 1, value + 2 * (first + 3) + 1, value - 2 * (first + 3) + 1)
            "mixed" -> listOf(value, value + 1, value - 1, value + delta, value - delta)
            else -> listOf(value, value + 1, value - delta, value + delta, value - 1)
        }
        val result = LinkedHashSet<Int>()
        candidates.filter { it > 0 }.forEach(result::add)
        var offset = 2
        while (result.size < 4 && offset < 100) {
            result.add((value + offset).coerceAtLeast(1))
            offset++
        }
        return result.toList().take(4)
    }
}
