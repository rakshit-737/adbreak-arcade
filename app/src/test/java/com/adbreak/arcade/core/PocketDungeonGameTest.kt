package com.adbreak.arcade.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PocketDungeonGameTest {
    @Test
    fun roomAndCombatStagesExposeNotificationSafeChoices() {
        val game = PocketDungeonGame()

        assertEquals("pocket_dungeon", game.id)
        assertEquals(2, game.render().buttons.size)
        assertEquals(listOf(GameAction.PRIMARY, GameAction.SECONDARY), game.render().buttonActions)
        assertTrue(game.render().subtitle.contains("Room 1/5"))

        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.inCombat)
        assertEquals("Moss Goblin", game.currentEnemyName)
        assertEquals(3, game.render().buttonActions.size)
        assertEquals(
            listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1),
            game.render().buttonActions,
        )
        assertTrue(game.render().subtitle.contains("turn-based combat"))
    }

    @Test
    fun deterministicPrimaryRouteClearsLootHealAndDistinctEnemies() {
        val game = PocketDungeonGame()

        assertTrue(game.apply(GameAction.PRIMARY)) // enter Moss Gate
        assertTrue(game.apply(GameAction.PRIMARY)) // hit goblin
        assertTrue(game.apply(GameAction.PRIMARY)) // defeat goblin
        assertEquals(1, game.roomIndex)
        assertEquals(1, game.enemiesDefeated)

        assertTrue(game.apply(GameAction.PRIMARY)) // open cache
        assertEquals(2, game.roomIndex)
        assertTrue(game.gold > 0)
        assertTrue(game.potions >= PocketDungeonGame.MAX_POTIONS)

        assertTrue(game.apply(GameAction.PRIMARY)) // rest
        assertEquals(3, game.roomIndex)

        assertTrue(game.apply(GameAction.PRIMARY)) // enter Frost Hall
        assertEquals("Bone Wraith", game.currentEnemyName)
        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertEquals(4, game.roomIndex)

        assertTrue(game.apply(GameAction.PRIMARY)) // challenge vault
        assertEquals("Pocket Dragon", game.currentEnemyName)
        repeat(3) { assertTrue(game.apply(GameAction.PRIMARY)) }

        assertTrue(game.render().isTerminal)
        assertEquals(GamePhase.WON, game.render().phase)
        assertEquals(PocketDungeonGame.TOTAL_ROOMS, game.roomsCleared)
        assertEquals(3, game.enemiesDefeated)
        assertEquals(listOf("REPLAY"), game.render().buttons)
    }

    @Test
    fun guardAndPotionAreDeterministicResourcesAndDefeatIsTerminal() {
        val game = PocketDungeonGame()
        assertTrue(game.apply(GameAction.PRIMARY))
        val healthBeforeGuard = game.health
        assertTrue(game.apply(GameAction.SECONDARY))
        assertEquals(healthBeforeGuard, game.health) // goblin attack 2, guard blocks 2

        val potionsBefore = game.potions
        assertTrue(game.apply(GameAction.PRIMARY)) // take damage before drinking
        assertTrue(game.apply(GameAction.CELL_1))
        assertEquals(potionsBefore - 1, game.potions)
        assertTrue(game.health > 0)

        val defeated = PocketDungeonGame()
        assertTrue(defeated.apply(GameAction.PRIMARY))
        val weakened = JSONObject(defeated.encode()).put("health", 1)
        assertTrue(defeated.restore(weakened.toString()))
        assertTrue(defeated.apply(GameAction.PRIMARY))
        assertEquals(GamePhase.LOST, defeated.render().phase)
        assertTrue(defeated.render().isTerminal)
        assertFalse(defeated.apply(GameAction.PRIMARY))
    }

    @Test
    fun snapshotsRoundTripAndMalformedStateIsRejectedAtomically() {
        val source = PocketDungeonGame()
        val fresh = PocketDungeonGame()
        assertTrue(fresh.restore(source.encode()))
        assertEquals(source.render(), fresh.render())
        assertEquals(source.encode(), fresh.encode())

        assertTrue(source.apply(GameAction.PRIMARY))
        assertTrue(source.apply(GameAction.SECONDARY))
        val encoded = source.encode()

        val restored = PocketDungeonGame()
        assertTrue(restored.restore(encoded))
        assertEquals(source.render(), restored.render())
        assertEquals(encoded, restored.encode())

        val before = restored.render()
        assertFalse(restored.restore("not json"))
        assertFalse(restored.restore(JSONObject(encoded).put("health", -1).toString()))
        assertFalse(restored.restore(JSONObject(encoded).put("enemyHealth", 999).toString()))
        assertEquals(before, restored.render())
    }

    @Test
    fun invalidActionsAndReplayChecksDoNotMutateState() {
        val game = PocketDungeonGame()
        val initial = game.encode()
        assertFalse(game.apply(GameAction.CELL_9))
        assertFalse(game.apply(GameAction.CELL_1)) // potion cannot be used outside combat
        assertEquals(initial, game.encode())

        repeat(2) { assertTrue(game.apply(GameAction.PRIMARY)) }
        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.PRIMARY))
        repeat(2) { assertTrue(game.apply(GameAction.PRIMARY)) }
        assertTrue(game.apply(GameAction.PRIMARY))
        repeat(3) { assertTrue(game.apply(GameAction.PRIMARY)) }
        assertTrue(game.render().isTerminal)
        val terminal = game.encode()
        assertFalse(game.apply(GameAction.SECONDARY))
        assertEquals(terminal, game.encode())
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertFalse(game.render().isTerminal)
        assertEquals(0, game.roomIndex)
    }
}
