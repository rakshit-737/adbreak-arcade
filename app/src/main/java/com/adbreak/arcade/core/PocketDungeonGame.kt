package com.adbreak.arcade.core

import org.json.JSONObject

/**
 * A small, deterministic dungeon run that can be played one notification
 * button press at a time. Room decisions and combat turns are separate stages
 * so a saved notification never needs a timer or hidden callback to continue.
 */
class PocketDungeonGame : ArcadeGame {
    override val id = "pocket_dungeon"
    override val title = "Pocket Dungeon"
    override val description = "Explore five tiny rooms, outlast distinct monsters, and defeat the dungeon boss."

    companion object {
        const val MAX_HEALTH = 20
        const val MAX_POTIONS = 3
        const val TOTAL_ROOMS = 5
        const val ATTACK_DAMAGE = 4
        const val POTION_HEAL = 6
        const val GUARD_BLOCK = 2

        private const val VERSION = 1
        private const val MAX_SCORE = 1_000_000
        private const val MAX_GOLD = 1_000_000
    }

    private enum class RoomKind {
        BATTLE,
        LOOT,
        HEAL,
        BOSS,
    }

    private data class Room(
        val name: String,
        val description: String,
        val kind: RoomKind,
        val enemyIndex: Int = -1,
    )

    private data class Enemy(
        val name: String,
        val maxHealth: Int,
        val attack: Int,
        val loot: Int,
    )

    private val enemies = listOf(
        Enemy("Moss Goblin", maxHealth = 5, attack = 2, loot = 10),
        Enemy("Bone Wraith", maxHealth = 8, attack = 3, loot = 18),
        Enemy("Pocket Dragon", maxHealth = 12, attack = 4, loot = 45),
    )

    private val rooms = listOf(
        Room("Moss Gate", "A goblin blocks the moss-covered gate.", RoomKind.BATTLE, enemyIndex = 0),
        Room("Sunken Cache", "A half-buried chest glints beneath the water.", RoomKind.LOOT),
        Room("Quiet Chapel", "A candlelit chapel offers a moment to recover.", RoomKind.HEAL),
        Room("Frost Hall", "Cold mist gathers around a restless wraith.", RoomKind.BATTLE, enemyIndex = 1),
        Room("Dragon Vault", "A tiny dragon guards the vault's last door.", RoomKind.BOSS, enemyIndex = 2),
    )

    var health: Int = MAX_HEALTH
        private set
    var potions: Int = MAX_POTIONS
        private set
    var gold: Int = 0
        private set
    var score: Int = 0
        private set
    var roomIndex: Int = 0
        private set
    var roomsCleared: Int = 0
        private set
    var enemiesDefeated: Int = 0
        private set
    var turns: Int = 0
        private set
    var enemyHealth: Int = -1
        private set
    var currentEnemyName: String? = null
        private set
    var inCombat: Boolean = false
        private set

    private var paused = false
    private var won = false
    private var lost = false
    private var enemyIndex = -1
    private var feedback = "Choose how to enter the room"
    private var guarding = false

    private val terminal: Boolean
        get() = won || lost

    init {
        reset()
    }

    override fun reset() {
        health = MAX_HEALTH
        potions = MAX_POTIONS
        gold = 0
        score = 0
        roomIndex = 0
        roomsCleared = 0
        enemiesDefeated = 0
        turns = 0
        enemyHealth = -1
        currentEnemyName = null
        inCombat = false
        paused = false
        won = false
        lost = false
        enemyIndex = -1
        feedback = "Choose how to enter the room"
        guarding = false
    }

    override fun apply(action: GameAction): Boolean {
        when (action) {
            GameAction.RESET, GameAction.NEW_GAME -> {
                reset()
                return true
            }
            GameAction.PAUSE -> {
                if (terminal || paused) return false
                paused = true
                feedback = "Dungeon paused"
                return true
            }
            GameAction.RESUME -> {
                if (terminal || !paused) return false
                paused = false
                feedback = "Dungeon resumed"
                return true
            }
            else -> Unit
        }

        if (terminal || paused) return false
        return if (inCombat) applyCombatAction(action) else applyRoomAction(action)
    }

