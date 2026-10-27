package com.adbreak.arcade.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarshipSurvivalGameTest {
    @Test
    fun initialRunHasFourNotificationSafeChoicesAndExplicitResources() {
        val game = StarshipSurvivalGame()
        val state = game.render()

        assertEquals("starship_survival", game.id)
        assertEquals(4, state.buttons.size)
        assertEquals(
            listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
            state.buttonActions,
        )
        assertTrue(state.scoreLine.contains("Hull ${StarshipSurvivalGame.MAX_HULL}"))
        assertTrue(state.scoreLine.contains("Shields ${StarshipSurvivalGame.MAX_SHIELDS}"))
        assertTrue(state.scoreLine.contains("Fuel ${StarshipSurvivalGame.MAX_FUEL}"))
        assertTrue(state.scoreLine.contains("Energy ${StarshipSurvivalGame.MAX_ENERGY}"))
        assertTrue(state.scoreLine.contains("Supplies ${StarshipSurvivalGame.MAX_SUPPLIES}"))
    }

    @Test
    fun actionsHaveDeterministicCostsAndAdvanceOneFiniteEvent() {
        val game = StarshipSurvivalGame()

        assertTrue(game.apply(GameAction.PRIMARY))
        assertEquals(StarshipSurvivalGame.MAX_FUEL - StarshipSurvivalGame.NAVIGATE_FUEL_COST, game.fuel)
        assertEquals(StarshipSurvivalGame.MAX_SUPPLIES - StarshipSurvivalGame.NAVIGATE_SUPPLIES_COST, game.supplies)
        assertEquals(1, game.eventIndex)

        assertTrue(game.apply(GameAction.SECONDARY))
        assertEquals(StarshipSurvivalGame.MAX_ENERGY - StarshipSurvivalGame.REPAIR_ENERGY_COST, game.energy)
        assertEquals(2, game.eventIndex)

        assertTrue(game.apply(GameAction.CELL_1))
        assertEquals(StarshipSurvivalGame.MAX_ENERGY - StarshipSurvivalGame.REPAIR_ENERGY_COST - StarshipSurvivalGame.SCAN_ENERGY_COST, game.energy)
        assertEquals(3, game.eventIndex)

        assertTrue(game.apply(GameAction.CELL_2))
        assertEquals(4, game.eventIndex)
        assertTrue(game.render().subtitle.contains("Sector 5/6"))
    }

    @Test
    fun hazardAwareRouteReachesFiniteEscapeAndSupportsReplay() {
        val game = StarshipSurvivalGame()
        repeat(StarshipSurvivalGame.TOTAL_EVENTS) {
            assertTrue(game.apply(if (it % 2 == 0) GameAction.CELL_2 else GameAction.CELL_1))
        }

        assertTrue(game.render().isTerminal)
        assertEquals(GamePhase.WON, game.render().phase)
        assertTrue(game.render().feedback.contains("Escape complete"))
        assertFalse(game.apply(GameAction.CELL_1))
        assertFalse(game.apply(GameAction.PRIMARY))

        assertTrue(game.apply(GameAction.NEW_GAME))
        assertFalse(game.render().isTerminal)
        assertEquals(0, game.eventIndex)
        assertEquals(StarshipSurvivalGame.MAX_HULL, game.hull)
    }

    @Test
    fun recklessNavigationCanReachFiniteFailure() {
        val game = StarshipSurvivalGame()
        repeat(StarshipSurvivalGame.TOTAL_EVENTS) {
            if (game.render().isTerminal) return@repeat
            assertTrue(game.apply(GameAction.PRIMARY))
        }

        assertEquals(GamePhase.LOST, game.render().phase)
        assertTrue(game.render().isTerminal)
        assertTrue(game.render().feedback.contains("Hull breach"))
        assertFalse(game.apply(GameAction.SECONDARY))
    }

    @Test
    fun pauseResumeAndInvalidActionsDoNotSpendResources() {
        val game = StarshipSurvivalGame()
        val before = game.encode()

        assertFalse(game.apply(GameAction.CELL_9))
        assertEquals(before, game.encode())
        assertTrue(game.apply(GameAction.PAUSE))
        assertEquals(GamePhase.PAUSED, game.render().phase)
        val paused = game.encode()
        assertFalse(game.apply(GameAction.PRIMARY))
        assertEquals(paused, game.encode())
        assertTrue(game.apply(GameAction.RESUME))
        assertEquals(GamePhase.PLAYING, game.render().phase)
        assertFalse(game.apply(GameAction.RESUME))
    }

    @Test
    fun snapshotsRoundTripAndMalformedSnapshotsAreAtomic() {
        val source = StarshipSurvivalGame()
        assertTrue(source.apply(GameAction.PRIMARY))
        assertTrue(source.apply(GameAction.CELL_1))
        val encoded = source.encode()

        val restored = StarshipSurvivalGame()
        assertTrue(restored.restore(encoded))
        assertEquals(source.render(), restored.render())
        assertEquals(encoded, restored.encode())

        val before = restored.render()
        assertFalse(restored.restore(JSONObject().put("v", 1).put("hull", -1).toString()))
        assertEquals(before, restored.render())
        assertFalse(restored.restore("not json"))
        assertEquals(before, restored.render())
    }

    @Test
    fun terminalSnapshotAndResetRemainUsable() {
        val game = StarshipSurvivalGame()
        repeat(StarshipSurvivalGame.TOTAL_EVENTS) {
            assertTrue(game.apply(if (it % 2 == 0) GameAction.CELL_2 else GameAction.CELL_1))
        }
        val restored = StarshipSurvivalGame()
        assertTrue(restored.restore(game.encode()))
        assertEquals(GamePhase.WON, restored.render().phase)
        assertFalse(restored.apply(GameAction.PRIMARY))
        assertTrue(restored.apply(GameAction.RESET))
        assertEquals(GamePhase.PLAYING, restored.render().phase)
        assertEquals(0, restored.eventIndex)
    }
}
