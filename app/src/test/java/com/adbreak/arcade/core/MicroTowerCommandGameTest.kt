package com.adbreak.arcade.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroTowerCommandGameTest {
    @Test
    fun initialStateExposesFixedPathAndCommandControls() {
        val game = MicroTowerCommandGame()
        val state = game.render()

        assertEquals("micro_tower_command", game.id)
        assertEquals(9, state.cellButtons.size)
        assertEquals(9, state.cellActions.size)
        assertEquals(listOf("1:PATH", "2:BUILD", "3:PATH", "4:BUILD", "5:PATH", "6:BUILD", "7:PATH", "8:BUILD", "9:PATH"), state.cellButtons)
        assertEquals(listOf(GameAction.PRIMARY, GameAction.SECONDARY), state.buttonActions)
        assertEquals(GamePhase.PLAYING, state.phase)
        assertTrue(state.scoreLine.contains("Gold 12"))
    }

    @Test
    fun pathCellsRejectBuildCommandsAndTowerCommandsSpendResources() {
        val game = MicroTowerCommandGame()

        assertFalse(game.apply(GameAction.CELL_1))
        assertEquals(12, game.resources)
        assertTrue(game.apply(GameAction.CELL_2))
        assertEquals(8, game.resources)
        assertTrue(game.render().cellButtons[1].contains("D1"))
        assertTrue(game.apply(GameAction.CELL_2))
        assertEquals(5, game.resources)
        assertTrue(game.render().cellButtons[1].contains("D2"))

        assertTrue(game.apply(GameAction.PRIMARY))
        assertEquals("FROST", game.selectedTower)
        assertTrue(game.apply(GameAction.CELL_4))
        assertEquals(0, game.resources)
        assertTrue(game.render().cellButtons[3].contains("F1"))
    }

    @Test
    fun towerTypesUpgradeIndependentlyAndResourcesAreLimited() {
        val game = MicroTowerCommandGame()
        assertTrue(game.apply(GameAction.CELL_2))
        assertTrue(game.apply(GameAction.CELL_2))
        assertEquals(5, game.resources)
        assertTrue(game.render().cellButtons[1].contains("D2"))

        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.CELL_4))
        assertEquals(0, game.resources)
        assertFalse(game.apply(GameAction.CELL_6))
        assertTrue(game.render().cellButtons[1].contains("D2"))
        assertTrue(game.render().cellButtons[3].contains("F1"))
    }

    @Test
    fun endTurnResolvesDeterministicallyAndRoundTripsExactly() {
        val source = MicroTowerCommandGame()
        assertTrue(source.apply(GameAction.CELL_2))
        assertTrue(source.apply(GameAction.CELL_4))
        assertTrue(source.apply(GameAction.SECONDARY))

        val encoded = source.encode()
        val restored = MicroTowerCommandGame()
        assertTrue(restored.restore(encoded))
        assertEquals(source.render(), restored.render())
        assertEquals(encoded, restored.encode())
        assertEquals(1, JSONObject(encoded).getInt("turns"))
        assertEquals(2, JSONObject(encoded).getJSONArray("pending").length())
        assertEquals(6, source.resources)
    }

    @Test
    fun malformedStateIsRejectedWithoutChangingTheCurrentRound() {
        val game = MicroTowerCommandGame()
        assertTrue(game.apply(GameAction.CELL_2))
        val before = game.render()
        assertFalse(game.restore("{}"))
        assertEquals(before, game.render())

        val invalidTower = JSONObject(game.encode()).apply {
            getJSONArray("towers").put(0, JSONObject().apply {
                put("type", "DART")
                put("level", 1)
            })
        }
        assertFalse(game.restore(invalidTower.toString()))
        assertEquals(before, game.render())
    }

    @Test
    fun terminalStatesRejectMovesAndReplayRestoresFreshRound() {
        val won = MicroTowerCommandGame()
        val wonState = JSONObject(won.encode()).apply {
            put("wave", 3)
            put("result", "WON")
            put("feedback", "All 3 waves cleared")
            put("pending", org.json.JSONArray())
            put("enemies", org.json.JSONArray())
        }
        assertTrue(won.restore(wonState.toString()))
        assertEquals(GamePhase.WON, won.render().phase)
        assertTrue(won.render().isTerminal)
        assertFalse(won.apply(GameAction.SECONDARY))
        assertTrue(won.apply(GameAction.NEW_GAME))
        assertEquals(GamePhase.PLAYING, won.render().phase)
        assertEquals(12, won.resources)

        val lost = MicroTowerCommandGame()
        val lostState = JSONObject(lost.encode()).apply {
            put("baseHealth", 0)
            put("result", "LOST")
            put("feedback", "The base was overrun")
        }
        assertTrue(lost.restore(lostState.toString()))
        assertEquals(GamePhase.LOST, lost.render().phase)
        assertFalse(lost.apply(GameAction.CELL_2))
    }

    @Test
    fun aDefenceCanClearAllWavesWithDistinctTowerRoles() {
        val game = MicroTowerCommandGame()
        assertTrue(game.apply(GameAction.CELL_2))
        assertTrue(game.apply(GameAction.CELL_4))
        assertTrue(game.apply(GameAction.CELL_6))

        repeat(20) {
            if (game.render().isTerminal) return@repeat
            assertTrue(game.apply(GameAction.SECONDARY))
        }

        assertEquals(GamePhase.WON, game.render().phase)
        assertNotEquals(GamePhase.LOST, game.render().phase)
        assertTrue(game.health > 0)
    }
}
