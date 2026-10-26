package com.adbreak.arcade.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeonTacticsGameTest {
    @Test
    fun initialBoardIsReadableAndNotificationSafe() {
        val game = NeonTacticsGame()
        val state = game.render()

        assertEquals("neon_tactics", game.id)
        assertEquals(9, state.cellButtons.size)
        assertEquals(9, state.cellActions.size)
        assertTrue(state.buttons.size <= 4)
        assertEquals(state.buttons.size, state.buttonActions.size)
        assertTrue(state.subtitle.contains("ADVANCE"))
        assertTrue(state.cellButtons[NeonTacticsGame.OBSTACLE_CELL].contains("BLOCK"))
        assertTrue(state.cellButtons[NeonTacticsGame.OBJECTIVE_CELL].contains("CORE"))
    }

    @Test
    fun invalidMovesAreRejectedWithoutChangingState() {
        val game = NeonTacticsGame()
        val before = game.encode()

        assertFalse(game.apply(GameAction.CELL_5)) // obstacle
        assertEquals(before, game.encode())
        assertFalse(game.apply(GameAction.CELL_9)) // non-adjacent enemy
        assertEquals(before, game.encode())
        assertFalse(game.apply(GameAction.CELL_3)) // non-adjacent cell
        assertEquals(before, game.encode())
    }

    @Test
    fun deterministicEncounterSupportsMoveAttackDefendAndObjectiveWin() {
        val game = NeonTacticsGame()

        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.render().subtitle.contains("STRIKE"))

        assertTrue(game.apply(GameAction.PRIMARY))
        assertEquals(5, game.playerHp)
        assertTrue(game.apply(GameAction.CELL_2))
        assertEquals(1, game.enemyHp)
        assertTrue(game.apply(GameAction.CELL_2))
        assertEquals(0, game.enemyHp)

        assertTrue(game.apply(GameAction.CELL_4))
        assertTrue(game.apply(GameAction.CELL_7))
        assertEquals(GamePhase.WON, game.render().phase)
        assertTrue(game.render().isTerminal)
        assertFalse(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertFalse(game.render().isTerminal)
        assertEquals(1, game.wins)
    }

    @Test
    fun enemyCanEndTheRoundAndReplayIsAvailable() {
        val game = NeonTacticsGame()
        repeat(3) { assertTrue(game.apply(GameAction.PRIMARY)) }
        repeat(6) { assertTrue(game.apply(GameAction.PRIMARY)) }

        assertEquals(GamePhase.LOST, game.render().phase)
        assertTrue(game.render().isTerminal)
        assertEquals(1, game.losses)
        assertFalse(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertFalse(game.render().isTerminal)
    }

    @Test
    fun stateRoundTripsAndMalformedStateDoesNotMutateTheGame() {
        val source = NeonTacticsGame()
        assertTrue(source.apply(GameAction.CELL_4))
        val encoded = source.encode()

        val restored = NeonTacticsGame()
        assertTrue(restored.restore(encoded))
        assertEquals(source.render(), restored.render())
        assertEquals(encoded, restored.encode())

        val before = source.render()
        assertFalse(source.restore("{}"))
        assertEquals(before, source.render())
    }
}
