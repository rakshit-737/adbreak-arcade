package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.Random

/**
 * A short, notification-friendly memory game.  A level is a sequence of
 * pulses (1-4); the player repeats it with four discrete actions.  There is
 * deliberately no timer or continuous input, so a round can be resumed from
 * a notification without losing a partially entered pattern.
 */
class PulseReactorGame(private val random: Random = Random()) : ArcadeGame {
    override val id = "pulse_reactor"
    override val title = "Pulse Reactor"
    override val description = "Repeat an escalating pulse pattern before the reactor overloads."

    companion object {
        private const val VERSION = 1
        private const val STARTING_LEVEL = 1
        private const val STARTING_LENGTH = 3
        private const val MAX_LEVEL = 10
        private const val PULSE_COUNT = 4
        private const val MISTAKE_BUDGET = 3
        private const val MAX_SCORE = 1_000_000_000
    }

    var level: Int = STARTING_LEVEL
        private set
    var score: Int = 0
        private set
    var streak: Int = 0
        private set
    var best: Int = 0
        private set
    var mistakes: Int = 0
        private set
    var mistakesRemaining: Int = MISTAKE_BUDGET
        private set

    private var sequence = emptyList<Int>()
    private var input = mutableListOf<Int>()
    private var patternVisible = true
    private var ended = false
    private var won = false
    private var feedback = "Memorize the pulse pattern"

    init {
        reset()
    }

    override fun reset() {
        level = STARTING_LEVEL
        score = 0
        streak = 0
        mistakes = 0
        mistakesRemaining = MISTAKE_BUDGET
        input.clear()
        sequence = nextSequence(sequenceLength(level))
        patternVisible = true
        ended = false
        won = false
        feedback = "Memorize the pulse pattern"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        if (ended) return false

        val pulse = when (action) {
            GameAction.PRIMARY -> 1
            GameAction.SECONDARY -> 2
            GameAction.CELL_1 -> 3
            GameAction.CELL_2 -> 4
            else -> return false
        }
        if (input.size >= sequence.size) return false

        val position = input.size
        input += pulse
        patternVisible = false
        if (pulse != sequence[position]) {
            input.clear()
            mistakes++
            mistakesRemaining = MISTAKE_BUDGET - mistakes
            streak = 0
            if (mistakesRemaining == 0) {
                ended = true
                feedback = "Reactor overload — the pattern was ${sequence.joinToString(" ")}"
            } else {
                feedback = "Mismatch at pulse ${position + 1} — $mistakesRemaining mistakes left"
            }
            return true
        }

        if (input.size < sequence.size) {
            feedback = "Pulse ${input.size}/${sequence.size} locked"
            return true
        }

        streak++
        best = maxOf(best, streak)
        val multiplier = 1 + ((streak - 1) / 3)
        val points = sequence.size * level * multiplier
        score += points
        if (level == MAX_LEVEL) {
            ended = true
            won = true
            feedback = "Reactor stabilized — +$points points"
        } else {
            level++
            mistakes = 0
            mistakesRemaining = MISTAKE_BUDGET
            input.clear()
            sequence = nextSequence(sequenceLength(level))
            patternVisible = true
            feedback = "Level $level — new pulse pattern ready · +$points"
        }
        return true
    }

    override fun render() = GameViewState(
        title = title,
        subtitle = when {
            won -> "Reactor stable — Level $level complete · replay for another run"
            ended -> "Reactor overloaded at Level $level · replay to try again"
            else -> if (patternVisible) {
                "Level $level · Memorize ${sequence.joinToString(" ")} · Input 0/${sequence.size}"
            } else {
                "Level $level · Repeat the pattern · Input ${input.size}/${sequence.size}"
            }
        },
        scoreLine = "Score $score  ·  Level $level  ·  Streak $streak  ·  Best $best  ·  Mistakes $mistakes/$MISTAKE_BUDGET",
        buttons = if (ended) listOf("NEW RUN") else listOf("1", "2", "3", "4"),
        buttonActions = if (ended) listOf(GameAction.NEW_GAME) else listOf(
            GameAction.PRIMARY,
            GameAction.SECONDARY,
            GameAction.CELL_1,
            GameAction.CELL_2,
        ),
        feedback = feedback,
        isTerminal = ended,
        phase = when {
            won -> GamePhase.WON
            ended -> GamePhase.LOST
            else -> GamePhase.PLAYING
        },
    )

