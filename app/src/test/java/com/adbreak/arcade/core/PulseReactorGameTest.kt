package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class PulseReactorGameTest {
    @Test
    fun seededGamesHaveTheSameNotificationSafePatternAndControls() {
        val first = PulseReactorGame(Random(41))
        val second = PulseReactorGame(Random(41))

        assertEquals(first.render(), second.render())
        assertEquals(listOf("1", "2", "3", "4"), first.render().buttons)
        assertEquals(
            listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
            first.render().buttonActions,
        )
        assertEquals(3, JSONObject(first.encode()).getJSONArray("sequence").length())
    }

    @Test
    fun correctPatternEscalatesLevelAndBuildsComboScore() {
        val game = PulseReactorGame(Random(7))

        repeat(2) { expectedLevel ->
            val state = game.render()
            val sequence = JSONObject(game.encode()).getJSONArray("sequence")
            for (index in 0 until sequence.length()) {
                assertTrue(game.apply(actionFor(sequence.getInt(index))))
            }
            assertEquals(expectedLevel + 2, game.level)
            assertTrue(game.score > 0)
        }

        assertEquals(2, game.streak)
        assertEquals(2, game.best)
        assertFalse(game.render().isTerminal)
    }

    @Test
    fun threeMistakesEndTheRunAndNewGameReplaysIt() {
        val game = PulseReactorGame(Random(9))
        val firstPulse = JSONObject(game.encode()).getJSONArray("sequence").getInt(0)
        val wrong = if (firstPulse == 1) GameAction.SECONDARY else GameAction.PRIMARY

        repeat(3) { attempt ->
            assertTrue(game.apply(wrong))
            if (attempt < 2) assertFalse(game.render().isTerminal)
        }

        assertEquals(GamePhase.LOST, game.render().phase)
        assertTrue(game.render().isTerminal)
        assertFalse(game.apply(wrong))
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertFalse(game.render().isTerminal)
        assertEquals(1, game.level)
        assertEquals(0, game.score)
        assertEquals(0, game.best)
    }

    @Test
    fun partialInputPersistsAndInvalidActionsDoNotChangeState() {
        val game = PulseReactorGame(Random(12))
        val sequence = JSONObject(game.encode()).getJSONArray("sequence")
        assertTrue(game.apply(actionFor(sequence.getInt(0))))
        val beforeInvalid = game.render()

        assertFalse(game.apply(GameAction.CELL_3))
        assertFalse(game.apply(GameAction.CELL_9))
        assertFalse(game.apply(GameAction.PAUSE))
        assertFalse(game.apply(GameAction.RESUME))
        assertEquals(beforeInvalid, game.render())

        val restored = PulseReactorGame(Random(99))
        assertTrue(restored.restore(game.encode()))
        assertEquals(game.render(), restored.render())
        assertEquals(game.encode(), restored.encode())
    }

    @Test
    fun patternIsHiddenAfterTheFirstPulseAndRestoredForTheNextLevel() {
        val game = PulseReactorGame(Random(21))
        val sequence = JSONObject(game.encode()).getJSONArray("sequence")
        assertTrue(game.render().subtitle.contains(sequence.getInt(0).toString()))
        assertTrue(game.apply(actionFor(sequence.getInt(0))))
        assertTrue(game.render().subtitle.contains("Repeat the pattern"))

        for (index in 1 until sequence.length()) assertTrue(game.apply(actionFor(sequence.getInt(index))))
        assertTrue(game.render().subtitle.contains("Memorize"))
    }

    @Test
    fun malformedPersistenceIsRejectedWithoutChangingTheGame() {
        val game = PulseReactorGame(Random(15))
        val before = game.render()
        assertFalse(game.restore("{}"))
        assertFalse(game.restore(JSONObject().apply {
            put("v", 1)
            put("sequence", JSONArray(listOf(1, 2, 3)))
            put("input", JSONArray(listOf(1, 2)))
            put("level", 1)
            put("score", 0)
            put("streak", 0)
            put("best", 0)
            put("mistakes", 0)
            put("mistakesRemaining", 99)
            put("terminal", false)
            put("won", false)
            put("feedback", "bad budget")
        }.toString()))
        assertEquals(before, game.render())
    }

    private fun actionFor(pulse: Int): GameAction = when (pulse) {
        1 -> GameAction.PRIMARY
        2 -> GameAction.SECONDARY
        3 -> GameAction.CELL_1
        4 -> GameAction.CELL_2
        else -> error("Unexpected pulse $pulse")
    }
}
