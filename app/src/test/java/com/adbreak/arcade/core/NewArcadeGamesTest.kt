package com.adbreak.arcade.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewArcadeGamesTest {
    private data class Fixture(val create: () -> ArcadeGame, val action: GameAction)

    private val fixtures = listOf(
        Fixture({ RockPaperScissorsGame() }, GameAction.PRIMARY),
        Fixture({ OddOneOutGame() }, GameAction.PRIMARY),
        Fixture({ EmojiDecodeGame() }, GameAction.CELL_1),
        Fixture({ TriviaBlitzGame() }, GameAction.SECONDARY),
        Fixture({ MastermindMiniGame() }, GameAction.CELL_2),
        Fixture({ NimGame() }, GameAction.PRIMARY),
        Fixture({ ConnectThreeGame() }, GameAction.CELL_1),
        Fixture({ GuessThePatternGame() }, GameAction.PRIMARY),
        Fixture({ AnagramChallengeGame() }, GameAction.PRIMARY),
        Fixture({ LightsOutMiniGame() }, GameAction.CELL_1)
    )

    @Test
    fun newGamesHaveStableIdsAndNotificationSizedControls() {
        val games = fixtures.map { it.create() }
        assertEquals(10, games.map { it.id }.toSet().size)
        games.forEach { game ->
            val state = game.render()
            assertTrue(game.id.isNotBlank())
            assertTrue(state.title.isNotBlank())
            assertTrue(state.buttons.size <= 4)
            assertEquals(state.buttons.size, state.buttonActions.size)
            assertTrue(state.cellButtons.size <= 9)
            assertEquals(state.cellButtons.size, state.cellActions.size)
        }
    }

    @Test
    fun everyNewGameRoundTripsAfterAnAction() {
        fixtures.forEach { fixture ->
            val source = fixture.create()
            assertTrue("${source.id} should accept its mapped action", source.apply(fixture.action))
            val encoded = source.encode()
            val restored = fixture.create()
            assertTrue("${source.id} should restore", restored.restore(encoded))
            assertEquals("${source.id} render should survive restore", source.render(), restored.render())
            assertEquals("${source.id} restore should be stable", encoded, restored.encode())
        }
    }

    @Test
    fun malformedStateIsRejectedWithoutChangingTheGame() {
        fixtures.forEach { fixture ->
            val game = fixture.create()
            game.apply(fixture.action)
            val before = game.render()
            assertFalse("${game.id} should reject malformed state", game.restore("{}"))
            assertEquals("${game.id} should remain unchanged after rejection", before, game.render())
        }
    }

    @Test
    fun resetReturnsEveryNewGameToItsInitialState() {
        fixtures.forEach { fixture ->
            val game = fixture.create()
            game.apply(fixture.action)
            assertTrue(game.apply(GameAction.RESET))
            val fresh = fixture.create()
            assertEquals("${game.id} reset should match a fresh game", fresh.render(), game.render())
            val restored = fixture.create()
            assertTrue("${game.id} reset state should restore", restored.restore(game.encode()))
            assertEquals("${game.id} reset state should restore", fresh.render(), restored.render())
        }
    }

    @Test
    fun lightsOutHasACompleteDeterministicSolution() {
        val game = LightsOutMiniGame()
        assertTrue(game.apply(GameAction.CELL_1))
        assertTrue(game.apply(GameAction.CELL_5))
        assertTrue(game.apply(GameAction.CELL_9))
        assertTrue(game.render().subtitle.contains("Solved"))
        assertFalse(game.apply(GameAction.CELL_1))
    }

    @Test
    fun lightsOutPreservesLifetimeWinsAcrossNewRoundsAndRestore() {
        val game = LightsOutMiniGame()
        repeat(2) {
            assertTrue(game.apply(GameAction.CELL_1))
            assertTrue(game.apply(GameAction.CELL_5))
            assertTrue(game.apply(GameAction.CELL_9))
            if (it == 0) assertTrue(game.apply(GameAction.NEW_GAME))
        }
        assertTrue(game.render().scoreLine.contains("Wins 2"))
        val restored = LightsOutMiniGame()
        assertTrue(restored.restore(game.encode()))
        assertEquals(game.render(), restored.render())
    }

    @Test
    fun triviaMigratesLegacyFourQuestionState() {
        val legacy = JSONObject().apply {
            put("v", 1)
            put("questionIndex", 0)
            put("attempts", 4)
            put("score", 2)
            put("streak", 1)
            put("best", 2)
            put("feedback", "Correct")
        }.toString()
        val game = TriviaBlitzGame()
        assertTrue(game.restore(legacy))
        assertEquals(2, JSONObject(game.encode()).optInt("v"))
        assertEquals(4, JSONObject(game.encode()).optInt("attempts"))
    }

    @Test
    fun connectThreeWinnerDetectionCoversRowsColumnsAndDiagonals() {
        assertTrue(ConnectThreeGame.hasWinner("XXX......", 'X'))
        assertTrue(ConnectThreeGame.hasWinner("X..X..X..", 'X'))
        assertTrue(ConnectThreeGame.hasWinner("X...X...X", 'X'))
        assertFalse(ConnectThreeGame.hasWinner("XOXXOOOX.", 'X'))
    }
}
