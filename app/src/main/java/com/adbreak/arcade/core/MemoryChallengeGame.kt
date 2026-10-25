package com.adbreak.arcade.core

import org.json.JSONObject

class MemoryChallengeGame : ArcadeGame {
    override val id = "memory_challenge"
    override val title = "Memory Challenge"
    override val description = "Remember the shown pattern, then tap it back in order."

    private var sequence = listOf(1, 2)
    private var input = mutableListOf<Int>()
    var level = 1
        private set
    var best = 1
        private set
    private var message = "Pattern: 1 2"

    override fun reset() { sequence = listOf(1, 2); input.clear(); level = 1; message = "Pattern: 1 2" }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) { reset(); return true }
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
            message = "Not quite — pattern: ${sequence.joinToString(" ")}"
            input.clear()
        } else if (input.size == sequence.size) {
            level++
            best = maxOf(best, level)
            sequence = sequence + ((level + 1) % 4 + 1)
            input.clear()
            message = "Level $level — pattern: ${sequence.joinToString(" ")}"
        }
        return true
    }

    override fun render() = GameViewState(
        title,
        message,
        "Level $level  ·  Best $best",
        listOf("1", "2", "3", "4"),
        listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2)
    )

    override fun encode() = JSONObject().apply {
        put("v", 1); put("sequence", sequence.joinToString(",")); put("input", input.joinToString(","))
        put("level", level); put("best", best); put("message", message)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val j = JSONObject(encoded)
        if (j.optInt("v", -1) != 1) return false
        val restoredSequence = j.optString("sequence").split(",").map { it.toIntOrNull() ?: return false }
        val restoredInput = j.optString("input").let { raw -> if (raw.isBlank()) emptyList() else raw.split(",").map { it.toIntOrNull() ?: return false } }
        val restoredLevel = j.optInt("level", -1); val restoredBest = j.optInt("best", -1); val restoredMessage = j.optString("message")
        if (restoredSequence.isEmpty() || restoredSequence.any { it !in 1..4 } || restoredInput.any { it !in 1..4 } || restoredInput.size > restoredSequence.size || restoredInput.indices.any { restoredInput[it] != restoredSequence[it] } || restoredLevel < 1 || restoredBest < restoredLevel || restoredMessage.isBlank()) return false
        sequence = restoredSequence; input = restoredInput.toMutableList(); level = restoredLevel; best = restoredBest; message = restoredMessage
        true
    }.getOrDefault(false)
}