    private fun applyRoomAction(action: GameAction): Boolean {
        if (roomIndex !in 0 until TOTAL_ROOMS) return false
        val room = rooms[roomIndex]
        return when (room.kind) {
            RoomKind.BATTLE, RoomKind.BOSS -> when (action) {
                GameAction.PRIMARY -> startCombat(scouted = false)
                GameAction.SECONDARY -> startCombat(scouted = true)
                else -> false
            }
            RoomKind.LOOT -> when (action) {
                GameAction.PRIMARY -> resolveLoot(goldFound = 14, potionFound = 1, label = "Opened the chest")
                GameAction.SECONDARY -> resolveLoot(goldFound = 8, potionFound = 0, label = "Safely searched the cache")
                else -> false
            }
            RoomKind.HEAL -> when (action) {
                GameAction.PRIMARY -> resolveHeal(8, "Rested at the chapel")
                GameAction.SECONDARY -> resolveHeal(4, "Prayed at the chapel")
                else -> false
            }
        }
    }

    private fun startCombat(scouted: Boolean): Boolean {
        val room = rooms[roomIndex]
        val selectedEnemy = enemies.getOrNull(room.enemyIndex) ?: return false
        enemyIndex = room.enemyIndex
        enemyHealth = (selectedEnemy.maxHealth - if (scouted) 1 else 0).coerceAtLeast(1)
        currentEnemyName = selectedEnemy.name
        inCombat = true
        guarding = false
        turns++
        if (scouted) score = safeAdd(score, 2)
        feedback = if (scouted) {
            "Scouted ${selectedEnemy.name} — it starts weakened"
        } else {
            "${selectedEnemy.name} attacks — choose a combat move"
        }
        return true
    }

    private fun resolveLoot(goldFound: Int, potionFound: Int, label: String): Boolean {
        turns++
        val oldPotions = potions
        gold = safeAdd(gold, goldFound)
        potions = (potions + potionFound).coerceAtMost(MAX_POTIONS)
        score = safeAdd(score, 8 + goldFound)
        val potionText = if (potions > oldPotions) " · potion +${potions - oldPotions}" else ""
        advanceRoom("$label — gold +$goldFound$potionText")
        return true
    }

    private fun resolveHeal(amount: Int, label: String): Boolean {
        turns++
        val oldHealth = health
        health = (health + amount).coerceAtMost(MAX_HEALTH)
        score = safeAdd(score, 6)
        advanceRoom("$label — health +${health - oldHealth}")
        return true
    }

    private fun applyCombatAction(action: GameAction): Boolean = when (action) {
        GameAction.PRIMARY -> attack()
        GameAction.SECONDARY -> guard()
        GameAction.CELL_1 -> drinkPotion()
        else -> false
    }

    private fun attack(): Boolean {
        val enemy = enemies.getOrNull(enemyIndex) ?: return false
        turns++
        guarding = false
        enemyHealth -= ATTACK_DAMAGE
        score = safeAdd(score, 10)
        if (enemyHealth <= 0) {
            defeatEnemy(enemy)
        } else {
            enemyTurn("Hit ${enemy.name} for $ATTACK_DAMAGE")
        }
        return true
    }

    private fun guard(): Boolean {
        val enemy = enemies.getOrNull(enemyIndex) ?: return false
        turns++
        guarding = true
        score = safeAdd(score, 2)
        enemyTurn("Guarded against ${enemy.name}")
        return true
    }

    private fun drinkPotion(): Boolean {
        if (potions <= 0 || health >= MAX_HEALTH) return false
        val enemy = enemies.getOrNull(enemyIndex) ?: return false
        turns++
        potions--
        val healed = minOf(POTION_HEAL, MAX_HEALTH - health)
        health += healed
        score = safeAdd(score, 4)
        guarding = false
        enemyTurn("Potion restored $healed health")
        return true
    }

