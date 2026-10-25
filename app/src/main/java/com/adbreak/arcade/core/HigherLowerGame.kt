package com.adbreak.arcade.core

import org.json.JSONObject
import java.util.Random

class HigherLowerGame(private val random: Random = Random(11)) : ArcadeGame {
    override val id = "higher_lower"
    override val title = "Higher or Lower"
    override val description = "Read the clue, narrow the range, and make a guess."

    private var low = 1
    private var high = 20
    private var secret = random.nextInt(20) + 1
    private var attempts = 5
    private var message = "Guess 1–20"
    private var roundEnded = false
    var wins = 0
        private set
    var bestStreak = 0
        private set
    private var streak = 0

    private fun startRound() {
        low = 1; high = 20; secret = random.nextInt(20) + 1; attempts = 5
        message = "Guess 1–20"; roundEnded = false
    }

    override fun reset() = startRound()

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) { startRound(); return true }
        if (roundEnded) return false
        val guess = when (action) {
            GameAction.PRIMARY -> (low + high) / 2
            GameAction.SECONDARY -> low
            GameAction.CELL_1 -> high
            else -> return false
        }
        if (guess == secret) {
            wins++; streak++; bestStreak = maxOf(bestStreak, streak)
            message = "Correct — $secret! Start a new round when ready"
            roundEnded = true
            return true
        }
        attempts--
        streak = 0
        if (attempts <= 0) {
            message = "Out of tries — it was $secret"
            roundEnded = true
        } else if (guess < secret) {
            low = (guess + 1).coerceAtMost(high)
            message = "Higher than $guess"
        } else {
            high = (guess - 1).coerceAtLeast(low)
            message = "Lower than $guess"
        }
        return true
    }

    override fun render(): GameViewState {
        val labels = if (roundEnded) listOf("NEW ROUND") else listOf("MID", "LOW", "HIGH")
        val actions = if (roundEnded) listOf(GameAction.NEW_GAME) else listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1)
        return GameViewState(title, message, "Range $low–$high  ·  Tries $attempts  ·  Wins $wins", labels, actions)
    }

    override fun encode() = JSONObject().apply {
        put("v", 1); put("low", low); put("high", high); put("secret", secret)
        put("attempts", attempts); put("message", message); put("roundEnded", roundEnded)
        put("wins", wins); put("bestStreak", bestStreak); put("streak", streak)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val j = JSONObject(encoded)
        if (j.optInt("v", -1) != 1) return false
        val restoredLow = j.optInt("low", -1); val restoredHigh = j.optInt("high", -1)
        val restoredSecret = j.optInt("secret", -1); val restoredAttempts = j.optInt("attempts", -1)
        val restoredMessage = j.optString("message"); val restoredEnded = j.optBoolean("roundEnded")
        val restoredWins = j.optInt("wins", -1); val restoredBest = j.optInt("bestStreak", -1); val restoredStreak = j.optInt("streak", -1)
        if (restoredLow !in 1..20 || restoredHigh !in restoredLow..20 || restoredSecret !in restoredLow..restoredHigh || restoredAttempts !in 0..5 || restoredMessage.isBlank() || restoredWins < 0 || restoredBest < restoredStreak || restoredStreak < 0) return false
        low = restoredLow; high = restoredHigh; secret = restoredSecret; attempts = restoredAttempts; message = restoredMessage; roundEnded = restoredEnded; wins = restoredWins; bestStreak = restoredBest; streak = restoredStreak
        true
    }.getOrDefault(false)
}
