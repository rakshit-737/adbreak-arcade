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
    private var feedback = "Ready"

    override fun reset() {
        score = 0
        feedback = "Ready"
    }

    override fun apply(action: GameAction): Boolean = when (action) {
        GameAction.PRIMARY -> {
            score += 1
            best = maxOf(best, score)
            feedback = if (score % 10 == 0) "Milestone reached — $score taps" else "Nice — $score taps"
            true
        }
        GameAction.RESET, GameAction.NEW_GAME -> { reset(); true }
        else -> false
    }

    override fun render() = GameViewState(
        title = title,
        subtitle = "Tap without leaving your video",
        scoreLine = "Score $score  ·  Best $best",
        buttons = listOf("TAP", "", "", ""),
        buttonActions = listOf(GameAction.PRIMARY),
        feedback = feedback
    )

    override fun encode() = JSONObject().apply {
        put("v", 2)
        put("score", score)
        put("best", best)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..2) return false
        val restoredScore = json.optInt("score", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredFeedback = if (version == 1) "Ready" else json.optString("feedback")
        if (restoredScore !in 0..1_000_000 || restoredBest !in restoredScore..1_000_000 || restoredFeedback.isBlank()) return false
        score = restoredScore
        best = restoredBest
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
