package com.adbreak.arcade.core

import org.json.JSONObject

class ConnectThreeGame : ArcadeGame {
    override val id = "connect_three"
    override val title = "Connect Three"
    override val description = "Drop X tokens into columns and connect three before the arcade does."

    private val board = CharArray(9) { '.' }
    private var moves = 0
    private var over = false
    private var opening = 0
    private var feedback = "You are X · tokens fall to the lowest empty space"

    override fun reset() {
        java.util.Arrays.fill(board, '.')
        moves = 0
        over = false
        opening = (opening + 1) % 3
        feedback = "You are X · tokens fall to the lowest empty space"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        if (over) return false
        val column = action.ordinal - GameAction.CELL_1.ordinal
        if (column !in 0..2) return false
        val playerRow = findOpenRow(column) ?: return false
        board[playerRow * 3 + column] = 'X'
        moves++
        if (hasWinner(board.concatToString(), 'X')) {
            over = true
            feedback = "You connected three"
            return true
        }
        if (board.none { it == '.' }) {
            over = true
            feedback = "Draw"
            return true
        }

        val winningReply = findWinningColumn('O')
        val blockingReply = findWinningColumn('X')
        val computerColumn = winningReply ?: blockingReply ?: chooseComputerColumn()
        val computerRow = findOpenRow(computerColumn)
        if (computerRow != null) board[computerRow * 3 + computerColumn] = 'O'
        if (hasWinner(board.concatToString(), 'O')) {
            over = true
            feedback = "Arcade connected three · last drop in column ${computerColumn + 1}"
        } else if (board.none { it == '.' }) {
            over = true
            feedback = "Draw"
        } else {
            feedback = if (blockingReply != null) "Arcade blocked column ${computerColumn + 1} · your turn" else "Arcade dropped in column ${computerColumn + 1} · your turn"
            findWinningColumn('O')?.let { feedback += " · Threat in column ${it + 1}" }
        }
        return true
    }

    private fun findOpenRow(column: Int): Int? = (2 downTo 0).firstOrNull { board[it * 3 + column] == '.' }

    private fun chooseComputerColumn(): Int {
        findWinningColumn('O')?.let { return it }
        findWinningColumn('X')?.let { return it }
        val preference = listOf(listOf(1, 0, 2), listOf(0, 2, 1), listOf(2, 1, 0))[opening]
        preference.firstOrNull { findOpenRow(it) != null }?.let { return it }
        return 0
    }

    private fun findWinningColumn(mark: Char): Int? = (0..2).firstOrNull { column ->
        val row = findOpenRow(column) ?: return@firstOrNull false
        board[row * 3 + column] = mark
        val wins = hasWinner(board.concatToString(), mark)
        board[row * 3 + column] = '.'
        wins
    }

    override fun render(): GameViewState {
        val boardText = (0..2).joinToString(" / ") { row ->
            board.slice(row * 3 until row * 3 + 3).joinToString("")
        }
        val phase = when {
            !over -> GamePhase.PLAYING
            hasWinner(board.concatToString(), 'X') -> GamePhase.WON
            hasWinner(board.concatToString(), 'O') -> GamePhase.LOST
            else -> GamePhase.DRAW
        }
        val actions = listOf(GameAction.CELL_1, GameAction.CELL_2, GameAction.CELL_3)
        val bestLine = lines.filter { line -> line.none { board[it] == 'O' } }
            .maxOfOrNull { line -> line.count { board[it] == 'X' } } ?: 0
        return GameViewState(
            title = title,
            subtitle = if (over) feedback else "Connect 3 X · row, column, or diagonal",
            scoreLine = "Moves $moves  ·  $boardText",
            buttons = if (over) listOf("NEW ROUND") else emptyList(),
            buttonActions = if (over) listOf(GameAction.NEW_GAME) else emptyList(),
            cellButtons = if (over) emptyList() else listOf("COL 1", "COL 2", "COL 3"),
            cellActions = if (over) emptyList() else actions,
            feedback = feedback,
            isTerminal = over,
            phase = phase,
            progress = GameProgress(bestLine, 3, "X tokens in best unblocked line"),
            record = if (over) GameRecord(if (phase == GamePhase.WON) 2 else if (phase == GamePhase.DRAW) 1 else 0, "Result: win 2 / draw 1") else null,
            disabledActions = if (over) emptyMap() else (0..2).filter { findOpenRow(it) == null }
                .associate { actions[it] to "Column ${it + 1} is full" }
        )
    }

    override fun encode() = JSONObject().apply {
        put("v", 1)
        put("board", board.concatToString())
        put("moves", moves)
        put("opening", opening)
        put("over", over)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != 1) return false
        val restoredBoard = json.optString("board")
        val restoredMoves = json.optInt("moves", -1)
        val restoredOver = json.optBoolean("over")
        val restoredFeedback = json.optString("feedback")
        val restoredOpening = json.optInt("opening", 0)
        if (restoredBoard.length != 9 || restoredBoard.any { it !in ".XO" } ||
            !hasGravity(restoredBoard) || restoredMoves < 0 || restoredFeedback.isBlank() || restoredOpening !in 0..2
        ) return false
        val xCount = restoredBoard.count { it == 'X' }
        val oCount = restoredBoard.count { it == 'O' }
        val xWinner = hasWinner(restoredBoard, 'X')
        val oWinner = hasWinner(restoredBoard, 'O')
        if (xWinner && oWinner || xCount < oCount || xCount > oCount + 1) return false
        val terminal = xWinner || oWinner || !restoredBoard.contains('.')
        if (restoredOver != terminal || (!restoredOver && xCount != oCount)) return false
        if (xWinner && xCount != oCount + 1 || oWinner && xCount != oCount) return false
        val expectedMoves = if (xCount == oCount + 1) (xCount + oCount + 1) / 2 else (xCount + oCount) / 2
        if (restoredMoves != expectedMoves) return false
        restoredBoard.forEachIndexed { index, value -> board[index] = value }
        moves = restoredMoves
        opening = restoredOpening
        over = restoredOver
        feedback = restoredFeedback
        true
    }.getOrDefault(false)

    private fun hasGravity(state: String): Boolean {
        for (column in 0..2) {
            var foundEmpty = false
            for (row in 2 downTo 0) {
                when (state[row * 3 + column]) {
                    '.' -> foundEmpty = true
                    else -> if (foundEmpty) return false
                }
            }
        }
        return true
    }

    companion object {
        private val lines = listOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )

        fun hasWinner(state: String, mark: Char): Boolean {
            if (state.length != 9 || mark !in "XO") return false
            return lines.any { line -> line.all { state[it] == mark } }
        }
    }
}
