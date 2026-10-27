package com.adbreak.arcade.core

import org.json.JSONObject

class LightsOutMiniGame : ArcadeGame {
    override val id = "lights_out_mini"
    override val title = "Lights Out Mini"
    override val description = "Toggle a compact 3×3 board until every light is off."

    private val puzzles = listOf(listOf(0, 4, 8), listOf(0, 2, 7), listOf(1, 3, 8), listOf(2, 3, 6), listOf(1, 4, 7), listOf(0, 5, 6))
    private var puzzleIndex = 0
    private val board = CharArray(9)
    private var moves = 0
    private var wins = 0
    private var bestMoves = 0
    private var over = false
    private var feedback = "Turn every light off"

    init { reset() }

    override fun reset() {
        board.fill('0')
        puzzles[puzzleIndex].forEach { toggle(it) }
        moves = 0
        over = false
        feedback = "Press flips self + orthogonal neighbors · press again to undo · par 3"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.PRIMARY) {
            puzzleIndex = (puzzleIndex + 1) % puzzles.size
            reset()
            return true
        }
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
            feedback = "Solved in $moves moves · ${if (moves == 3) "perfect par 3" else "par 3; repeated presses cancel"}"
        } else {
            val remaining = minimumMoves(board.concatToString())
            feedback = "Pressed ${index + 1} · $remaining ${if (remaining == 1) "press" else "presses"} from dark · press again to undo"
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
        subtitle = if (over) "Solved — retry or choose a new puzzle" else "Turn all 9 lights off · puzzle ${puzzleIndex + 1}",
        scoreLine = "Moves $moves  ·  Wins $wins  ·  Best ${if (bestMoves == 0) "—" else bestMoves}",
        buttons = if (over) listOf("RETRY", "NEW PUZZLE") else listOf("NEW PUZZLE"),
        buttonActions = if (over) listOf(GameAction.NEW_GAME, GameAction.PRIMARY) else listOf(GameAction.PRIMARY),
        cellButtons = if (over) emptyList() else board.mapIndexed { index, value -> "${index + 1}:${if (value == '1') "ON" else "OFF"}" },
        cellActions = if (over) emptyList() else (0..8).map { GameAction.values()[GameAction.CELL_1.ordinal + it] },
        feedback = feedback,
        isTerminal = over,
        phase = if (over) GamePhase.WON else GamePhase.PLAYING,
        progress = GameProgress(board.count { it == '0' }, 9, "Lights off (may decrease)"),
        record = if (over) GameRecord(moves, "Moves / par-3 puzzle", higherIsBetter = false) else null,
    )

    override fun encode() = JSONObject().apply {
        put("v", 1)
        put("board", board.concatToString())
        put("puzzleIndex", puzzleIndex)
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
        val restoredPuzzleIndex = json.optInt("puzzleIndex", 0)
        val allOff = restoredBoard.length == 9 && restoredBoard.all { it == '0' }
        if (restoredBoard.length != 9 || restoredBoard.any { it !in "01" } || restoredMoves < 0 ||
            restoredWins < 0 || restoredBestMoves < 0 || restoredOver && restoredBestMoves > restoredMoves ||
            restoredFeedback.isBlank() || restoredOver != allOff ||
            (restoredWins == 0 && restoredBestMoves != 0) ||
            (restoredWins > 0 && restoredBestMoves == 0) || restoredPuzzleIndex !in puzzles.indices
        ) return false
        restoredBoard.forEachIndexed { index, value -> board[index] = value }
        moves = restoredMoves
        puzzleIndex = restoredPuzzleIndex
        wins = restoredWins
        bestMoves = restoredBestMoves
        over = restoredOver
        feedback = restoredFeedback
        true
    }.getOrDefault(false)

    companion object {
        private val pressMasks = (0..8).map { index ->
            (0..8).filter { kotlin.math.abs(it / 3 - index / 3) + kotlin.math.abs(it % 3 - index % 3) <= 1 }
                .fold(0) { mask, position -> mask or (1 shl position) }
        }
        private val distances = IntArray(512) { 10 }.apply {
            for (presses in 0..511) {
                val state = (0..8).filter { presses and (1 shl it) != 0 }.fold(0) { mask, index -> mask xor pressMasks[index] }
                this[state] = minOf(this[state], Integer.bitCount(presses))
            }
        }

        internal fun minimumMoves(state: String): Int {
            require(state.length == 9 && state.all { it in "01" })
            val mask = state.indices.filter { state[it] == '1' }.fold(0) { bits, index -> bits or (1 shl index) }
            return distances[mask]
        }
    }
}
