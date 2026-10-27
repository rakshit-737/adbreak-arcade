package com.adbreak.arcade.core

import org.json.JSONObject

class NimGame : ArcadeGame {
    override val id = "nim"
    override val title = "Nim"
    override val description = "Take 1–3 stones; taking the last wins. Leave multiples of four to control the reply."

    private var startingStones = 13
    private var stones = startingStones
    private var wins = 0
    private var losses = 0
    private var moves = 0
    private var over = false
    private var feedback = "Take 1–3 · last stone wins · aim to leave 4, 8, or 12"

    override fun reset() {
        startingStones = if (startingStones == 15) 13 else startingStones + 1
        stones = startingStones
        wins = 0
        losses = 0
        moves = 0
        over = false
        feedback = "Take 1–3 · last stone wins · aim to leave 4, 8, or 12"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        if (over) return false
        val amount = when (action) {
            GameAction.PRIMARY -> 1
            GameAction.SECONDARY -> 2
            GameAction.CELL_1 -> 3
            else -> return false
        }
        if (amount > stones) return false
        stones -= amount
        moves++
        if (stones == 0) {
            wins++
            over = true
            feedback = "You took the last stone — you win"
            return true
        }

        val computerAmount = if (stones <= 3) stones else if (stones % 4 == 0) 1 else stones % 4
        val leftSafePile = stones % 4 == 0
        stones -= computerAmount
        if (stones == 0) {
            losses++
            over = true
            feedback = "Arcade took the last $computerAmount — arcade wins. Leave a multiple of 4 next time"
        } else {
            feedback = "You took $amount · arcade took $computerAmount. ${if (leftSafePile) "Good: you left a multiple of 4" else "Arcade now controls the multiples of 4"}"
        }
        return true
    }

    override fun render(): GameViewState {
        if (over) {
            return GameViewState(
                title = title,
                subtitle = feedback,
                scoreLine = "Wins $wins  ·  Losses $losses  ·  Moves $moves",
                buttons = listOf("NEW ROUND"),
                buttonActions = listOf(GameAction.NEW_GAME),
                feedback = feedback,
                isTerminal = true,
                phase = if (wins > 0) GamePhase.WON else GamePhase.LOST,
                progress = GameProgress(startingStones, startingStones, "Stones removed"),
                record = GameRecord(wins, "Round won / 1")
            )
        }
        val amounts = (1..3)
        return GameViewState(
            title = title,
            subtitle = "$stones stones remain · take the last to win",
            scoreLine = "Wins $wins  ·  Losses $losses  ·  Moves $moves",
            buttons = amounts.map { "TAKE $it" },
            buttonActions = amounts.map { amount ->
                when (amount) {
                    1 -> GameAction.PRIMARY
                    2 -> GameAction.SECONDARY
                    else -> GameAction.CELL_1
                }
            },
            feedback = feedback,
            progress = GameProgress(startingStones - stones, startingStones, "Stones removed"),
            disabledActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1)
                .mapIndexedNotNull { index, action -> if (index + 1 > stones) action to "Only $stones stones remain" else null }.toMap()
        )
    }

    override fun encode() = JSONObject().apply {
        put("v", 1)
        put("stones", stones)
        put("startingStones", startingStones)
        put("wins", wins)
        put("losses", losses)
        put("moves", moves)
        put("over", over)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != 1) return false
        val restoredStones = json.optInt("stones", -1)
        val restoredWins = json.optInt("wins", -1)
        val restoredLosses = json.optInt("losses", -1)
        val restoredMoves = json.optInt("moves", -1)
        val restoredOver = json.optBoolean("over")
        val restoredFeedback = json.optString("feedback")
        val restoredStart = json.optInt("startingStones", 13)
        if (restoredStart !in 13..15 || restoredStones !in 0..restoredStart || restoredWins !in 0..1 || restoredLosses !in 0..1 ||
            restoredMoves < 0 || restoredFeedback.isBlank() || restoredOver != (restoredStones == 0) ||
            restoredWins + restoredLosses != (if (restoredOver) 1 else 0) ||
            restoredWins + restoredLosses > restoredMoves
        ) return false
        stones = restoredStones
        startingStones = restoredStart
        wins = restoredWins
        losses = restoredLosses
        moves = restoredMoves
        over = restoredOver
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
