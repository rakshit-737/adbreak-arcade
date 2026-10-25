package com.adbreak.arcade.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UltimateArcadeUpgradeTest {
    @Test
    fun mastermindKeepsTheSameCodeUntilSolvedOrFourAttempts() {
        val game = MastermindMiniGame()
        val first = game.render()
        assertTrue(game.apply(GameAction.SECONDARY))
        assertTrue(game.render().subtitle.contains("Attempt 2/4"))
        assertEquals(first.buttons, game.render().buttons)
        assertFalse(game.render().isTerminal)
    }

    @Test
    fun triviaDoesNotPutEveryCorrectAnswerInTheFirstSlot() {
        val game = TriviaBlitzGame()
        val positions = mutableSetOf<Int>()
        repeat(4) {
            val state = game.render()
            val expected = listOf("Mars", "6", "Carbon dioxide", "Pacific").firstOrNull { it in state.buttons }
            if (expected != null) positions += state.buttons.indexOf(expected)
            game.apply(GameAction.PRIMARY)
        }
        assertTrue("Answer positions should vary", positions.size > 1)
    }

    @Test
    fun terminalPhaseIsExplicitForHigherLower() {
        val game = HigherLowerGame(java.util.Random(1))
        assertEquals(GamePhase.PLAYING, game.render().phase)
    }

    @Test
    fun unknownRegistryIdsFailInsteadOfSilentlyBecomingTapCounter() {
        var failed = false
        try {
            GameRegistry.create("not_a_real_game")
        } catch (_: IllegalStateException) {
            failed = true
        }
        assertTrue(failed)
    }
}