    override fun encode() = JSONObject().apply {
        put("v", VERSION)
        put("sequence", JSONArray(sequence))
        put("input", JSONArray(input))
        put("patternVisible", patternVisible)
        put("level", level)
        put("score", score)
        put("streak", streak)
        put("best", best)
        put("mistakes", mistakes)
        put("mistakesRemaining", mistakesRemaining)
        put("terminal", ended)
        put("won", won)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != VERSION) return false

        val sequenceArray = json.optJSONArray("sequence") ?: return false
        val inputArray = json.optJSONArray("input") ?: return false
        val restoredLevel = json.optInt("level", -1)
        val restoredSequence = List(sequenceArray.length()) {
            sequenceArray.optInt(it, Int.MIN_VALUE)
        }
        val restoredInput = List(inputArray.length()) {
            inputArray.optInt(it, Int.MIN_VALUE)
        }
        val restoredPatternVisible = json.optBoolean("patternVisible", restoredInput.isEmpty())
        val restoredScore = json.optInt("score", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredMistakes = json.optInt("mistakes", -1)
        val restoredRemaining = json.optInt("mistakesRemaining", -1)
        val restoredEnded = json.optBoolean("terminal", false)
        val restoredWon = json.optBoolean("won", false)
        val restoredFeedback = json.optString("feedback")

        val expectedLength = sequenceLength(restoredLevel)
        if (restoredLevel !in STARTING_LEVEL..MAX_LEVEL ||
            restoredSequence.size != expectedLength ||
            restoredSequence.any { it !in 1..PULSE_COUNT } ||
            restoredInput.any { it !in 1..PULSE_COUNT } ||
            restoredInput.size > restoredSequence.size ||
            restoredInput.indices.any { restoredInput[it] != restoredSequence[it] } ||
            restoredPatternVisible && restoredInput.isNotEmpty() ||
            restoredScore !in 0..MAX_SCORE ||
            restoredStreak !in 0..MAX_SCORE ||
            restoredBest !in restoredStreak..MAX_SCORE ||
            restoredMistakes !in 0..MISTAKE_BUDGET ||
            restoredRemaining != MISTAKE_BUDGET - restoredMistakes ||
            restoredFeedback.isBlank()
        ) return false

        val shouldBeTerminal = restoredWon || restoredMistakes == MISTAKE_BUDGET
        if (restoredEnded != shouldBeTerminal || (restoredWon && restoredLevel != MAX_LEVEL) ||
            (restoredWon && restoredInput.size != restoredSequence.size) ||
            (!restoredWon && restoredEnded && restoredInput.isNotEmpty()) ||
            (!restoredEnded && restoredInput.size >= restoredSequence.size)
        ) return false

        sequence = restoredSequence
        input = restoredInput.toMutableList()
        patternVisible = restoredPatternVisible
        level = restoredLevel
        score = restoredScore
        streak = restoredStreak
        best = restoredBest
        mistakes = restoredMistakes
        mistakesRemaining = restoredRemaining
        ended = restoredEnded
        won = restoredWon
        feedback = restoredFeedback
        true
    }.getOrDefault(false)

    private fun sequenceLength(currentLevel: Int): Int = STARTING_LENGTH + currentLevel - STARTING_LEVEL

    private fun nextSequence(length: Int): List<Int> =
        List(length) { random.nextInt(PULSE_COUNT) + 1 }
}
