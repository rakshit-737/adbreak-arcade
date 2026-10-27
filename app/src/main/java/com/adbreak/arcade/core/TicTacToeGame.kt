package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject

class TicTacToeGame : ArcadeGame {
    override val id = "tic_tac_toe"
    override val title = "Tic-Tac-Toe"
    override val description = "Block three in a row; a draw against perfect Arcade earns a point."

    private val board = Array(9) { ' ' }
    private var over = false
    private var result = "Your turn"
    private var feedback = "Pick a cell"
    var wins = 0
        private set
    var losses = 0
        private set
    var draws = 0
        private set
    var score = 0
        private set
    var bestStreak = 0
        private set
    private var streak = 0

    override fun reset() {
        if (!over && board.any { it != ' ' }) streak = 0
        java.util.Arrays.fill(board, ' ')
        over = false
        result = "Your turn"
        feedback = "Pick a cell"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        if (over) return false
        val index = action.ordinal - GameAction.CELL_1.ordinal
        if (index !in 0..8 || board[index] != ' ') return false

        board[index] = 'X'
        if (winner('X')) {
            wins++
            streak++
            bestStreak = maxOf(bestStreak, streak)
            score += 3
            result = "You win"
            feedback = "Great line — +3 points"
            over = true
            return true
        }
        if (board.none { it == ' ' }) {
            draws++
            streak++
            bestStreak = maxOf(bestStreak, streak)
            score++
            result = "Draw"
            feedback = "Perfect defense — +1 point · replay ready"
            over = true
            return true
        }

        val ai = bestAiMove() ?: return true
        board[ai] = 'O'
        if (winner('O')) {
            losses++
            streak = 0
            result = "Arcade wins"
            feedback = "Arcade completed three at cell ${ai + 1} — replay ready"
            over = true
        } else if (board.none { it == ' ' }) {
            draws++
            streak++
            bestStreak = maxOf(bestStreak, streak)
            score++
            result = "Draw"
            feedback = "Perfect defense — +1 point · replay ready"
            over = true
        } else {
            feedback = "Arcade played cell ${ai + 1} — your turn"
        }
        return true
    }

    private fun bestAiMove(): Int? {
        var bestIndex: Int? = null
        var bestScore = Int.MIN_VALUE
        val preferredOrder = listOf(4, 0, 2, 6, 8, 1, 3, 5, 7)
        preferredOrder.filter { board[it] == ' ' }.forEach { index ->
            board[index] = 'O'
            val moveScore = minimax(board.toList(), maximizing = false, depth = 0)
            board[index] = ' '
            if (moveScore > bestScore) {
                bestScore = moveScore
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun minimax(state: List<Char>, maximizing: Boolean, depth: Int): Int {
        if (hasWinner(state, 'O')) return 10 - depth
        if (hasWinner(state, 'X')) return depth - 10
        if (state.none { it == ' ' }) return 0

        return if (maximizing) {
            state.indices.filter { state[it] == ' ' }.maxOf { index ->
                val next = state.toMutableList()
                next[index] = 'O'
                minimax(next, maximizing = false, depth = depth + 1)
            }
        } else {
            state.indices.filter { state[it] == ' ' }.minOf { index ->
                val next = state.toMutableList()
                next[index] = 'X'
                minimax(next, maximizing = true, depth = depth + 1)
            }
        }
    }

    private fun winner(mark: Char) = hasWinner(board.toList(), mark)

    override fun render() = GameViewState(
        title,
        if (over) result else "You: X · Block O's three; a draw earns 1",
        "Score $score  ·  W $wins  L $losses  D $draws  ·  Unbeaten $streak",
        buttons = if (over) listOf("NEW GAME") else emptyList(),
        buttonActions = if (over) listOf(GameAction.NEW_GAME) else emptyList(),
        cellButtons = board.mapIndexed { i, mark -> "${i + 1}:${if (mark == ' ') "·" else mark}" },
        cellActions = (0..8).map { GameAction.values()[GameAction.CELL_1.ordinal + it] },
        feedback = feedback,
        isTerminal = over,
        phase = when {
            !over -> GamePhase.PLAYING
            result == "You win" -> GamePhase.WON
            result == "Arcade wins" -> GamePhase.LOST
            else -> GamePhase.DRAW
        },
        progress = GameProgress(board.count { it != ' ' }, 9, "Cells played"),
        record = if (over && result != "Arcade wins" && streak > 0) GameRecord(streak, "Unbeaten boards") else null,
        disabledActions = board.indices.filter { over || board[it] != ' ' }.associate {
            GameAction.values()[GameAction.CELL_1.ordinal + it] to if (over) "Board complete — start a new game" else "Cell ${it + 1} is occupied by ${board[it]}"
        },
    )

    override fun encode() = JSONObject().apply {
        put("v", 3)
        put("board", JSONArray(board.map { it.toString() }))
        put("over", over)
        put("result", result)
        put("feedback", feedback)
        put("wins", wins)
        put("losses", losses)
        put("draws", draws)
        put("score", score)
        put("bestStreak", bestStreak)
        put("streak", streak)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..3) return false
        val values = json.getJSONArray("board")
        if (values.length() != 9) return false
        val restoredBoard = List(9) { values.optString(it).firstOrNull() ?: return false }
        if (restoredBoard.any { it !in listOf(' ', 'X', 'O') }) return false

        val xCount = restoredBoard.count { it == 'X' }
        val oCount = restoredBoard.count { it == 'O' }
        if (xCount !in listOf(oCount, oCount + 1)) return false
        val xWinner = hasWinner(restoredBoard, 'X')
        val oWinner = hasWinner(restoredBoard, 'O')
        if (xWinner && oWinner) return false

        val restoredOver = json.optBoolean("over")
        val restoredResult = json.optString("result", "Your turn")
        val expectedResult = when {
            xWinner -> "You win"
            oWinner -> "Arcade wins"
            restoredBoard.none { it == ' ' } -> "Draw"
            else -> "Your turn"
        }
        if (restoredOver != (xWinner || oWinner || restoredBoard.none { it == ' ' }) || restoredResult != expectedResult) return false
        if (!restoredOver && xCount != oCount) return false
        if (xWinner && xCount != oCount + 1) return false
        if (oWinner && xCount != oCount) return false

        val restoredFeedback = if (version == 1) "Pick a cell" else json.optString("feedback")
        val restoredWins = json.optInt("wins", 0)
        val restoredLosses = json.optInt("losses", 0)
        val restoredDraws = json.optInt("draws", 0)
        val restoredScore = json.optInt("score", 0)
        val restoredBestStreak = json.optInt("bestStreak", 0)
        val restoredStreak = json.optInt("streak", 0)
        if (restoredFeedback.isBlank() || restoredWins !in 0..1_000_000 || restoredLosses !in 0..1_000_000 ||
            restoredDraws !in 0..1_000_000 || restoredScore !in 0..4_000_000 ||
            restoredBestStreak !in 0..1_000_000 || restoredStreak !in 0..restoredBestStreak) return false

        for (i in 0..8) board[i] = restoredBoard[i]
        over = restoredOver
        result = restoredResult
        feedback = restoredFeedback
        wins = restoredWins
        losses = restoredLosses
        draws = restoredDraws
        score = restoredScore
        bestStreak = restoredBestStreak
        streak = restoredStreak
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
