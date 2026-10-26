package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.Random

class MemoryChallengeGame(private val random: Random = Random()) : ArcadeGame {
    override val id = "memory_challenge"
    override val title = "Memory Challenge"
    override val description = "Memorize a growing sequence, replay it cleanly, and keep three lives."

    private var sequence = listOf(1, 2)
    private var input = mutableListOf<Int>()
    var level = 1
        private set
    var best = 1
        private set
    var score = 0
        private set
    private var mistakes = 0
    private var lives = 3
    private var correctInputs = 0
    private var totalInputs = 0
    private var mastered = false
    private var roundEnded = false
    private var message = "Pattern: 1 2"
    private var feedback = "Memorize the pattern"

    private val accuracyPercent: Int
        get() = if (totalInputs == 0) 0 else correctInputs * 100 / totalInputs

    override fun reset() {
        sequence = listOf(1, 2)
        input.clear()
        level = 1
        score = 0
        mistakes = 0
        lives = 3
        correctInputs = 0
        totalInputs = 0
        mastered = false
        roundEnded = false
        message = "Pattern: 1 2"
        feedback = "Memorize the pattern"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        if (mastered || roundEnded) return false
        val value = when (action) {
            GameAction.PRIMARY -> 1
            GameAction.SECONDARY -> 2
            GameAction.CELL_1 -> 3
            GameAction.CELL_2 -> 4
            else -> return false
        }
        if (input.size >= sequence.size) return false

        totalInputs++
        input += value
        if (input.last() != sequence[input.lastIndex]) {
            mistakes++
            lives--
            input.clear()
            if (lives == 0) {
                roundEnded = true
                message = "Pattern lost · level $level"
                feedback = "Out of lives — start a new memory run"
            } else {
                message = "Repeat the pattern"
                feedback = "Miss — $lives tries left"
            }
        } else {
            correctInputs++
            if (input.size == sequence.size) {
                score += sequence.size * 10
                if (level >= 100) {
                    mastered = true
                    roundEnded = true
                    feedback = "Memory mastered — new run ready"
                } else {
                    level++
                    best = maxOf(best, level)
                    sequence = sequence + (random.nextInt(4) + 1)
                    lives = 3
                    feedback = "Level $level — memorize the new pattern"
                }
                input.clear()
                message = if (mastered) "Level $level complete" else "Pattern: ${sequence.joinToString(" ")}"
            } else {
                message = "Repeat the pattern"
                feedback = "Good start — keep going"
            }
        }
        return true
    }

    override fun render() = GameViewState(
        title = title,
        subtitle = message,
        scoreLine = "Score $score  ·  Level $level  ·  Best $best  ·  Lives $lives  ·  Accuracy $accuracyPercent%",
        buttons = if (mastered || roundEnded) listOf("NEW RUN") else listOf("1", "2", "3", "4"),
        buttonActions = if (mastered || roundEnded) listOf(GameAction.NEW_GAME) else listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
        feedback = feedback,
        isTerminal = mastered || roundEnded,
        phase = when {
            mastered -> GamePhase.WON
            roundEnded -> GamePhase.LOST
            else -> GamePhase.PLAYING
        },
    )

    override fun encode() = JSONObject().apply {
        put("v", 3)
        put("sequence", JSONArray(sequence))
        put("input", JSONArray(input))
        put("level", level)
        put("best", best)
        put("score", score)
        put("mistakes", mistakes)
        put("lives", lives)
        put("correctInputs", correctInputs)
        put("totalInputs", totalInputs)
        put("mastered", mastered)
        put("roundEnded", roundEnded)
        put("message", message)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..3) return false

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
        val restoredMastered = json.optBoolean("mastered", false)
        val restoredEnded = if (version >= 3) json.optBoolean("roundEnded", restoredMastered) else restoredMastered
        val restoredLives = if (version >= 3) json.optInt("lives", 3) else 3
        val restoredCorrect = if (version >= 3) json.optInt("correctInputs", 0) else 0
        val restoredTotal = if (version >= 3) json.optInt("totalInputs", 0) else 0
        val restoredMessage = json.optString("message")
        val restoredFeedback = if (version == 1) "Memorize the pattern" else json.optString("feedback")

        if (restoredSequence.size != restoredLevel + 1 || restoredSequence.isEmpty() || restoredSequence.any { it !in 1..4 } ||
            restoredInput.any { it !in 1..4 } || restoredInput.size >= restoredSequence.size ||
            restoredInput.indices.any { restoredInput[it] != restoredSequence[it] } ||
            restoredLevel !in 1..100 || restoredBest !in restoredLevel..100 ||
            restoredScore !in 0..1_000_000 || restoredMistakes !in 0..1_000_000 ||
            restoredLives !in 0..3 || restoredCorrect !in 0..1_000_000 || restoredTotal !in restoredCorrect..1_000_000 ||
            restoredMessage.isBlank() || restoredFeedback.isBlank()) return false

        if (restoredMastered && !restoredEnded) return false
        sequence = restoredSequence
        input = restoredInput.toMutableList()
        level = restoredLevel
        best = restoredBest
        score = restoredScore
        mistakes = restoredMistakes
        lives = restoredLives
        correctInputs = restoredCorrect
        totalInputs = restoredTotal
        mastered = restoredMastered
        roundEnded = restoredEnded
        message = restoredMessage
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
