package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.Random

class MemoryChallengeGame(private val random: Random = Random()) : ArcadeGame {
    override val id = "memory_challenge"
    override val title = "Memory Challenge"
    override val description = "Remember the shown pattern, then tap it back in order."

    private var sequence = listOf(1, 2)
    private var input = mutableListOf<Int>()
    var level = 1
        private set
    var best = 1
        private set
    var score = 0
        private set
    private var mistakes = 0
    private var message = "Pattern: 1 2"
    private var feedback = "Memorize the pattern"

    override fun reset() {
        sequence = listOf(1, 2)
        input.clear()
        level = 1
        score = 0
        mistakes = 0
        message = "Pattern: 1 2"
        feedback = "Memorize the pattern"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        val value = when (action) {
            GameAction.PRIMARY -> 1
            GameAction.SECONDARY -> 2
            GameAction.CELL_1 -> 3
            GameAction.CELL_2 -> 4
            else -> return false
        }
        if (input.size >= sequence.size) return false

        input += value
        if (input.last() != sequence[input.lastIndex]) {
            mistakes++
            input.clear()
            message = "Repeat the pattern"
            feedback = "Miss — start the pattern again"
        } else if (input.size == sequence.size) {
            score += sequence.size * 10
            level++
            best = maxOf(best, level)
            sequence = sequence + (random.nextInt(4) + 1)
            input.clear()
            message = "Pattern: ${sequence.joinToString(" ")}"
            feedback = "Level $level — memorize the new pattern"
        } else {
            message = "Repeat the pattern"
            feedback = "Good start — keep going"
        }
        return true
    }

    override fun render() = GameViewState(
        title,
        message,
        "Score $score  ·  Level $level  ·  Best $best",
        listOf("1", "2", "3", "4"),
        listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
        feedback = feedback
    )

    override fun encode() = JSONObject().apply {
        put("v", 2)
        put("sequence", JSONArray(sequence))
        put("input", JSONArray(input))
        put("level", level)
        put("best", best)
        put("score", score)
        put("mistakes", mistakes)
        put("message", message)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..2) return false

        val restoredSequence = if (version == 1) {
            json.optString("sequence").split(",").map { it.toIntOrNull() ?: return false }
        } else {
            val array = json.optJSONArray("sequence") ?: return false
            List(array.length()) { array.optInt(it, Int.MIN_VALUE) }
        }
        val restoredInput = if (version == 1) {
            json.optString("input").let { raw ->
                if (raw.isBlank()) emptyList() else raw.split(",").map { it.toIntOrNull() ?: return false }
            }
        } else {
            val array = json.optJSONArray("input") ?: return false
            List(array.length()) { array.optInt(it, Int.MIN_VALUE) }
        }
        val restoredLevel = json.optInt("level", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredScore = json.optInt("score", 0)
        val restoredMistakes = json.optInt("mistakes", 0)
        val restoredMessage = json.optString("message")
        val restoredFeedback = if (version == 1) "Memorize the pattern" else json.optString("feedback")

        if (restoredSequence.size != restoredLevel + 1 || restoredSequence.isEmpty() || restoredSequence.any { it !in 1..4 } ||
            restoredInput.any { it !in 1..4 } || restoredInput.size >= restoredSequence.size ||
            restoredInput.indices.any { restoredInput[it] != restoredSequence[it] } ||
            restoredLevel !in 1..100 || restoredBest !in restoredLevel..100 ||
            restoredScore !in 0..1_000_000 || restoredMistakes !in 0..1_000_000 ||
            restoredMessage.isBlank() || restoredFeedback.isBlank()) return false

        sequence = restoredSequence
        input = restoredInput.toMutableList()
        level = restoredLevel
        best = restoredBest
        score = restoredScore
        mistakes = restoredMistakes
        message = restoredMessage
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
