package com.adbreak.arcade.core

import org.json.JSONObject

/**
 * A target-based tap sprint. A target keeps the game meaningful in a
 * notification without requiring a wall-clock callback or a background loop.
 */
class TapCounterGame : ArcadeGame {
    override val id = "tap_counter"
    override val title = "Tap Counter"
    override val description = "Build a combo and clear a short target-based tap sprint."

    companion object {
        private const val TARGET = 20
        private const val MAX_VALUE = 1_000_000
    }

    var score: Int = 0
        private set
    var best: Int = 0
        private set
    var bestStreak: Int = 0
        private set
    var roundsCompleted: Int = 0
        private set
    private var taps = 0
    private var streak = 0
    private var feedback = "Ready"

    override fun reset() {
        score = 0
        taps = 0
        streak = 0
        feedback = "Ready"
    }

    override fun apply(action: GameAction): Boolean = when (action) {
        GameAction.PRIMARY -> if (taps >= TARGET) {
            false
        } else {
            taps++
            streak++
            val multiplier = 1 + ((streak - 1) / 5).coerceAtMost(3)
            score += multiplier
            best = maxOf(best, score)
            bestStreak = maxOf(bestStreak, streak)
            feedback = when {
                taps >= TARGET -> {
                    roundsCompleted++
                    "Sprint clear — $score points"
                }
                taps % 10 == 0 -> "Milestone reached — $taps taps"
                multiplier > 1 -> "Combo x$multiplier — +$multiplier"
                else -> "Nice — $taps taps"
            }
            true
        }
        GameAction.RESET, GameAction.NEW_GAME -> {
            reset()
            true
        }
        else -> false
    }

    override fun render() = GameViewState(
        title = title,
        subtitle = if (taps >= TARGET) "Target cleared — replay to beat your best" else "Reach $TARGET taps · every fifth tap raises the combo",
        scoreLine = "Score $score  ·  Progress $taps/$TARGET  ·  Streak $streak  ·  Best $best",
        buttons = if (taps >= TARGET) listOf("NEW SPRINT") else listOf("TAP"),
        buttonActions = if (taps >= TARGET) listOf(GameAction.NEW_GAME) else listOf(GameAction.PRIMARY),
        feedback = feedback,
        isTerminal = taps >= TARGET,
        phase = if (taps >= TARGET) GamePhase.WON else GamePhase.PLAYING,
    )

    override fun encode() = JSONObject().apply {
        put("v", 3)
        put("score", score)
        put("best", best)
        put("bestStreak", bestStreak)
        put("roundsCompleted", roundsCompleted)
        put("taps", taps)
        put("streak", streak)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..3) return false
        val restoredScore = json.optInt("score", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredTaps = if (version >= 3) json.optInt("taps", -1) else restoredScore
        val restoredStreak = if (version >= 3) json.optInt("streak", 0) else 0
        val restoredBestStreak = if (version >= 3) json.optInt("bestStreak", restoredStreak) else restoredBest
        val restoredRounds = if (version >= 3) json.optInt("roundsCompleted", 0) else 0
        val restoredFeedback = if (version == 1) "Ready" else json.optString("feedback")
        if (restoredScore !in 0..MAX_VALUE || restoredBest !in restoredScore..MAX_VALUE ||
            restoredTaps !in 0..TARGET || restoredStreak !in 0..TARGET ||
            restoredBestStreak !in restoredStreak..MAX_VALUE || restoredRounds !in 0..MAX_VALUE ||
            restoredFeedback.isBlank()) return false
        score = restoredScore
        best = restoredBest
        taps = restoredTaps
        streak = restoredStreak
        bestStreak = restoredBestStreak
        roundsCompleted = restoredRounds
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