    private fun enemyTurn(playerFeedback: String) {
        val enemy = enemies[enemyIndex]
        val damage = (enemy.attack - if (guarding) GUARD_BLOCK else 0).coerceAtLeast(0)
        health = (health - damage).coerceAtLeast(0)
        guarding = false
        if (health == 0) {
            lost = true
            feedback = "${enemy.name} hits for $damage — you were defeated"
        } else {
            feedback = "$playerFeedback · ${enemy.name} hits for $damage"
        }
    }

    private fun defeatEnemy(enemy: Enemy) {
        inCombat = false
        enemyHealth = -1
        currentEnemyName = null
        enemyIndex = -1
        guarding = false
        enemiesDefeated++
        gold = safeAdd(gold, enemy.loot)
        score = safeAdd(score, enemy.loot)
        advanceRoom("${enemy.name} defeated — gold +${enemy.loot}")
    }

    private fun advanceRoom(message: String) {
        roomsCleared++
        roomIndex++
        if (roomIndex >= TOTAL_ROOMS) {
            won = true
            score = safeAdd(score, 100)
            feedback = "Dungeon cleared — score $score"
        } else {
            feedback = "$message · next: ${rooms[roomIndex].name}"
        }
    }

    override fun render(): GameViewState {
        if (won || lost) {
            return GameViewState(
                title = title,
                subtitle = if (won) {
                    "Dungeon cleared — replay to enter again"
                } else {
                    "Defeated in room ${roomIndex + 1}/$TOTAL_ROOMS — replay to try again"
                },
                scoreLine = scoreLine(),
                buttons = listOf("REPLAY"),
                buttonActions = listOf(GameAction.NEW_GAME),
                feedback = feedback,
                isTerminal = true,
                phase = if (won) GamePhase.WON else GamePhase.LOST,
            )
        }

        if (paused) {
            return GameViewState(
                title = title,
                subtitle = "Paused · room ${roomIndex + 1}/$TOTAL_ROOMS",
                scoreLine = scoreLine(),
                buttons = listOf("RESUME", "RESET"),
                buttonActions = listOf(GameAction.RESUME, GameAction.RESET),
                feedback = feedback,
                phase = GamePhase.PAUSED,
            )
        }

        val room = rooms[roomIndex]
        return if (inCombat) {
            GameViewState(
                title = title,
                subtitle = "${room.name} · ${currentEnemyName.orEmpty()} HP $enemyHealth · turn-based combat",
                scoreLine = scoreLine(),
                buttons = buildList {
                    add("ATTACK")
                    add("GUARD")
                    if (potions > 0) add("POTION x$potions")
                },
                buttonActions = buildList {
                    add(GameAction.PRIMARY)
                    add(GameAction.SECONDARY)
                    if (potions > 0) add(GameAction.CELL_1)
                },
                feedback = feedback,
            )
        } else {
            val roomButtons = when (room.kind) {
                RoomKind.BATTLE -> listOf("FIGHT", "SCOUT")
                RoomKind.BOSS -> listOf("CHALLENGE", "READ RUNES")
                RoomKind.LOOT -> listOf("OPEN CHEST", "SEARCH SAFE")
                RoomKind.HEAL -> listOf("REST", "PRAY")
            }
            GameViewState(
                title = title,
                subtitle = "Room ${roomIndex + 1}/$TOTAL_ROOMS · ${room.name}: ${room.description}",
                scoreLine = scoreLine(),
                buttons = roomButtons,
                buttonActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY),
                feedback = feedback,
            )
        }
    }

    private fun scoreLine(): String =
        "Score $score · Health $health/$MAX_HEALTH · Potions $potions · Gold $gold · Rooms $roomsCleared/$TOTAL_ROOMS"

    override fun encode(): String = JSONObject().apply {
        put("v", VERSION)
        put("health", health)
        put("potions", potions)
        put("gold", gold)
        put("score", score)
        put("roomIndex", roomIndex)
        put("roomsCleared", roomsCleared)
        put("enemiesDefeated", enemiesDefeated)
        put("turns", turns)
        put("enemyIndex", enemyIndex)
        put("enemyHealth", enemyHealth)
        put("currentEnemyName", currentEnemyName)
        put("inCombat", inCombat)
        put("paused", paused)
        put("won", won)
        put("lost", lost)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != VERSION) return false

        val restoredHealth = json.optInt("health", -1)
        val restoredPotions = json.optInt("potions", -1)
        val restoredGold = json.optInt("gold", -1)
        val restoredScore = json.optInt("score", -1)
        val restoredRoomIndex = json.optInt("roomIndex", -1)
        val restoredRoomsCleared = json.optInt("roomsCleared", -1)
        val restoredEnemiesDefeated = json.optInt("enemiesDefeated", -1)
        val restoredTurns = json.optInt("turns", -1)
        val restoredEnemyIndex = json.optInt("enemyIndex", -2)
        val restoredEnemyHealth = json.optInt("enemyHealth", -2)
        val restoredInCombat = json.optBoolean("inCombat", false)
        val restoredPaused = json.optBoolean("paused", false)
        val restoredWon = json.optBoolean("won", false)
        val restoredLost = json.optBoolean("lost", false)
        val restoredFeedback = json.optString("feedback")
        val restoredRoom = rooms.getOrNull(restoredRoomIndex)
        val completedEnemyCount = if (restoredRoomIndex in 0..TOTAL_ROOMS) {
            rooms.take(restoredRoomIndex).count { it.enemyIndex >= 0 }
        } else {
            -1
        }
        val restoredEnemy = enemies.getOrNull(restoredEnemyIndex)
        val expectedEnemyName = restoredEnemy?.name
        val restoredName = json.optString("currentEnemyName", "")

        if (restoredHealth !in 0..MAX_HEALTH || restoredPotions !in 0..MAX_POTIONS ||
            restoredGold !in 0..MAX_GOLD || restoredScore !in 0..MAX_SCORE ||
            restoredRoomIndex !in 0..TOTAL_ROOMS || restoredRoomsCleared != restoredRoomIndex ||
            restoredEnemiesDefeated != completedEnemyCount || restoredTurns !in 0..MAX_SCORE ||
            restoredFeedback.isBlank() || restoredWon && restoredLost || restoredPaused && (restoredWon || restoredLost) ||
            restoredName != (expectedEnemyName ?: "")
        ) return false

        if (restoredWon) {
            if (restoredRoomIndex != TOTAL_ROOMS || restoredHealth == 0 || restoredInCombat ||
                restoredEnemyIndex != -1 || restoredEnemyHealth != -1 || restoredEnemiesDefeated != enemies.size
            ) return false
        } else if (restoredLost) {
            if (restoredHealth != 0 || !restoredInCombat || restoredRoom?.enemyIndex != restoredEnemyIndex ||
                restoredEnemy == null || restoredEnemyHealth !in 1..restoredEnemy.maxHealth
            ) return false
        } else {
            if (restoredRoomIndex == TOTAL_ROOMS || restoredHealth == 0) return false
            if (restoredInCombat) {
                if (restoredRoom?.enemyIndex != restoredEnemyIndex || restoredEnemy == null ||
                    restoredEnemyHealth !in 1..restoredEnemy.maxHealth
                ) return false
            } else if (restoredEnemyIndex != -1 || restoredEnemyHealth != -1 || restoredName.isNotEmpty()) {
                return false
            }
        }

        health = restoredHealth
        potions = restoredPotions
        gold = restoredGold
        score = restoredScore
        roomIndex = restoredRoomIndex
        roomsCleared = restoredRoomsCleared
        enemiesDefeated = restoredEnemiesDefeated
        turns = restoredTurns
        enemyIndex = restoredEnemyIndex
        enemyHealth = restoredEnemyHealth
        currentEnemyName = expectedEnemyName
        inCombat = restoredInCombat
        paused = restoredPaused
        won = restoredWon
        lost = restoredLost
        feedback = restoredFeedback
        guarding = false
        true
    }.getOrDefault(false)

    private fun safeAdd(left: Int, right: Int): Int =
        if (right > MAX_SCORE - left) MAX_SCORE else left + right
}
