package com.adbreak.arcade.core

import org.json.JSONObject
import java.util.Random

class HigherLowerGame(private val random: Random = Random()) : ArcadeGame {
    override val id = "higher_lower"
    override val title = "Higher or Lower"
    override val description = "Find the hidden number; midpoint guesses always have enough tries."

    private var rangeMax = 20
    private var low = 1
    private var high = 20
    private var secret = random.nextInt(20) + 1
    private var attempts = 5
    private var attemptLimit = 5
    private var message = "Guess 1–20"
    private var feedback = "MID narrows safely; edges risk more tries"
    private var roundEnded = false
    var wins = 0
        private set
    var bestStreak = 0
        private set
    var score = 0
        private set
    private var lastRoundScore = 0
    private var streak = 0
    private var roundsCompleted = 0

    private fun nextRangeMax() = (20 + wins.coerceAtMost(6) * 5).coerceAtMost(50)
    private fun guessesNeeded(maximum: Int) = if (maximum > 31) 6 else 5

    private fun startRound() {
        if (!roundEnded && attempts < attemptLimit) streak = 0
        rangeMax = nextRangeMax()
        low = 1
        high = rangeMax
        secret = random.nextInt(rangeMax) + 1
        attemptLimit = guessesNeeded(rangeMax)
        attempts = attemptLimit
        message = "Guess 1–$rangeMax"
        feedback = "MID narrows safely; edges risk more tries"
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
        if (action in duplicateActions()) return false
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
            roundsCompleted++
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
        if (guess < secret) low = guess + 1 else high = guess - 1
        if (attempts <= 0) {
            streak = 0
            roundsCompleted++
            message = "Out of tries — it was $secret"
            feedback = "Round over — start a new round to try again"
            roundEnded = true
        } else if (guess < secret) {
            message = "Higher than $guess"
            feedback = "Ruled out $guess and below · $attempts tries left"
        } else {
            message = "Lower than $guess"
            feedback = "Ruled out $guess and above · $attempts tries left"
        }
        return true
    }

    private fun duplicateActions(): Map<GameAction, String> = buildMap {
        val midpoint = (low + high) / 2
        if (low == midpoint) put(GameAction.SECONDARY, "Same guess as MID $midpoint")
        if (high == midpoint) put(GameAction.CELL_1, "Same guess as MID $midpoint")
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
        return GameViewState(
            title,
            message,
            "Range $low–$high  ·  Tries $attempts/$attemptLimit  ·  Win streak $streak",
            labels,
            actions,
            feedback = feedback,
            isTerminal = roundEnded,
            phase = if (!roundEnded) GamePhase.PLAYING else if (message.startsWith("Correct")) GamePhase.WON else GamePhase.LOST,
            progress = GameProgress(if (roundEnded && attempts > 0) rangeMax - 1 else rangeMax - (high - low + 1), rangeMax - 1, "Numbers ruled out"),
            record = if (roundEnded && attempts > 0 && streak > 0) GameRecord(streak, "Round win streak") else null,
            disabledActions = if (roundEnded) emptyMap() else duplicateActions(),
        )
    }

    override fun encode() = JSONObject().apply {
        put("v", 4)
        put("attemptLimit", attemptLimit)
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
        put("roundsCompleted", roundsCompleted)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..4) return false

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
        val restoredRounds = json.optInt("roundsCompleted", 0)
        val restoredLimit = if (version >= 4) json.optInt("attemptLimit", -1) else guessesNeeded(restoredRangeMax)

        if (restoredRangeMax !in 20..50 || restoredLow !in 1..restoredRangeMax ||
            restoredHigh !in restoredLow..restoredRangeMax || restoredSecret !in 1..restoredRangeMax ||
            restoredLimit != guessesNeeded(restoredRangeMax) ||
            restoredAttempts !in 0..(if (version >= 4) restoredLimit else 5) || restoredMessage.isBlank() || restoredFeedback.isBlank() ||
            restoredWins !in 0..1_000_000 || restoredBest !in 0..1_000_000 ||
            restoredStreak !in 0..restoredBest || restoredScore !in 0..10_000_000 ||
            restoredLastRoundScore !in 0..100 || restoredRounds !in 0..1_000_000) return false

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
        // Active legacy large-range boards receive the extra guess they should have had.
        attempts = restoredAttempts + if (version < 4 && !restoredEnded) restoredLimit - 5 else 0
        attemptLimit = restoredLimit
        message = restoredMessage
        feedback = restoredFeedback
        roundEnded = restoredEnded
        wins = restoredWins
        bestStreak = restoredBest
        score = restoredScore
        lastRoundScore = restoredLastRoundScore
        streak = restoredStreak
        roundsCompleted = restoredRounds
        true
    }.getOrDefault(false)
}
