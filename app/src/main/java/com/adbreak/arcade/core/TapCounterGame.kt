package com.adbreak.arcade.core

import org.json.JSONObject

class TapCounterGame : ArcadeGame {
    override val id = "tap_counter"
    override val title = "Tap Counter"
    override val description = "A satisfying score chase with one perfect notification-sized control."

    var score: Int = 0
        private set
    var best: Int = 0
        private set

    override fun reset() { score = 0 }

    override fun apply(action: GameAction): Boolean = when (action) {
        GameAction.PRIMARY -> { score += 1; best = maxOf(best, score); true }
        GameAction.RESET, GameAction.NEW_GAME -> { reset(); true }
        else -> false
    }

    override fun render() = GameViewState(
        title = title,
        subtitle = "Tap without leaving your video",
        scoreLine = "Score $score  ·  Best $best",
        buttons = listOf("TAP", "", "", ""),
        buttonActions = listOf(GameAction.PRIMARY),
        feedback = "Ready"
    )

    override fun encode() = JSONObject().apply {
        put("v", 1)
        put("score", score)
        put("best", best)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != 1) return false
        val restoredScore = json.optInt("score", -1)
        val restoredBest = json.optInt("best", -1)
        if (restoredScore < 0 || restoredBest < restoredScore) return false
        score = restoredScore
        best = restoredBest
        true
    }.getOrDefault(false)
}
