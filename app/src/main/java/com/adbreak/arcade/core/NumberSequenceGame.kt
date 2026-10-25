package com.adbreak.arcade.core

import org.json.JSONObject
import java.util.Random

class NumberSequenceGame(private val random: Random = Random(8)) : ArcadeGame {
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

    init { newQuestion() }

    private fun newQuestion() {
        geometric = random.nextBoolean()
        if (geometric) {
            start = random.nextInt(3) + 1; step = random.nextInt(2) + 2
            answer = start * step * step * step
            sequence = "$start, ${start * step}, ${start * step * step}, ?, ${start * step * step * step * step}"
        } else {
            start = random.nextInt(8) + 1; step = random.nextInt(5) + 2
            answer = start + step * 3
            sequence = "$start, ${start + step}, ${start + step * 2}, ?, ${start + step * 4}"
        }
    }

    private fun choices() = if (geometric) listOf(answer, answer + step, answer / step, answer + 1) else listOf(answer, answer + 1, answer - step, answer + step)

    override fun reset() { score = 0; streak = 0; newQuestion() }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) { reset(); return true }
        val index = when (action) { GameAction.PRIMARY -> 0; GameAction.SECONDARY -> 1; GameAction.CELL_1 -> 2; GameAction.CELL_2 -> 3; else -> -1 }
        val options = choices()
        if (index !in options.indices) return false
        if (options[index] == answer) { score++; streak++; best = maxOf(best, streak) } else streak = 0
        newQuestion()
        return true
    }

    override fun render() = GameViewState(title, sequence, "Score $score  ·  Streak $streak  ·  Best $best", choices().map { it.toString() }, listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2))

    override fun encode() = JSONObject().apply {
        put("v", 1); put("start", start); put("step", step); put("answer", answer); put("geometric", geometric)
        put("sequence", sequence); put("score", score); put("streak", streak); put("best", best)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val j = JSONObject(encoded); if (j.optInt("v", -1) != 1) return false
        val restoredStart = j.optInt("start", -1); val restoredStep = j.optInt("step", -1); val restoredAnswer = j.optInt("answer", -1); val restoredGeometric = j.optBoolean("geometric"); val restoredSequence = j.optString("sequence"); val restoredScore = j.optInt("score", -1); val restoredStreak = j.optInt("streak", -1); val restoredBest = j.optInt("best", -1)
        if (restoredStart < 1 || restoredStep < 2 || restoredAnswer < 1 || restoredSequence.isBlank() || restoredScore < 0 || restoredStreak < 0 || restoredBest < restoredStreak) return false
        val expectedSequence = if (restoredGeometric) "$restoredStart, ${restoredStart * restoredStep}, ${restoredStart * restoredStep * restoredStep}, ?, ${restoredStart * restoredStep * restoredStep * restoredStep * restoredStep}" else "$restoredStart, ${restoredStart + restoredStep}, ${restoredStart + restoredStep * 2}, ?, ${restoredStart + restoredStep * 4}"
        val expectedAnswer = if (restoredGeometric) restoredStart * restoredStep * restoredStep * restoredStep else restoredStart + restoredStep * 3
        if (restoredSequence != expectedSequence || restoredAnswer != expectedAnswer) return false
        start = restoredStart; step = restoredStep; answer = restoredAnswer; geometric = restoredGeometric; sequence = restoredSequence; score = restoredScore; streak = restoredStreak; best = restoredBest
        true
    }.getOrDefault(false)
}
