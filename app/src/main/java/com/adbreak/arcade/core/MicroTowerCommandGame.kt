package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

/**
 * A small, turn-based tower defence game designed for the notification controls.
 *
 * The nine cells are a fixed zig-zag command strip: path cells are even numbered
 * cells and build pads are the cells between them. PRIMARY changes the selected
 * tower, SECONDARY ends the turn, and a build-pad cell places or upgrades a tower.
 */
class MicroTowerCommandGame : ArcadeGame {
    override val id = "micro_tower_command"
    override val title = "Micro Tower Command"
    override val description = "Place dart and frost towers, then command deterministic waves before they reach the base."

    private enum class TowerType(val code: String, val label: String, val buildCost: Int) {
        DART("D", "DART", 4),
        FROST("F", "FROST", 5),
    }

    private enum class EnemyType(
        val code: String,
        val label: String,
        val maxHealth: Int,
        val reward: Int,
        val baseDamage: Int,
    ) {
        GRUNT("G", "GRUNT", 2, 2, 1),
        RUNNER("R", "RUNNER", 1, 2, 1),
        BRUTE("B", "BRUTE", 6, 3, 2),
    }

    private enum class Result { PLAYING, WON, LOST }

    private data class Tower(val type: TowerType, var level: Int)

    private data class Enemy(
        val id: Int,
        val type: EnemyType,
        var health: Int,
        var position: Int,
        var slowed: Boolean,
    )

    companion object {
        private const val STATE_VERSION = 1
        private const val TOTAL_CELLS = 9
        private const val PATH_LENGTH = 5
        private const val MAX_WAVES = 3
        private const val MAX_BASE_HEALTH = 6
        private const val STARTING_GOLD = 12
        private const val MAX_GOLD = 99
        private const val MAX_TOWER_LEVEL = 3
        private const val MAX_TURNS = 10_000

        /** Zero-based cells occupied by the five-cell path. */
        val PATH_CELLS: List<Int> = listOf(0, 2, 4, 6, 8)

        /** Zero-based cells where a tower may be placed. */
        val BUILD_CELLS: List<Int> = listOf(1, 3, 5, 7)
    }

    private var gold = STARTING_GOLD
    private var baseHealth = MAX_BASE_HEALTH
    private var wave = 1
    private var turns = 0
    private var nextEnemyId = 1
    private var selectedType = TowerType.DART
    private var result = Result.PLAYING
    private var feedback = "Build a tower, then end turn"
    private var towers = arrayOfNulls<Tower>(TOTAL_CELLS)
    private var enemies = mutableListOf<Enemy>()
    private var pending = mutableListOf<EnemyType>()
    private var roundsWon = 0
    private var roundsLost = 0

    init {
        resetRound()
    }

    /** Current spendable command points. */
    val resources: Int
        get() = gold

    /** Current base health. */
    val health: Int
        get() = baseHealth

    /** Current wave, from one through [MAX_WAVES]. */
    val currentWave: Int
        get() = wave

    /** Number of resolved turns in the current round. */
    val turnCount: Int
        get() = turns

    /** The selected tower type used by the next placement. */
    val selectedTower: String
        get() = selectedType.name

    /** Number of towers currently on the command strip. */
    val towerCount: Int
        get() = towers.count { it != null }

    override fun reset() {
        resetRound()
    }

    private fun resetRound() {
        gold = STARTING_GOLD
        baseHealth = MAX_BASE_HEALTH
        wave = 1
        turns = 0
        nextEnemyId = 1
        selectedType = TowerType.DART
        result = Result.PLAYING
        feedback = "Build a tower, then end turn"
        towers = arrayOfNulls(TOTAL_CELLS)
        enemies = mutableListOf()
        pending = waveEnemies(wave).toMutableList()
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
            resetRound()
            return true
        }
        if (result != Result.PLAYING) return false

