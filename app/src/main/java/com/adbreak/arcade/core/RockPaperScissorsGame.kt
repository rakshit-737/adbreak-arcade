package com.adbreak.arcade.core

import org.json.JSONObject
import java.util.Random

class RockPaperScissorsGame(private val random: Random = Random()) : ArcadeGame {
    override val id = "rock_paper_scissors"
    override val title = "Rock Paper Scissors"
    override val description = "Play five fair, random throws. Each arcade move is locked before you choose."

    private val moves = listOf("Rock", "Paper", "Scissors")
    private var round = 0
    private var computerMove = random.nextInt(3)
    private var movePlan = computerMove.toString() + (1..4).joinToString("") { random.nextInt(3).toString() }
    private var bounded = true
    private var wins = 0
    private var losses = 0
    private var draws = 0
    private var feedback = "Random opponent · move already locked · draws use a throw"

    override fun reset() {
        round = 0
        computerMove = random.nextInt(3)
        movePlan = computerMove.toString() + (1..4).joinToString("") { random.nextInt(3).toString() }
        bounded = true
        wins = 0
        losses = 0
        draws = 0
        feedback = "Random opponent · move already locked · draws use a throw"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        if (isOver()) return false
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
                feedback = "Draw — both chose ${moves[playerMove]}"
            }
            1 -> {
                wins++
                feedback = "You win — ${rule(playerMove)}"
            }
            else -> {
                losses++
                feedback = "Arcade wins — ${rule(computerMove)}"
            }
        }
        round++
        if (!bounded) computerMove = random.nextInt(3)
        else if (!isOver()) computerMove = movePlan[round].digitToInt()
        return true
    }

    private fun isOver() = bounded && round >= 5

    private fun rule(winner: Int) = listOf("Rock breaks Scissors", "Paper covers Rock", "Scissors cut Paper")[winner]

    override fun render() = GameViewState(
        title = title,
        subtitle = if (isOver()) "Five throws complete · ${if (wins > losses) "You win" else if (wins < losses) "Arcade wins" else "Tied"}" else "Throw ${if (bounded) "${round + 1}/5" else "${round + 1}"} · choose a move",
        scoreLine = "Wins $wins  ·  Losses $losses  ·  Draws $draws",
        buttons = if (isOver()) listOf("NEW MATCH") else moves,
        buttonActions = if (isOver()) listOf(GameAction.NEW_GAME) else listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1),
        feedback = feedback,
        isTerminal = isOver(),
        phase = if (!isOver()) GamePhase.PLAYING else if (wins > losses) GamePhase.WON else if (wins < losses) GamePhase.LOST else GamePhase.DRAW,
        progress = if (bounded) GameProgress(round, 5, "Throws played") else null,
        record = if (isOver()) GameRecord(wins, "Wins / 5 throws") else null
    )

    override fun encode() = JSONObject().apply {
        put("v", 2)
        put("round", round)
        put("computerMove", computerMove)
        put("movePlan", movePlan)
        put("bounded", bounded)
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
        val restoredBounded = json.optBoolean("bounded", false)
        val restoredPlan = json.optString("movePlan", "")
        if (restoredRound < 0 || restoredComputerMove !in 0..2 ||
            restoredWins < 0 || restoredLosses < 0 || restoredDraws < 0 ||
            restoredRound != restoredWins + restoredLosses + restoredDraws ||
            restoredFeedback.isBlank()
        ) return false
        if (restoredBounded && (restoredRound > 5 || restoredPlan.length != 5 ||
                restoredPlan.any { it !in '0'..'2' } ||
                restoredPlan[minOf(restoredRound, 4)].digitToInt() != restoredComputerMove)) return false
        round = restoredRound
        bounded = restoredBounded
        movePlan = restoredPlan
        computerMove = restoredComputerMove
        wins = restoredWins
        losses = restoredLosses
        draws = restoredDraws
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
