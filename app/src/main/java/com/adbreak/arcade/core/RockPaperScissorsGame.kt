package com.adbreak.arcade.core

import org.json.JSONObject
import java.util.Random

class RockPaperScissorsGame(private val random: Random = Random()) : ArcadeGame {
    override val id = "rock_paper_scissors"
    override val title = "Rock Paper Scissors"
    override val description = "Pick a move and outsmart the arcade in one quick round."

    private val moves = listOf("Rock", "Paper", "Scissors")
    private var round = 0
    private var computerMove = random.nextInt(3)
    private var wins = 0
    private var losses = 0
    private var draws = 0
    private var feedback = "Choose your move"

    override fun reset() {
        round = 0
        computerMove = random.nextInt(3)
        wins = 0
        losses = 0
        draws = 0
        feedback = "Choose your move"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        val playerMove = when (action) {
            GameAction.PRIMARY -> 0
            GameAction.SECONDARY -> 1
            GameAction.CELL_1 -> 2
            else -> return false
        }
        val result = (playerMove - computerMove + 3) % 3
        when (result) {
            0 -> {
                draws++
                feedback = "Draw — arcade chose ${moves[computerMove]}"
            }
            1 -> {
                wins++
                feedback = "You win — arcade chose ${moves[computerMove]}"
            }
            else -> {
                losses++
                feedback = "Arcade wins — it chose ${moves[computerMove]}"
            }
        }
        round++
        computerMove = random.nextInt(3)
        return true
    }

    override fun render() = GameViewState(
        title = title,
        subtitle = "Rock, paper, or scissors?",
        scoreLine = "Wins $wins  ·  Losses $losses  ·  Draws $draws",
        buttons = moves,
        buttonActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1),
        feedback = feedback
    )

    override fun encode() = JSONObject().apply {
        put("v", 2)
        put("round", round)
        put("computerMove", computerMove)
        put("wins", wins)
        put("losses", losses)
        put("draws", draws)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) !in 1..2) return false
        val restoredRound = json.optInt("round", -1)
        val restoredComputerMove = json.optInt("computerMove", -1)
        val restoredWins = json.optInt("wins", -1)
        val restoredLosses = json.optInt("losses", -1)
        val restoredDraws = json.optInt("draws", -1)
        val restoredFeedback = json.optString("feedback")
        if (restoredRound < 0 || restoredComputerMove !in 0..2 ||
            restoredWins < 0 || restoredLosses < 0 || restoredDraws < 0 ||
            restoredRound != restoredWins + restoredLosses + restoredDraws ||
            restoredFeedback.isBlank()
        ) return false
        round = restoredRound
        computerMove = restoredComputerMove
        wins = restoredWins
        losses = restoredLosses
        draws = restoredDraws
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
