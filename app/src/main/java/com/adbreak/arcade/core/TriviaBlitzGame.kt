package com.adbreak.arcade.core

import org.json.JSONObject

class TriviaBlitzGame : ArcadeGame {
    override val id = "trivia_blitz"
    override val title = "Trivia Blitz"
    override val description = "Answer a bite-sized general knowledge question from a rotating offline deck."

    private data class Question(val prompt: String, val choices: List<String>, val answerIndex: Int, val explanation: String)
    private val legacyQuestionCount = 4

    private val questions = listOf(
        Question("Which planet is known as the Red Planet?", listOf("Venus", "Jupiter", "Mars", "Mercury"), 2, "Mars: iron oxide gives its surface a red color"),
        Question("How many sides does a hexagon have?", listOf("5", "7", "8", "6"), 3, "6: the prefix hexa means six"),
        Question("Which gas do plants use in photosynthesis?", listOf("Oxygen", "Carbon dioxide", "Nitrogen", "Helium"), 1, "Carbon dioxide: plants use it to build sugars"),
        Question("Which ocean is the largest?", listOf("Atlantic", "Indian", "Pacific", "Arctic"), 2, "Pacific: it covers the largest surface area"),
        Question("What is the fastest land animal?", listOf("Cheetah", "Horse", "Ostrich", "Lion"), 0, "Cheetah: the fastest land sprinter"),
        Question("Which instrument has keys, pedals, and strings?", listOf("Violin", "Piano", "Flute", "Drum"), 1, "Piano: keys drive hammers that strike strings"),
        Question("What is the first prime number?", listOf("0", "1", "2", "3"), 2, "2: its only positive divisors are 1 and 2"),
        Question("Which shape has three sides?", listOf("Square", "Circle", "Triangle", "Hexagon"), 2, "Triangle: tri means three")
    )
    private var questionIndex = 0
    private var startIndex = 0
    private var bounded = true
    private var attempts = 0
    private var score = 0
    private var streak = 0
    private var best = 0
    private var feedback = "Answer the question"

    override fun reset() {
        startIndex = (startIndex + 1) % questions.size
        questionIndex = startIndex
        bounded = true
        attempts = 0
        score = 0
        streak = 0
        best = 0
        feedback = "Answer the question"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        if (isOver()) return false
        val choiceIndex = when (action) {
            GameAction.PRIMARY -> 0
            GameAction.SECONDARY -> 1
            GameAction.CELL_1 -> 2
            GameAction.CELL_2 -> 3
            else -> return false
        }
        val question = questions[questionIndex]
        attempts++
        if ((choiceIndex + startIndex) % 4 == question.answerIndex) {
            score++
            streak++
            best = maxOf(best, streak)
            feedback = "Correct — ${question.explanation}"
        } else {
            streak = 0
            feedback = "Not quite — ${question.explanation}"
        }
        questionIndex = (questionIndex + 1) % questions.size
        return true
    }

    private fun isOver() = bounded && attempts >= questions.size

    override fun render(): GameViewState {
        val question = questions[questionIndex]
        return GameViewState(
            title = title,
            subtitle = if (isOver()) "Deck complete · $score/${questions.size} correct" else question.prompt,
            scoreLine = "Correct $score/$attempts  ·  Streak $streak",
            buttons = if (isOver()) listOf("NEW DECK") else question.choices.drop(startIndex % 4) + question.choices.take(startIndex % 4),
            buttonActions = if (isOver()) listOf(GameAction.NEW_GAME) else listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
            feedback = feedback,
            isTerminal = isOver(),
            phase = if (!isOver()) GamePhase.PLAYING else if (score == questions.size) GamePhase.WON else GamePhase.DRAW,
            progress = if (bounded) GameProgress(attempts, questions.size, "Questions answered") else null,
            record = if (isOver()) GameRecord(score, "Correct / ${questions.size}") else null
        )
    }

    override fun encode() = JSONObject().apply {
        put("v", 2)
        put("questionIndex", questionIndex)
        put("startIndex", startIndex)
        put("bounded", bounded)
        put("attempts", attempts)
        put("score", score)
        put("streak", streak)
        put("best", best)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..2) return false
        val restoredQuestionIndex = json.optInt("questionIndex", -1)
        val restoredAttempts = json.optInt("attempts", -1)
        val restoredScore = json.optInt("score", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredFeedback = json.optString("feedback")
        val expectedDeckSize = if (version == 1) legacyQuestionCount else questions.size
        val restoredStart = json.optInt("startIndex", 0)
        val restoredBounded = json.optBoolean("bounded", false)
        if (restoredQuestionIndex !in 0 until expectedDeckSize || restoredAttempts < 0 ||
            restoredStart !in 0 until expectedDeckSize ||
            restoredQuestionIndex != (restoredStart + restoredAttempts % expectedDeckSize) % expectedDeckSize ||
            (restoredBounded && restoredAttempts > expectedDeckSize) ||
            restoredScore < 0 || restoredScore > restoredAttempts || restoredStreak < 0 ||
            restoredStreak > restoredScore || restoredBest < restoredStreak ||
            restoredBest > restoredScore || restoredFeedback.isBlank()
        ) return false
        questionIndex = restoredQuestionIndex % questions.size
        // Keep the pending legacy question while migrating its four-card cursor to eight cards.
        startIndex = (questionIndex - restoredAttempts % questions.size + questions.size) % questions.size
        bounded = restoredBounded
        attempts = restoredAttempts
        score = restoredScore
        streak = restoredStreak
        best = restoredBest
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
