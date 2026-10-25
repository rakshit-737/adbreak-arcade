package com.adbreak.arcade.core

import org.json.JSONObject
import java.util.Random

class HigherLowerGame(private val random: Random = Random()) : ArcadeGame {
    override val id = "higher_lower"
    override val title = "Higher or Lower"
    override val description = "Read the clue, narrow the range, and make a guess."

    private var rangeMax = 20
    private var low = 1
    private var high = 20
    private var secret = random.nextInt(20) + 1
    private var attempts = 5
    private var message = "Guess 1–20"
    private var feedback = "Use the midpoint or an edge guess"
    private var roundEnded = false
    var wins = 0
        private set
    var bestStreak = 0
        private set
    var score = 0
        private set
    private var lastRoundScore = 0
    private var streak = 0

    private fun nextRangeMax() = (20 + wins.coerceAtMost(6) * 5).coerceAtMost(50)

    private fun startRound() {
        rangeMax = nextRangeMax()
        low = 1
        high = rangeMax
        secret = random.nextInt(rangeMax) + 1
        attempts = 5
        message = "Guess 1–$rangeMax"
        feedback = "Use the midpoint or an edge guess"
        roundEnded = false
        lastRoundScore = 0
    }

    override fun reset() = startRound()

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            startRound()
            return true
        }
        if (roundEnded) return false
        val guess = when (action) {
            GameAction.PRIMARY -> (low + high) / 2
            GameAction.SECONDARY -> low
            GameAction.CELL_1 -> high
            else -> return false
        }
        if (guess !in low..high) return false

        if (guess == secret) {
            val roundScore = attempts + rangeMax / 10
            wins++
            streak++
            bestStreak = maxOf(bestStreak, streak)
            score += roundScore
            lastRoundScore = roundScore
            message = "Correct — $secret! Start a new round when ready"
            feedback = "Great read — +$roundScore points"
            roundEnded = true
            return true
        }

        attempts--
        streak = 0
        if (attempts <= 0) {
            message = "Out of tries — it was $secret"
            feedback = "Round over — start a new round to try again"
            roundEnded = true
        } else if (guess < secret) {
            low = (guess + 1).coerceAtMost(high)
            message = "Higher than $guess"
            feedback = "The range is getting tighter"
        } else {
            high = (guess - 1).coerceAtLeast(low)
            message = "Lower than $guess"
            feedback = "The range is getting tighter"
        }
        return true
    }

    override fun render(): GameViewState {
        val labels = if (roundEnded) {
            listOf("NEW ROUND")
        } else {
            listOf("MID ${(low + high) / 2}", "LOW $low", "HIGH $high")
        }
        val actions = if (roundEnded) {
            listOf(GameAction.NEW_GAME)
        } else {
            listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1)
        }
        val roundSummary = if (lastRoundScore > 0) "  ·  Last +$lastRoundScore" else ""
        return GameViewState(
            title,
            message,
            "Range $low–$high  ·  Tries $attempts  ·  Wins $wins  ·  Score $score$roundSummary",
            labels,
            actions,
            feedback = feedback,
            isTerminal = roundEnded,
            phase = if (!roundEnded) GamePhase.PLAYING else if (message.startsWith("Correct")) GamePhase.WON else GamePhase.LOST
        )
    }

    override fun encode() = JSONObject().apply {
        put("v", 2)
        put("rangeMax", rangeMax)
        put("low", low)
        put("high", high)
        put("secret", secret)
        put("attempts", attempts)
        put("message", message)
        put("feedback", feedback)
        put("roundEnded", roundEnded)
        put("wins", wins)
        put("bestStreak", bestStreak)
        put("score", score)
        put("lastRoundScore", lastRoundScore)
        put("streak", streak)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..2) return false

        val restoredRangeMax = json.optInt("rangeMax", 20)
        val restoredLow = json.optInt("low", -1)
        val restoredHigh = json.optInt("high", -1)
        val restoredSecret = json.optInt("secret", -1)
        val restoredAttempts = json.optInt("attempts", -1)
        val restoredMessage = json.optString("message")
        val restoredFeedback = if (version == 1) "Use the midpoint or an edge guess" else json.optString("feedback")
        val restoredEnded = json.optBoolean("roundEnded")
        val restoredWins = json.optInt("wins", -1)
        val restoredBest = json.optInt("bestStreak", -1)
        val restoredScore = json.optInt("score", restoredWins)
        val restoredLastRoundScore = json.optInt("lastRoundScore", 0)
        val restoredStreak = json.optInt("streak", -1)

        if (restoredRangeMax !in 20..50 || restoredLow !in 1..restoredRangeMax ||
            restoredHigh !in restoredLow..restoredRangeMax || restoredSecret !in 1..restoredRangeMax ||
            restoredAttempts !in 0..5 || restoredMessage.isBlank() || restoredFeedback.isBlank() ||
            restoredWins !in 0..1_000_000 || restoredBest !in 0..1_000_000 ||
            restoredStreak !in 0..restoredBest || restoredScore !in 0..10_000_000 ||
            restoredLastRoundScore !in 0..100) return false

        if (!restoredEnded) {
            if (restoredAttempts == 0 || restoredSecret !in restoredLow..restoredHigh) return false
        } else if (restoredAttempts == 0) {
            if (!restoredMessage.startsWith("Out of tries")) return false
        } else if (!restoredMessage.startsWith("Correct")) {
            return false
        }

        rangeMax = restoredRangeMax
        low = restoredLow
        high = restoredHigh
        secret = restoredSecret
        attempts = restoredAttempts
        message = restoredMessage
        feedback = restoredFeedback
        roundEnded = restoredEnded
        wins = restoredWins
        bestStreak = restoredBest
        score = restoredScore
        lastRoundScore = restoredLastRoundScore
        streak = restoredStreak
        true
    }.getOrDefault(false)
}
