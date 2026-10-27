package com.adbreak.arcade.core

import org.json.JSONObject
import java.util.ArrayDeque

/**
 * A compact, deterministic tactics encounter designed for notification-sized controls.
 * Move onto adjacent cells, attack an adjacent drone, or defend against its telegraphed
 * strike. The fixed obstacle and core make every round readable and replayable.
 */
class NeonTacticsGame : ArcadeGame {
    override val id = "neon_tactics"
    override val title = "Neon Tactics"
    override val description = "Reach the neon core, read the drone's intent, and survive the grid."

    companion object {
        const val GRID_SIZE = 3
        const val CELL_COUNT = GRID_SIZE * GRID_SIZE
        const val OBJECTIVE_CELL = 6
        const val OBSTACLE_CELL = 4
        const val START_CELL = 0
        const val ENEMY_START_CELL = 8
        const val MAX_PLAYER_HP = 6
        const val MAX_ENEMY_HP = 2
        const val ENEMY_DAMAGE = 2
        const val DEFEND_REDUCTION = 1
        const val DATA_CELL = 2

        private const val MAX_COUNTER = 1_000_000
        private const val VERSION = 2

        /** Maps a zero-based board cell to the shared action set. */
        fun actionForCell(index: Int): GameAction? =
            if (index in 0 until CELL_COUNT) {
                GameAction.values()[GameAction.CELL_1.ordinal + index]
            } else {
                null
            }
    }

    private enum class EnemyIntent {
        ADVANCE,
        STRIKE,
        DISABLED,
    }

    var playerCell: Int = START_CELL
        private set
    var enemyCell: Int = ENEMY_START_CELL
        private set
    var playerHp: Int = MAX_PLAYER_HP
        private set
    var enemyHp: Int = MAX_ENEMY_HP
        private set
    var turn: Int = 0
        private set
    var score: Int = 0
        private set
    var wins: Int = 0
        private set
    var losses: Int = 0
        private set
    var bestScore: Int = 0
        private set

    private var over = false
    private var won = false
    private var dataRecovered = false
    private var feedback = "Reach CORE at cell 7 · adjacent ENEMY cells are attack targets"

    init {
        reset()
    }

    private val terminal: Boolean
        get() = over

    private val intent: EnemyIntent
        get() = when {
            enemyHp <= 0 -> EnemyIntent.DISABLED
            areAdjacent(playerCell, enemyCell) -> EnemyIntent.STRIKE
            else -> EnemyIntent.ADVANCE
        }

    override fun reset() {
        playerCell = START_CELL
        enemyCell = ENEMY_START_CELL
        playerHp = MAX_PLAYER_HP
        enemyHp = MAX_ENEMY_HP
        turn = 0
        score = 0
        over = false
        won = false
        dataRecovered = false
        feedback = "CORE at 7 wins · optional DATA at 3 +20 · disable DRONE +30"
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            reset()
            return true
        }
        if (terminal) return false

