package com.adbreak.arcade.core

import org.json.JSONObject

class LightsOutMiniGame : ArcadeGame {
    override val id = "lights_out_mini"
    override val title = "Lights Out Mini"
    override val description = "Toggle a compact 3×3 board until every light is off."

    private val initialBoard = "100010001"
    private val board = CharArray(9)
    private var moves = 0
    private var wins = 0
    private var bestMoves = 0
    private var over = false
    private var feedback = "Turn every light off"

    init { reset() }

    override fun reset() {
        initialBoard.forEachIndexed { index, value -> board[index] = value }
        moves = 0
        over = false
        feedback = "Turn every light off"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        if (over) return false
        val index = action.ordinal - GameAction.CELL_1.ordinal
        if (index !in 0..8) return false
        toggle(index)
        moves++
        if (board.all { it == '0' }) {
            wins++
            bestMoves = if (bestMoves == 0) moves else minOf(bestMoves, moves)
            over = true
            feedback = "Solved in $moves moves"
        } else {
            feedback = "Keep going — $moves moves"
        }
        return true
    }

    private fun toggle(index: Int) {
        val affected = listOf(index, index - 3, index + 3, index - 1, index + 1)
            .filter { it in 0..8 && (it / 3 == index / 3 || it == index || it / 3 != index / 3 && it % 3 == index % 3) }
        affected.forEach { position -> board[position] = if (board[position] == '1') '0' else '1' }
    }

    override fun render(): GameViewState = GameViewState(
        title = title,
        subtitle = if (over) "Solved — reset for another run" else "Turn every light off",
        scoreLine = "Moves $moves  ·  Wins $wins  ·  Best ${if (bestMoves == 0) "—" else bestMoves}",
        cellButtons = board.mapIndexed { index, value -> "${index + 1}:${if (value == '1') "ON" else "·"}" },
        cellActions = (0..8).map { GameAction.values()[GameAction.CELL_1.ordinal + it] },
        feedback = feedback,
        isTerminal = over
    )

    override fun encode() = JSONObject().apply {
        put("v", 1)
        put("board", board.concatToString())
        put("moves", moves)
        put("wins", wins)
        put("bestMoves", bestMoves)
        put("over", over)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != 1) return false
        val restoredBoard = json.optString("board")
        val restoredMoves = json.optInt("moves", -1)
        val restoredWins = json.optInt("wins", -1)
        val restoredBestMoves = json.optInt("bestMoves", -1)
        val restoredOver = json.optBoolean("over")
        val restoredFeedback = json.optString("feedback")
        val allOff = restoredBoard.length == 9 && restoredBoard.all { it == '0' }
        if (restoredBoard.length != 9 || restoredBoard.any { it !in "01" } || restoredMoves < 0 ||
            restoredWins < 0 || restoredBestMoves < 0 || restoredOver && restoredBestMoves > restoredMoves ||
            restoredFeedback.isBlank() || restoredOver != allOff ||
            (restoredWins == 0 && restoredBestMoves != 0) ||
            (restoredWins > 0 && restoredBestMoves == 0)
        ) return false
        restoredBoard.forEachIndexed { index, value -> board[index] = value }
        moves = restoredMoves
        wins = restoredWins
        bestMoves = restoredBestMoves
        over = restoredOver
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}
