package com.adbreak.arcade.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class GameplayExcellenceTest {
    @Test
    fun tapCounterHasAReplayableTargetAndRejectsMovesAfterTheSprint() {
        val game = TapCounterGame()
        repeat(20) { assertTrue(game.apply(GameAction.PRIMARY)) }

        assertTrue(game.render().isTerminal)
        assertEquals(GamePhase.WON, game.render().phase)
        assertFalse(game.apply(GameAction.PRIMARY))
        assertTrue(game.best > 0)
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertEquals(0, game.score)
        assertTrue(game.best > 0)
    }

    @Test
    fun quickMathKeepsDivisionExactAndReportsAccuracy() {
        val game = QuickMathGame(Random(14))
        val initial = JSONObject(game.encode())
        val prompt = initial.getString("prompt")
        val answer = initial.getInt("answer")
        if (prompt.contains("÷")) {
            val parts = prompt.split(" ÷ ")
            assertEquals(0, parts[0].toInt() % parts[1].toInt())
        }
        val correctIndex = game.render().buttons.indexOf(answer.toString())
        assertTrue(correctIndex in 0..3)
        assertTrue(game.apply(game.render().buttonActions[correctIndex]))
        assertEquals(100, game.render().scoreLine.substringAfter("Accuracy ").substringBefore('%').toInt())
    }

    @Test
    fun memoryHasAThreeLifeTerminalFailureAndReplay() {
        val game = MemoryChallengeGame(Random(2))
        repeat(3) {
            assertTrue(game.apply(GameAction.SECONDARY))
            if (it < 2) assertFalse(game.render().isTerminal)
        }
        assertEquals(GamePhase.LOST, game.render().phase)
        assertFalse(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertFalse(game.render().isTerminal)
    }

    @Test
    fun wordScrambleDoesNotImmediatelyRepeatTheSolvedWord() {
        val game = WordScrambleGame(Random(9))
        val oldScramble = game.render().subtitle.removePrefix("Unscramble: ")
        val correctIndex = game.render().buttons.indexOfFirst {
            it.toCharArray().sorted() == oldScramble.toCharArray().sorted()
        }
        assertTrue(correctIndex in 0..3)
        assertTrue(game.apply(game.render().buttonActions[correctIndex]))
        assertNotEquals(oldScramble, game.render().subtitle.removePrefix("Unscramble: "))
    }

    @Test
    fun numberSequenceOffersOneCorrectAnswerAcrossGeneratedFamilies() {
        repeat(40) { seed ->
            val game = NumberSequenceGame(Random(seed.toLong()))
            val state = JSONObject(game.encode())
            assertTrue(state.getString("pattern").isNotBlank())
            assertEquals(4, game.render().buttons.toSet().size)
            assertTrue(game.render().buttons.contains(state.getInt("answer").toString()))
            assertFalse(game.apply(GameAction.CELL_9))
        }
    }
}