        return when (action) {
            GameAction.PRIMARY -> {
                selectedType = if (selectedType == TowerType.DART) TowerType.FROST else TowerType.DART
                feedback = "Selected ${selectedType.label} · choose a build pad"
                true
            }

            GameAction.SECONDARY -> {
                resolveTurn()
                true
            }

            else -> {
                val cell = cellIndex(action) ?: return false
                commandBuildPad(cell)
            }
        }
    }

    private fun commandBuildPad(cell: Int): Boolean {
        if (cell !in BUILD_CELLS) return false
        val tower = towers[cell]
        if (tower == null) {
            if (gold < selectedType.buildCost) {
                feedback = "Need ${selectedType.buildCost} gold for ${selectedType.label}"
                return false
            }
            gold -= selectedType.buildCost
            towers[cell] = Tower(selectedType, 1)
            feedback = "${selectedType.label} tower placed · end turn to fire"
            return true
        }

        if (tower.level >= MAX_TOWER_LEVEL) {
            feedback = "Tower at cell ${cell + 1} is fully upgraded"
            return false
        }
        val cost = upgradeCost(tower.level)
        if (gold < cost) {
            feedback = "Need $cost gold to upgrade cell ${cell + 1}"
            return false
        }
        gold -= cost
        tower.level++
        feedback = "${tower.type.label} tower upgraded to level ${tower.level}"
        return true
    }

    private fun resolveTurn() {
        if (turns >= MAX_TURNS) {
            finishLoss("The command line stalled")
            return
        }
        turns++

        if (pending.isNotEmpty()) {
            val type = pending.removeAt(0)
            enemies += Enemy(nextEnemyId++, type, type.maxHealth, 0, slowed = false)
        }

        fireTowers()
        collectDefeatedEnemies()
        advanceEnemies()
        if (result != Result.PLAYING) return

        if (pending.isEmpty() && enemies.isEmpty()) {
            if (wave == MAX_WAVES) {
                finishWin("All $MAX_WAVES waves cleared")
            } else {
                val cleared = wave
                wave++
                pending = waveEnemies(wave).toMutableList()
                feedback = "Wave $cleared cleared · wave $wave incoming"
            }
        } else {
            val remaining = enemies.size + pending.size
            feedback = "Turn $turns resolved · $remaining enemies remain"
        }
    }

    private fun fireTowers() {
        BUILD_CELLS.forEach { cell ->
            val tower = towers[cell] ?: return@forEach
            val target = enemies
                .asSequence()
                .filter { it.health > 0 && inRange(cell, tower, it) }
                .sortedWith(compareByDescending<Enemy> { it.position }.thenBy { it.id })
                .firstOrNull()
                ?: return@forEach

            val damage = when (tower.type) {
                TowerType.DART -> 2 + tower.level * 2
                TowerType.FROST -> 1 + tower.level
            }
            target.health -= damage
            if (tower.type == TowerType.FROST && target.health > 0) {
                target.slowed = true
            }
        }
    }

    private fun collectDefeatedEnemies() {
        val defeated = enemies.filter { it.health <= 0 }
        if (defeated.isNotEmpty()) {
            gold = (gold + defeated.sumOf { it.type.reward }).coerceAtMost(MAX_GOLD)
            enemies.removeAll { it.health <= 0 }
        }
    }

    private fun advanceEnemies() {
        val leaking = mutableListOf<Enemy>()
        enemies.forEach { enemy ->
            if (enemy.slowed) {
                enemy.slowed = false
            } else {
                enemy.position++
            }
            if (enemy.position >= PATH_LENGTH) leaking += enemy
        }
        leaking.forEach { enemy ->
            baseHealth -= enemy.type.baseDamage
            enemies.remove(enemy)
        }
        if (baseHealth <= 0) {
            baseHealth = 0
            finishLoss("The base was overrun")
        }
    }

    private fun inRange(cell: Int, tower: Tower, enemy: Enemy): Boolean {
        val rangeInHalfSteps = when (tower.type) {
            TowerType.DART -> 6 + (tower.level - 1) * 2
            TowerType.FROST -> 4 + (tower.level - 1) * 2
        }
        return abs(cell - enemy.position * 2) <= rangeInHalfSteps
    }

    private fun finishWin(message: String) {
        result = Result.WON
        roundsWon++
        feedback = message
    }

    private fun finishLoss(message: String) {
        result = Result.LOST
        roundsLost++
        feedback = message
    }

    private fun waveEnemies(number: Int): List<EnemyType> = when (number) {
        1 -> listOf(EnemyType.GRUNT, EnemyType.GRUNT, EnemyType.RUNNER)
        2 -> listOf(EnemyType.RUNNER, EnemyType.GRUNT, EnemyType.BRUTE, EnemyType.GRUNT)
        3 -> listOf(EnemyType.RUNNER, EnemyType.BRUTE, EnemyType.GRUNT, EnemyType.BRUTE, EnemyType.RUNNER)
        else -> emptyList()
    }

    private fun upgradeCost(level: Int): Int = 1 + level * 2

    override fun render(): GameViewState {
        val terminal = result != Result.PLAYING
        val subtitle = when (result) {
            Result.PLAYING -> "Path 1→3→5→7→9 · ${selectedType.label} selected · tap pads to build/upgrade"
            Result.WON -> "Victory · replay to command another defence"
            Result.LOST -> "Defeat · replay with better tower timing"
        }
        val board = (0 until TOTAL_CELLS).map { cell ->
            val content = if (cell in BUILD_CELLS) {
                towers[cell]?.let { "${it.type.code}${it.level}" } ?: "BUILD"
            } else {
                val pathPosition = PATH_CELLS.indexOf(cell)
                val occupants = enemies.filter { it.position == pathPosition }
                if (occupants.isEmpty()) "PATH" else occupants.joinToString("+") { "${it.type.code}${it.health}" }
            }
            "${cell + 1}:$content"
        }
        return GameViewState(
            title = title,
            subtitle = subtitle,
            scoreLine = "Gold $gold  ·  Base $baseHealth/$MAX_BASE_HEALTH  ·  Wave $wave/$MAX_WAVES  ·  Turn $turns",
            buttons = if (terminal) listOf("REPLAY") else listOf("TYPE ${selectedType.label}", "END TURN"),
            buttonActions = if (terminal) listOf(GameAction.NEW_GAME) else listOf(GameAction.PRIMARY, GameAction.SECONDARY),
            cellButtons = board,
            cellActions = (0 until TOTAL_CELLS).map { GameAction.values()[GameAction.CELL_1.ordinal + it] },
            feedback = feedback,
            isTerminal = terminal,
            phase = when (result) {
                Result.PLAYING -> GamePhase.PLAYING
                Result.WON -> GamePhase.WON
                Result.LOST -> GamePhase.LOST
            },
        )
    }

    override fun encode(): String = JSONObject().apply {
        put("v", STATE_VERSION)
        put("gold", gold)
        put("baseHealth", baseHealth)
        put("wave", wave)
        put("turns", turns)
        put("nextEnemyId", nextEnemyId)
        put("selected", selectedType.name)
        put("result", result.name)
        put("feedback", feedback)
        put("roundsWon", roundsWon)
        put("roundsLost", roundsLost)
        put("towers", JSONArray().apply {
            towers.forEach { tower ->
                if (tower == null) {
                    put(JSONObject.NULL)
                } else {
                    put(JSONObject().apply {
                        put("type", tower.type.name)
                        put("level", tower.level)
                    })
                }
            }
        })
        put("enemies", JSONArray().apply {
            enemies.forEach { enemy ->
                put(JSONObject().apply {
                    put("id", enemy.id)
                    put("type", enemy.type.name)
                    put("health", enemy.health)
                    put("position", enemy.position)
                    put("slowed", enemy.slowed)
                })
            }
        })
        put("pending", JSONArray().apply { pending.forEach { put(it.name) } })
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != STATE_VERSION) return false

        val restoredGold = json.optInt("gold", -1)
        val restoredHealth = json.optInt("baseHealth", -1)
        val restoredWave = json.optInt("wave", -1)
        val restoredTurns = json.optInt("turns", -1)
        val restoredNextId = json.optInt("nextEnemyId", -1)
        val restoredSelected = runCatching { TowerType.valueOf(json.optString("selected")) }.getOrNull()
        val restoredResult = runCatching { Result.valueOf(json.optString("result")) }.getOrNull()
        val restoredFeedback = json.optString("feedback")
        val restoredRoundsWon = json.optInt("roundsWon", -1)
        val restoredRoundsLost = json.optInt("roundsLost", -1)
        val restoredTowers = parseTowers(json.optJSONArray("towers")) ?: return false
        val restoredEnemies = parseEnemies(json.optJSONArray("enemies")) ?: return false
        val restoredPending = parsePending(json.optJSONArray("pending")) ?: return false

        if (restoredSelected == null || restoredResult == null || restoredFeedback.isBlank()) return false
        if (restoredGold !in 0..MAX_GOLD || restoredHealth !in 0..MAX_BASE_HEALTH ||
            restoredWave !in 1..MAX_WAVES || restoredTurns !in 0..MAX_TURNS || restoredNextId < 1 ||
            restoredRoundsWon < 0 || restoredRoundsLost < 0
        ) return false
        if (restoredResult == Result.PLAYING && restoredHealth == 0) return false
        if (restoredResult == Result.LOST && restoredHealth > 0) return false
        if (restoredResult == Result.WON &&
            (restoredWave != MAX_WAVES || restoredHealth == 0 || restoredEnemies.isNotEmpty() || restoredPending.isNotEmpty())
        ) return false
        if (restoredResult != Result.PLAYING && restoredResult != Result.WON && restoredResult != Result.LOST) return false
        if (restoredResult == Result.PLAYING && restoredEnemies.isEmpty() && restoredPending.isEmpty()) return false
        if (restoredEnemies.any { it.id >= restoredNextId }) return false
        if (restoredEnemies.map { it.id }.toSet().size != restoredEnemies.size) return false
        if (restoredTowers.filterIndexed { index, _ -> index !in BUILD_CELLS }.any { it != null }) return false

        gold = restoredGold
        baseHealth = restoredHealth
        wave = restoredWave
        turns = restoredTurns
        nextEnemyId = restoredNextId
        selectedType = restoredSelected
        result = restoredResult
        feedback = restoredFeedback
        roundsWon = restoredRoundsWon
        roundsLost = restoredRoundsLost
        towers = restoredTowers
        enemies = restoredEnemies.toMutableList()
        pending = restoredPending.toMutableList()
        true
    }.getOrDefault(false)

    private fun parseTowers(array: JSONArray?): Array<Tower?>? {
        if (array == null || array.length() != TOTAL_CELLS) return null
        val parsed = arrayOfNulls<Tower>(TOTAL_CELLS)
        for (index in 0 until TOTAL_CELLS) {
            if (array.isNull(index)) continue
            val json = array.optJSONObject(index) ?: return null
            val type = runCatching { TowerType.valueOf(json.optString("type")) }.getOrNull() ?: return null
            val level = json.optInt("level", -1)
            if (level !in 1..MAX_TOWER_LEVEL || index !in BUILD_CELLS) return null
            parsed[index] = Tower(type, level)
        }
        return parsed
    }

    private fun parseEnemies(array: JSONArray?): List<Enemy>? {
        if (array == null || array.length() > 32) return null
        val parsed = mutableListOf<Enemy>()
        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: return null
            val id = json.optInt("id", -1)
            val type = runCatching { EnemyType.valueOf(json.optString("type")) }.getOrNull() ?: return null
            val health = json.optInt("health", -1)
            val position = json.optInt("position", -1)
            if (id < 1 || health !in 1..type.maxHealth || position !in 0 until PATH_LENGTH) return null
            parsed += Enemy(id, type, health, position, json.optBoolean("slowed", false))
        }
        return parsed
    }

    private fun parsePending(array: JSONArray?): List<EnemyType>? {
        if (array == null || array.length() > 32) return null
        val parsed = mutableListOf<EnemyType>()
        for (index in 0 until array.length()) {
            val type = runCatching { EnemyType.valueOf(array.optString(index)) }.getOrNull() ?: return null
            parsed += type
        }
        return parsed
    }

    private fun cellIndex(action: GameAction): Int? {
        val index = action.ordinal - GameAction.CELL_1.ordinal
        return index.takeIf { it in 0 until TOTAL_CELLS }
    }
}
