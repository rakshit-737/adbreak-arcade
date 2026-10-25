package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class ExistingGamesImprovementTest {
    @Test
    fun tapCounterReportsMilestonesAndReadsLegacyState() {
        val game = TapCounterGame()
        repeat(10) { assertTrue(game.apply(GameAction.PRIMARY)) }

        assertEquals("Milestone reached — 10 taps", game.render().feedback)
        assertTrue(TapCounterGame().restore(JSONObject().apply {
            put("v", 1)
            put("score", 4)
            put("best", 10)
        }.toString()))
    }

    @Test
    fun quickMathUsesUniqueChoicesAndRejectsDuplicateSerializedChoices() {
        val game = QuickMathGame(Random(1))
        val state = game.render()
        assertEquals(4, state.buttons.toSet().size)

        val malformed = JSONObject(game.encode()).put("choices", JSONArray(listOf(1, 1, 2, 3)))
        assertFalse(QuickMathGame().restore(malformed.toString()))
    }

    @Test
    fun numberSequenceKeepsFourVisibleOptionsAndValidatesItsRule() {
        val game = NumberSequenceGame(Random(8))
        assertEquals(4, game.render().buttons.toSet().size)

        val malformed = JSONObject(game.encode()).put("answer", 999)
        assertFalse(NumberSequenceGame().restore(malformed.toString()))
    }

    @Test
    fun memoryHidesThePatternAfterInputAndPersistsPartialProgress() {
        val game = MemoryChallengeGame(Random(5))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertEquals("Repeat the pattern", game.render().subtitle)

        val restored = MemoryChallengeGame(Random(5))
        assertTrue(restored.restore(game.encode()))
        assertTrue(restored.apply(GameAction.SECONDARY))
        assertEquals(2, restored.level)
        assertTrue(restored.score > 0)
    }

    @Test
    fun higherLowerCanRestoreAnOutOfTriesRoundAndOffersReplay() {
        val game = HigherLowerGame(Random(1))
        val state = JSONObject().apply {
            put("v", 1)
            put("low", 1)
            put("high", 20)
            put("secret", 20)
            put("attempts", 1)
            put("message", "Guess 1–20")
            put("roundEnded", false)
            put("wins", 0)
            put("bestStreak", 0)
            put("streak", 0)
        }
        assertTrue(game.restore(state.toString()))
        assertTrue(game.apply(GameAction.SECONDARY))
        assertTrue(game.restore(game.encode()))
        assertTrue(game.render().subtitle.startsWith("Out of tries"))
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertTrue(game.render().subtitle.startsWith("Guess 1–20"))
    }

    @Test
    fun ticTacToeRestoredTerminalStateExposesNewGameAndRejectsImpossibleActiveState() {
        val won = JSONObject().apply {
            put("v", 1)
            put("board", JSONArray(listOf("X", "X", "X", "O", "O", " ", " ", " ", " ")))
            put("over", true)
            put("result", "You win")
        }
        val game = TicTacToeGame()
        assertTrue(game.restore(won.toString()))
        assertEquals(listOf("NEW GAME"), game.render().buttons)

        val impossible = JSONObject().apply {
            put("v", 1)
            put("board", JSONArray(listOf("X", " ", " ", " ", " ", " ", " ", " ", " ")))
            put("over", false)
            put("result", "Your turn")
        }
        assertFalse(TicTacToeGame().restore(impossible.toString()))
    }

    @Test
    fun wordScrambleValidatesThatTheScrambleIsAnAnagram() {
        val game = WordScrambleGame(Random(4))
        val state = game.render()
        val correctIndex = state.buttons.indexOfFirst {
            it.toCharArray().sorted() == state.subtitle.removePrefix("Unscramble: ").trim().toCharArray().sorted()
        }
        assertTrue(correctIndex in 0..3)
        assertTrue(game.apply(state.buttonActions[correctIndex]))
        assertTrue(game.render().feedback.contains("Correct"))

        val malformed = JSONObject(game.encode()).put("scramble", "abc")
        assertFalse(WordScrambleGame().restore(malformed.toString()))
    }

    @Test
    fun everyGameRoundTripsItsRenderedState() {
        GameRegistry.definitions.forEach { source ->
            val restored = GameRegistry.create(source.id)
            assertTrue("${source.id} should restore", restored.restore(source.encode()))
            assertEquals(source.render(), restored.render())
        }
    }
}
