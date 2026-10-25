package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject

class TicTacToeGame : ArcadeGame {
    override val id = "tic_tac_toe"
    override val title = "Tic-Tac-Toe"
    override val description = "Play numbered cells against a lightweight defensive AI."

    private val board = Array(9) { ' ' }
    private var over = false
    private var result = "Your turn"

    override fun reset() { java.util.Arrays.fill(board, ' '); over = false; result = "Your turn" }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) { reset(); return true }
        if (over) return false
        val index = action.ordinal - GameAction.CELL_1.ordinal
        if (index !in 0..8 || board[index] != ' ') return false
        board[index] = 'X'
        if (winner('X')) { result = "You win"; over = true; return true }
        if (board.none { it == ' ' }) { result = "Draw"; over = true; return true }

        val ai = findWinningMove('O')
            ?: findWinningMove('X')
            ?: if (board[4] == ' ') 4
            else listOf(0, 2, 6, 8).firstOrNull { board[it] == ' ' }
            ?: board.indices.firstOrNull { board[it] == ' ' }
            ?: return true
        board[ai] = 'O'
        if (winner('O')) { result = "Arcade wins"; over = true }
        else if (board.none { it == ' ' }) { result = "Draw"; over = true }
        return true
    }

    private fun findWinningMove(mark: Char): Int? = board.indices.firstOrNull { index ->
        board[index] == ' ' && board.toList().toMutableList().also { it[index] = mark }.let { hasWinner(it, mark) }
    }

    private fun winner(mark: Char) = hasWinner(board.toList(), mark)

    override fun render() = GameViewState(
        title,
        result,
        board.joinToString(" ") { if (it == ' ') "·" else it.toString() },
        cellButtons = board.mapIndexed { i, mark -> "${i + 1}:${if (mark == ' ') "·" else mark}" },
        cellActions = (0..8).map { GameAction.values()[GameAction.CELL_1.ordinal + it] }
    )

    override fun encode() = JSONObject().apply {
        put("v", 1); put("board", JSONArray(board.map { it.toString() }))
        put("over", over); put("result", result)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != 1) return false
        val values = json.getJSONArray("board")
        if (values.length() != 9) return false
        val restoredBoard = List(9) { values.optString(it).firstOrNull() ?: return false }
        if (restoredBoard.any { it !in listOf(' ', 'X', 'O') }) return false
        val xCount = restoredBoard.count { it == 'X' }
        val oCount = restoredBoard.count { it == 'O' }
        if (xCount !in oCount..(oCount + 1)) return false
        val xWinner = hasWinner(restoredBoard, 'X')
        val oWinner = hasWinner(restoredBoard, 'O')
        if (xWinner && oWinner) return false
        val restoredOver = json.optBoolean("over")
        val restoredResult = json.optString("result", "Your turn")
        if (!restoredOver && (xWinner || oWinner || restoredBoard.none { it == ' ' })) return false
        for (i in 0..8) board[i] = restoredBoard[i]
        over = restoredOver; result = restoredResult
        true
    }.getOrDefault(false)

    companion object {
        fun hasWinner(board: List<Char>, mark: Char): Boolean {
            if (board.size != 9) return false
            return listOf(
                intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
                intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
                intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
            ).any { line -> line.all { board[it] == mark } }
        }
    }
}
