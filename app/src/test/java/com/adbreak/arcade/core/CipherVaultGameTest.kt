package com.adbreak.arcade.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class CipherVaultGameTest {
    @Test
    fun repeatedSymbolsUseExactThenMultiplicityAwareMisplacedClues() {
        assertEquals(CipherVaultGame.ClueResult(2, 2), CipherVaultGame.evaluateClues("1122", "1212"))
        assertEquals(CipherVaultGame.ClueResult(0, 4), CipherVaultGame.evaluateClues("1122", "2211"))
        assertEquals(CipherVaultGame.ClueResult(1, 0), CipherVaultGame.evaluateClues("1234", "1111"))
        assertEquals(CipherVaultGame.ClueResult(0, 2), CipherVaultGame.evaluateClues("1123", "3331"))
    }

    @Test
    fun seededGamesExposeOnlyNotificationSafeStagedControls() {
        val first = CipherVaultGame(Random(41))
        val second = CipherVaultGame(Random(41))

        assertEquals(first.render(), second.render())
        assertEquals(9, first.render().cellButtons.size)
        assertEquals(9, first.render().cellActions.size)
        // Empty/exhausted controls stay visible with an explicit disabled reason.
        assertEquals(listOf("CLEAR DRAFT", "HINT (3)"), first.render().buttons)
        assertEquals(listOf(GameAction.PRIMARY, GameAction.SECONDARY), first.render().buttonActions)
        assertTrue(first.render().disabledActions.containsKey(GameAction.PRIMARY))
        assertTrue(first.render().cellButtons.none { it.contains("EditText", ignoreCase = true) })
        assertEquals(JSONObject(first.encode()).getString("secret").length, first.render().scoreLine.substringAfter("Guess ").substringBefore("  ·").count { it == '·' })
    }

    @Test
    fun stagedGuessCanBeSubmittedAndDifficultyScoreAndStreakProgress() {
        val game = CipherVaultGame(Random(7))

        solveCurrentRound(game)
        assertEquals(GamePhase.WON, game.render().phase)
        assertTrue(game.render().isTerminal)
        assertEquals(1, game.solved)
        assertEquals(1, game.streak)
        assertEquals(1, game.bestStreak)
        assertTrue(game.score > 0)
        val scoreAfterFirstWin = game.score
        assertFalse(game.apply(GameAction.PRIMARY))

        assertTrue(game.apply(GameAction.NEW_GAME))
        assertFalse(game.render().isTerminal)
        assertEquals(2, JSONObject(game.encode()).getInt("difficulty"))
        solveCurrentRound(game)
        assertEquals(2, game.streak)
        assertEquals(2, game.bestStreak)
        assertTrue(game.score > scoreAfterFirstWin)
    }

    @Test
    fun hintsAreLimitedAndPersistWithAStagedDraft() {
        val game = CipherVaultGame(Random(12))
        val before = game.render()
        assertTrue(game.apply(GameAction.SECONDARY))
        assertEquals(2, JSONObject(game.encode()).getInt("hintsAvailable"))
        assertTrue(game.apply(GameAction.CELL_4))
        assertTrue(game.apply(GameAction.CELL_3)) // beginner vault uses digits 1–4

        val encoded = game.encode()
        val restored = CipherVaultGame(Random(99))
        assertTrue(restored.restore(encoded))
        assertEquals(game.render(), restored.render())
        assertEquals(encoded, restored.encode())
        assertFalse(game.render() == before)

        assertTrue(game.apply(GameAction.SECONDARY))
        assertTrue(game.apply(GameAction.SECONDARY))
        val afterBudget = game.render()
        assertFalse(game.apply(GameAction.SECONDARY))
        assertEquals(afterBudget, game.render())
    }

    @Test
    fun terminalLossRejectsFurtherGuessingAndReplaysAsANewRound() {
        val game = CipherVaultGame(Random(9))
        val secret = JSONObject(game.encode()).getString("secret")
        val wrong = if (secret.first() == '1') "2${secret.drop(1)}" else "1${secret.drop(1)}"

        repeat(6) {
            stage(game, wrong)
            assertTrue(game.apply(GameAction.PRIMARY))
        }
        assertEquals(GamePhase.LOST, game.render().phase)
        assertTrue(game.render().isTerminal)
        val terminal = game.render()
        assertFalse(game.apply(GameAction.CELL_1))
        assertEquals(terminal, game.render())
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertFalse(game.render().isTerminal)
        assertEquals(0, JSONObject(game.encode()).getInt("attempts"))
    }

    @Test
    fun invalidActionsAndMalformedStateDoNotMutateTheGame() {
        val game = CipherVaultGame(Random(15))
        val initial = game.render()
        assertFalse(game.apply(GameAction.PRIMARY))
        assertFalse(game.apply(GameAction.PAUSE))
        assertFalse(game.apply(GameAction.RESUME))
        assertEquals(initial, game.render())

        assertFalse(game.restore("{}"))
        assertEquals(initial, game.render())

        val malformed = JSONObject(game.encode()).apply { put("hintsAvailable", 99) }.toString()
        assertFalse(game.restore(malformed))
        assertEquals(initial, game.render())
    }

    private fun solveCurrentRound(game: CipherVaultGame) {
        val secret = JSONObject(game.encode()).getString("secret")
        stage(game, secret)
        assertTrue(game.apply(GameAction.PRIMARY))
    }

    private fun stage(game: CipherVaultGame, code: String) {
        code.forEach { digit ->
            assertTrue(game.apply(actionFor(digit)))
        }
    }

    private fun actionFor(digit: Char): GameAction = when (digit) {
        '1' -> GameAction.CELL_1
        '2' -> GameAction.CELL_2
        '3' -> GameAction.CELL_3
        '4' -> GameAction.CELL_4
        '5' -> GameAction.CELL_5
        '6' -> GameAction.CELL_6
        '7' -> GameAction.CELL_7
        '8' -> GameAction.CELL_8
        '9' -> GameAction.CELL_9
        else -> error("Unexpected cipher digit $digit")
    }
}