        // Capture the telegraph before the player's move. A player can evade a strike
        // by moving away, but the drone never gets a surprise change of intent.
        val telegraphedIntent = intent
        return when (action) {
            GameAction.PRIMARY -> defend(telegraphedIntent)
            else -> {
                val index = cellIndex(action) ?: return false
                resolveCell(index, telegraphedIntent)
            }
        }
    }

    private fun defend(telegraphedIntent: EnemyIntent): Boolean {
        finishPlayerTurn(
            actionScore = 0,
            actionFeedback = "Defend raised · incoming strike reduced by $DEFEND_REDUCTION",
            telegraphedIntent = telegraphedIntent,
            defending = true,
        )
        return true
    }

    private fun resolveCell(index: Int, telegraphedIntent: EnemyIntent): Boolean {
        if (index !in 0 until CELL_COUNT || index == OBSTACLE_CELL || index == playerCell) return false

        if (index == enemyCell && enemyHp > 0) {
            if (!areAdjacent(playerCell, enemyCell)) return false
            enemyHp--
            finishPlayerTurn(
                actionScore = 15,
                actionFeedback = if (enemyHp == 0) {
                    "Direct hit · DRONE disabled · reach CORE"
                } else {
                    "Direct hit · DRONE integrity $enemyHp/$MAX_ENEMY_HP"
                },
                telegraphedIntent = telegraphedIntent,
                defending = false,
            )
            return true
        }

        if (!areAdjacent(playerCell, index)) return false

        playerCell = index
        val recoveredNow = index == DATA_CELL && !dataRecovered
        if (recoveredNow) dataRecovered = true
        if (playerCell == OBJECTIVE_CELL) {
            turn = (turn + 1).coerceAtMost(MAX_COUNTER)
            score += extractionReward(playerHp, turn)
            bestScore = maxOf(bestScore, score)
            over = true
            won = true
            wins = (wins + 1).coerceAtMost(MAX_COUNTER)
            feedback = "CORE secured · ${if (dataRecovered) "DATA recovered" else "DATA skipped"} · score $score · replay"
            return true
        }

        finishPlayerTurn(
            actionScore = if (recoveredNow) 20 else 0,
            actionFeedback = if (recoveredNow) "DATA recovered · +20 · reach CORE" else "Moved to cell ${index + 1}",
            telegraphedIntent = telegraphedIntent,
            defending = false,
        )
        return true
    }

    private fun finishPlayerTurn(
        actionScore: Int,
        actionFeedback: String,
        telegraphedIntent: EnemyIntent,
        defending: Boolean,
    ) {
        turn = (turn + 1).coerceAtMost(MAX_COUNTER)
        score += actionScore
        bestScore = maxOf(bestScore, score)
        feedback = actionFeedback

        if (enemyHp > 0) {
            resolveEnemyTurn(telegraphedIntent, defending)
        }
        if (!over && playerHp <= 0) {
            over = true
            won = false
            losses = (losses + 1).coerceAtMost(MAX_COUNTER)
            feedback = "Signal lost · the DRONE overwhelmed you · replay"
        }
    }

    private fun resolveEnemyTurn(telegraphedIntent: EnemyIntent, defending: Boolean) {
        when (telegraphedIntent) {
            EnemyIntent.DISABLED -> Unit
            EnemyIntent.STRIKE -> {
                if (areAdjacent(playerCell, enemyCell)) {
                    val damage = (ENEMY_DAMAGE - if (defending) DEFEND_REDUCTION else 0).coerceAtLeast(0)
                    playerHp = (playerHp - damage).coerceAtLeast(0)
                    feedback += if (damage == 0) {
                        " · strike blocked"
                    } else {
                        " · strike -$damage HP"
                    }
                } else {
                    feedback += " · strike missed"
                }
            }
            EnemyIntent.ADVANCE -> {
                val next = nextStepToward(enemyCell, playerCell)
                if (next != null && next != playerCell) {
                    enemyCell = next
                    feedback += " · DRONE advances to cell ${enemyCell + 1}"
                } else {
                    feedback += " · DRONE holds"
                }
            }
        }
    }

    private fun nextStepToward(from: Int, target: Int): Int? {
        if (from == target) return null
        val distances = IntArray(CELL_COUNT) { Int.MAX_VALUE }
        val queue = ArrayDeque<Int>()
        distances[target] = 0
        queue.addLast(target)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            neighbors(current).forEach { candidate ->
                if (candidate != OBSTACLE_CELL && distances[candidate] == Int.MAX_VALUE) {
                    distances[candidate] = distances[current] + 1
                    queue.addLast(candidate)
                }
            }
        }

        return neighbors(from)
            .filter { it != OBSTACLE_CELL && it != playerCell }
            .minWithOrNull(compareBy<Int> { distances[it] }.thenBy { it })
            ?.takeIf { distances[it] != Int.MAX_VALUE }
    }

    private fun neighbors(index: Int): List<Int> {
        val row = index / GRID_SIZE
        val column = index % GRID_SIZE
        return listOf(
            if (row > 0) index - GRID_SIZE else -1,
            if (column > 0) index - 1 else -1,
            if (row < GRID_SIZE - 1) index + GRID_SIZE else -1,
            if (column < GRID_SIZE - 1) index + 1 else -1,
        ).filter { it >= 0 }
    }

    private fun areAdjacent(first: Int, second: Int): Boolean =
        kotlin.math.abs(first / GRID_SIZE - second / GRID_SIZE) +
            kotlin.math.abs(first % GRID_SIZE - second % GRID_SIZE) == 1

    private fun cellIndex(action: GameAction): Int? {
        val index = action.ordinal - GameAction.CELL_1.ordinal
        return index.takeIf { it in 0 until CELL_COUNT }
    }

    override fun render(): GameViewState {
        val currentIntent = intent
        val subtitle = when {
            won -> "CORE secured · replay to run it again"
            !over -> "CORE 7 wins · ${intentText(currentIntent)}"
            else -> "Signal lost · replay to try the encounter again"
        }
        val buttons = if (terminal) listOf("REPLAY") else listOf("DEFEND")
        val actions = if (terminal) listOf(GameAction.NEW_GAME) else listOf(GameAction.PRIMARY)
        return GameViewState(
            title = title,
            subtitle = subtitle,
            scoreLine = "HP $playerHp/$MAX_PLAYER_HP · DRONE $enemyHp/$MAX_ENEMY_HP · DATA 3 ${if (dataRecovered) "secured" else "+20 optional"} · Turn $turn · Score $score",
            buttons = buttons,
            buttonActions = actions,
            cellButtons = (0 until CELL_COUNT).map { cellLabel(it) },
            cellActions = (0 until CELL_COUNT).mapNotNull { actionForCell(it) },
            feedback = feedback,
            isTerminal = terminal,
            phase = when {
                won -> GamePhase.WON
                terminal -> GamePhase.LOST
                else -> GamePhase.PLAYING
            },
            progress = GameProgress(if (won) 1 else 0, 1, "Secure CORE · extract healthy and quickly for bonus"),
            record = score.takeIf { it > 0 }?.let { GameRecord(it, "Tactical score") },
            disabledActions = buildMap {
                (0 until CELL_COUNT).forEach { cell ->
                    val reason = when {
                        terminal -> "Encounter finished — replay"
                        cell == OBSTACLE_CELL -> "Blocked cell"
                        cell == playerCell -> "You are here"
                        !areAdjacent(playerCell, cell) -> "Choose an adjacent cell"
                        else -> null
                    }
                    if (reason != null) put(actionForCell(cell)!!, reason)
                }
            },
        )
    }

    private fun cellLabel(index: Int): String = when {
        index == OBSTACLE_CELL -> "${index + 1}:BLOCK"
        index == playerCell && index == OBJECTIVE_CELL -> "${index + 1}:YOU/CORE"
        index == playerCell -> "${index + 1}:YOU"
        index == enemyCell && enemyHp > 0 -> "${index + 1}:DRONE($enemyHp)"
        index == OBJECTIVE_CELL -> "${index + 1}:CORE"
        index == DATA_CELL && !dataRecovered -> "${index + 1}:DATA"
        else -> "${index + 1}:·"
    }

    private fun intentText(value: EnemyIntent): String = when (value) {
        EnemyIntent.ADVANCE -> "INTENT: ADVANCE 1 toward you (no hit)"
        EnemyIntent.STRIKE -> "INTENT: STRIKE for $ENEMY_DAMAGE"
        EnemyIntent.DISABLED -> "INTENT: DISABLED"
    }

    override fun encode(): String = JSONObject().apply {
        put("v", VERSION)
        put("playerCell", playerCell)
        put("enemyCell", enemyCell)
        put("playerHp", playerHp)
        put("enemyHp", enemyHp)
        put("turn", turn)
        put("score", score)
        put("wins", wins)
        put("losses", losses)
        put("bestScore", bestScore)
        put("over", over)
        put("won", won)
        put("dataRecovered", dataRecovered)
        put("intent", intent.name)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..VERSION) return false

        val restoredPlayerCell = json.optInt("playerCell", -1)
        val restoredEnemyCell = json.optInt("enemyCell", -1)
        val restoredPlayerHp = json.optInt("playerHp", -1)
        val restoredEnemyHp = json.optInt("enemyHp", -1)
        val restoredTurn = json.optInt("turn", -1)
        val restoredScore = json.optInt("score", -1)
        val restoredWins = json.optInt("wins", -1)
        val restoredLosses = json.optInt("losses", -1)
        val restoredBestScore = json.optInt("bestScore", -1)
        val restoredOver = json.optBoolean("over", false)
        val restoredWon = json.optBoolean("won", false)
        val restoredIntent = json.optString("intent")
        val restoredFeedback = json.optString("feedback")
        val restoredData = if (version >= 2) json.optBoolean("dataRecovered", false) else false
        val expectedIntent = when {
            restoredEnemyHp <= 0 -> EnemyIntent.DISABLED.name
            areAdjacent(restoredPlayerCell, restoredEnemyCell) -> EnemyIntent.STRIKE.name
            else -> EnemyIntent.ADVANCE.name
        }

        if (restoredPlayerCell !in 0 until CELL_COUNT ||
            restoredEnemyCell !in 0 until CELL_COUNT ||
            restoredPlayerCell == OBSTACLE_CELL ||
            restoredEnemyCell == OBSTACLE_CELL ||
            restoredPlayerCell == restoredEnemyCell && restoredEnemyHp > 0 ||
            restoredPlayerHp !in 0..MAX_PLAYER_HP ||
            restoredEnemyHp !in 0..MAX_ENEMY_HP ||
            restoredTurn !in 0..MAX_COUNTER ||
            restoredScore !in 0..MAX_COUNTER ||
            restoredWins !in 0..MAX_COUNTER ||
            restoredLosses !in 0..MAX_COUNTER ||
            restoredBestScore !in 0..MAX_COUNTER ||
            restoredBestScore < restoredScore ||
            restoredFeedback.isBlank() ||
            restoredIntent != expectedIntent ||
            restoredWon && !restoredOver ||
            restoredOver != (restoredWon || restoredPlayerHp == 0) ||
            restoredWon != (restoredPlayerCell == OBJECTIVE_CELL) ||
            !restoredOver && restoredPlayerCell == OBJECTIVE_CELL
        ) return false

        // V1 rewarded unlimited movement/defending. Retain the encounter, but rebuild
        // its record from finite accomplishments rather than importing farmed points.
        val earnedScore = (MAX_ENEMY_HP - restoredEnemyHp) * 15 +
            (if (restoredData) 20 else 0) +
            (if (restoredWon) extractionReward(restoredPlayerHp, restoredTurn) else 0)
        if (version >= 2 && (restoredScore != earnedScore || restoredBestScore > 107)) return false

        playerCell = restoredPlayerCell
        enemyCell = restoredEnemyCell
        playerHp = restoredPlayerHp
        enemyHp = restoredEnemyHp
        turn = restoredTurn
        score = earnedScore
        wins = restoredWins
        losses = restoredLosses
        bestScore = if (version == 1) earnedScore else restoredBestScore
        over = restoredOver
        won = restoredWon
        dataRecovered = restoredData
        feedback = restoredFeedback
        true
    }.getOrDefault(false)

    private fun extractionReward(hp: Int, turns: Int): Int =
        25 + hp * 2 + (10 - turns).coerceAtLeast(0) * 2
}
