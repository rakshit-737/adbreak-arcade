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

        private const val VERSION = 2
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
    private var combatBeat = 0
    private var ward = 0
    private var relic = false

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
        combatBeat = 0
        ward = 0
        relic = false
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
                GameAction.PRIMARY -> {
                    if (health <= 3) return false
                    health -= 3
                    relic = true
                    resolveLoot(goldFound = 14, potionFound = 1, label = "Trap −3 HP · relic secured (+40 on escape)")
                }
                GameAction.SECONDARY -> resolveLoot(goldFound = 8, potionFound = 0, label = "Safely searched the cache")
                else -> false
            }
            RoomKind.HEAL -> when (action) {
                GameAction.PRIMARY -> resolveHeal(8, "Rested at the chapel")
                GameAction.SECONDARY -> {
                    ward = 3
                    resolveHeal(4, "Prayed · ward absorbs next 3 damage")
                }
                else -> false
            }
        }
    }

    private fun startCombat(scouted: Boolean): Boolean {
        if (scouted && potions == 0) return false
        val room = rooms[roomIndex]
        val selectedEnemy = enemies.getOrNull(room.enemyIndex) ?: return false
        enemyIndex = room.enemyIndex
        enemyHealth = (selectedEnemy.maxHealth - if (scouted) 2 else 0).coerceAtLeast(1)
        if (scouted) potions--
        currentEnemyName = selectedEnemy.name
        inCombat = true
        guarding = false
        combatBeat = 0
        turns++
        feedback = if (scouted) {
            "Scouted ${selectedEnemy.name} · potion −1 · enemy HP −2"
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
        val damage = minOf(ATTACK_DAMAGE, enemyHealth)
        enemyHealth -= damage
        score = safeAdd(score, damage * 2)
        if (enemyHealth <= 0) {
            defeatEnemy(enemy)
        } else {
            enemyTurn("Hit ${enemy.name} for $damage")
        }
        return true
    }

    private fun guard(): Boolean {
        val enemy = enemies.getOrNull(enemyIndex) ?: return false
        turns++
        guarding = true
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
        guarding = false
        enemyTurn("Potion restored $healed health")
        return true
    }

    private fun enemyTurn(playerFeedback: String) {
        val enemy = enemies[enemyIndex]
        val heavy = combatBeat == 2
        val parry = heavy && guarding
        val incoming = if (heavy) enemy.attack + 3 else enemy.attack
        val blocked = if (parry) incoming else if (guarding) GUARD_BLOCK else 0
        val unblocked = (incoming - blocked).coerceAtLeast(0)
        val absorbed = minOf(ward, unblocked)
        ward -= absorbed
        val damage = unblocked - absorbed
        health = (health - damage).coerceAtLeast(0)
        combatBeat = (combatBeat + 1) % 3
        guarding = false
        if (parry) {
            val counter = minOf(2, enemyHealth)
            enemyHealth -= counter
            score = safeAdd(score, counter * 2)
            if (enemyHealth <= 0) {
                defeatEnemy(enemy)
                return
            }
        }
        if (health == 0) {
            lost = true
            feedback = "${enemy.name} hits for $damage — you were defeated"
        } else {
            feedback = if (parry) "Heavy strike parried · counter −2 enemy HP" else {
                "$playerFeedback · ${enemy.name} hits for $damage${if (absorbed > 0) " · ward −$absorbed" else ""}"
            }
        }
    }

    private fun defeatEnemy(enemy: Enemy) {
        inCombat = false
        enemyHealth = -1
        currentEnemyName = null
        enemyIndex = -1
        guarding = false
        combatBeat = 0
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
            score = safeAdd(score, 100 + if (relic) 40 else 0)
            feedback = "Dungeon cleared · ${if (relic) "relic escaped +40" else "relic skipped"} · score $score"
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
                progress = dungeonProgress(),
                record = score.takeIf { it > 0 }?.let { GameRecord(it, "Dungeon score") },
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
                progress = dungeonProgress(),
                record = score.takeIf { it > 0 }?.let { GameRecord(it, "Dungeon score") },
            )
        }

        val room = rooms[roomIndex]
        return if (inCombat) {
            GameViewState(
                title = title,
                subtitle = "${currentEnemyName.orEmpty()} HP $enemyHealth · turn-based combat · ${enemyIntent()}",
                scoreLine = scoreLine(),
                buttons = listOf("HIT $ATTACK_DAMAGE", if (combatBeat == 2) "PARRY · counter 2" else "GUARD · block $GUARD_BLOCK", "POTION +$POTION_HEAL ($potions)"),
                buttonActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1),
                feedback = feedback,
                progress = dungeonProgress(),
                record = score.takeIf { it > 0 }?.let { GameRecord(it, "Dungeon score") },
                disabledActions = when {
                    potions == 0 -> mapOf(GameAction.CELL_1 to "No potions left")
                    health == MAX_HEALTH -> mapOf(GameAction.CELL_1 to "Health already full")
                    else -> emptyMap()
                },
            )
        } else {
            val roomButtons = when (room.kind) {
                RoomKind.BATTLE -> listOf("FIGHT", "SCOUT · −1 potion")
                RoomKind.BOSS -> listOf("CHALLENGE", "SCOUT · −1 potion")
                RoomKind.LOOT -> listOf("RELIC · −3 HP", "SAFE · +8 gold")
                RoomKind.HEAL -> listOf("REST · +8 HP", "PRAY · +4 HP +3 ward")
            }
            GameViewState(
                title = title,
                subtitle = "Room ${roomIndex + 1}/$TOTAL_ROOMS · ${room.name}: ${room.description}",
                scoreLine = scoreLine(),
                buttons = roomButtons,
                buttonActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY),
                feedback = feedback + when (room.kind) {
                    RoomKind.BATTLE, RoomKind.BOSS -> "\nScout spends 1 potion for −2 enemy HP · heavy every third reply: guard to parry"
                    RoomKind.LOOT -> "\nRelic: +14 gold, +1 potion, +40 score if you escape · trap costs 3 HP"
                    RoomKind.HEAL -> "\nWard absorbs damage after guarding · choose healing or protection"
                },
                progress = dungeonProgress(),
                record = score.takeIf { it > 0 }?.let { GameRecord(it, "Dungeon score") },
                disabledActions = when {
                    room.kind == RoomKind.LOOT && health <= 3 -> mapOf(GameAction.PRIMARY to "Need more than 3 HP to survive the trap")
                    room.enemyIndex >= 0 && potions == 0 -> mapOf(GameAction.SECONDARY to "Scouting needs 1 potion")
                    else -> emptyMap()
                },
            )
        }
    }

    private fun scoreLine(): String =
        "Health $health/$MAX_HEALTH · Potions $potions · Ward $ward · Gold $gold · Score $score"

    private fun dungeonProgress(): GameProgress =
        GameProgress(roomsCleared, TOTAL_ROOMS, "Defeat the dragon · relic ${if (relic) "secured" else "optional +40"}")

    private fun enemyIntent(): String = if (combatBeat == 2) {
        "HEAVY ${enemies[enemyIndex].attack + 3} · guard parries"
    } else {
        "STRIKE ${enemies[enemyIndex].attack} · heavy in ${2 - combatBeat}"
    }

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
        put("combatBeat", combatBeat)
        put("ward", ward)
        put("relic", relic)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..VERSION) return false

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
        val restoredBeat = if (version >= 2) json.optInt("combatBeat", -1) else 0
        val restoredWard = if (version >= 2) json.optInt("ward", -1) else 0
        val restoredRelic = if (version >= 2) json.optBoolean("relic", false) else false
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
        if (restoredBeat !in 0..2 || restoredWard !in 0..3 ||
            !restoredInCombat && restoredBeat != 0 || restoredRelic && restoredRoomIndex < 2
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
        // Old guard/potion points could be farmed. Keep progress and resources while
        // retaining only a conservative finite achievement score for legacy runs.
        score = if (version == 1) {
            restoredGold.coerceAtMost(100) + restoredEnemiesDefeated * 10 + if (restoredWon) 100 else 0
        } else restoredScore
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
        combatBeat = restoredBeat
        ward = restoredWard
        relic = restoredRelic
        true
    }.getOrDefault(false)

    private fun safeAdd(left: Int, right: Int): Int =
        if (right > MAX_SCORE - left) MAX_SCORE else left + right
}
