package com.adbreak.arcade.core

import org.json.JSONObject

class TriviaBlitzGame : ArcadeGame {
    override val id = "trivia_blitz"
    override val title = "Trivia Blitz"
    override val description = "Answer a bite-sized general knowledge question before the moment passes."

    private data class Question(val prompt: String, val choices: List<String>, val answerIndex: Int)

    private val questions = listOf(
        Question("Which planet is known as the Red Planet?", listOf("Mars", "Venus", "Jupiter", "Mercury"), 0),
        Question("How many sides does a hexagon have?", listOf("6", "5", "7", "8"), 0),
        Question("What gas do plants absorb?", listOf("Oxygen", "Nitrogen", "Carbon dioxide", "Helium"), 2),
        Question("Which ocean is the largest?", listOf("Pacific", "Atlantic", "Indian", "Arctic"), 0)
    )
    private var questionIndex = 0
    private var attempts = 0
    private var score = 0
    private var streak = 0
    private var best = 0
    private var feedback = "Answer the question"

    override fun reset() {
        questionIndex = 0
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
        val choiceIndex = when (action) {
            GameAction.PRIMARY -> 0
            GameAction.SECONDARY -> 1
            GameAction.CELL_1 -> 2
            GameAction.CELL_2 -> 3
            else -> return false
        }
        val question = questions[questionIndex]
        attempts++
        if (choiceIndex == question.answerIndex) {
            score++
            streak++
            best = maxOf(best, streak)
            feedback = "Correct — ${question.choices[question.answerIndex]}"
        } else {
            streak = 0
            feedback = "Correct answer: ${question.choices[question.answerIndex]}"
        }
        questionIndex = (questionIndex + 1) % questions.size
        return true
    }

    override fun render(): GameViewState {
        val question = questions[questionIndex]
        return GameViewState(
            title = title,
            subtitle = question.prompt,
            scoreLine = "Score $score  ·  Streak $streak  ·  Best $best",
            buttons = question.choices,
            buttonActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
            feedback = feedback
        )
    }

    override fun encode() = JSONObject().apply {
        put("v", 1)
        put("questionIndex", questionIndex)
        put("attempts", attempts)
        put("score", score)
        put("streak", streak)
        put("best", best)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != 1) return false
        val restoredQuestionIndex = json.optInt("questionIndex", -1)
        val restoredAttempts = json.optInt("attempts", -1)
        val restoredScore = json.optInt("score", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("best", -1)
        val restoredFeedback = json.optString("feedback")
        if (restoredQuestionIndex !in questions.indices || restoredAttempts < 0 ||
            restoredQuestionIndex != restoredAttempts % questions.size ||
            restoredScore < 0 || restoredScore > restoredAttempts || restoredStreak < 0 ||
            restoredStreak > restoredScore || restoredBest < restoredStreak ||
            restoredBest > restoredScore || restoredFeedback.isBlank()
        ) return false
        questionIndex = restoredQuestionIndex
        attempts = restoredAttempts
        score = restoredScore
        streak = restoredStreak
        best = restoredBest
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
