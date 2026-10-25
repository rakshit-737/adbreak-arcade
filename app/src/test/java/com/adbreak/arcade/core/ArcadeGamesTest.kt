package com.adbreak.arcade.core

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class ArcadeGamesTest {
    @Test fun everyRegisteredGameHasStableIdAndCanBeRecreated() {
        val games = GameRegistry.definitions
        assertEquals(games.size, games.map { it.id }.toSet().size)
        games.forEach {
            assertTrue(it.id.isNotBlank())
            assertTrue(it.render().title.isNotBlank())
            assertEquals(it.id, GameRegistry.create(it.id).id)
        }
        assertEquals(17, games.size)
    }
    @Test fun ticTacToeRejectsOccupiedMoveAndCanWin() { val game = TicTacToeGame(); assertTrue(game.apply(GameAction.CELL_1)); assertFalse(game.apply(GameAction.CELL_1)); assertTrue(game.apply(GameAction.RESET)); assertTrue(game.render().scoreLine.contains("·")) }
    @Test fun ticTacToeDetectsEveryWinLine() {
        val lines = listOf(intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8), intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8), intArrayOf(0, 4, 8), intArrayOf(2, 4, 6))
        lines.forEach { line -> val board = MutableList(9) { ' ' }; line.forEach { board[it] = 'X' }; assertTrue("line ${line.contentToString()}", TicTacToeGame.hasWinner(board, 'X')) }
    }
    @Test fun ticTacToeDetectsDrawBoard() { assertFalse(TicTacToeGame.hasWinner(listOf('X','O','X','X','O','O','O','X','X'), 'X')); assertFalse(TicTacToeGame.hasWinner(listOf('X','O','X','X','O','O','O','X','X'), 'O')) }
    @Test fun ticTacToeAiBlocksImmediateThreat() { val game = TicTacToeGame(); assertTrue(game.apply(GameAction.CELL_1)); assertTrue(game.apply(GameAction.CELL_2)); assertEquals("3:O", game.render().cellButtons[2]) }
    @Test fun ticTacToeRestoredDrawRejectsMoves() { val game = TicTacToeGame(); val state = JSONObject().apply { put("v", 1); put("board", org.json.JSONArray(listOf("X", "O", "X", "X", "O", "O", "O", "X", "X"))); put("over", true); put("result", "Draw") }.toString(); assertTrue(game.restore(state)); assertFalse(game.apply(GameAction.CELL_1)) }
    @Test fun quickMathAcceptsOnlyKnownChoiceActions() { val game = QuickMathGame(java.util.Random(1)); assertFalse(game.apply(GameAction.CELL_9)); assertTrue(game.apply(GameAction.PRIMARY)) }
    @Test fun higherLowerKeepsTerminalFeedback() { val game = HigherLowerGame(java.util.Random(1)); val state = JSONObject().apply { put("v", 1); put("low", 1); put("high", 20); put("secret", 10); put("attempts", 5); put("message", "Guess 1–20"); put("roundEnded", false); put("wins", 0); put("bestStreak", 0); put("streak", 0) }.toString(); assertTrue(game.restore(state)); assertTrue(game.apply(GameAction.PRIMARY)); assertTrue(game.render().subtitle.contains("Correct")); assertEquals(listOf("NEW ROUND"), game.render().buttons) }
    @Test fun higherLowerEnforcesAttemptLimit() { val game = HigherLowerGame(java.util.Random(1)); val state = JSONObject().apply { put("v", 1); put("low", 1); put("high", 20); put("secret", 20); put("attempts", 1); put("message", "Guess 1–20"); put("roundEnded", false); put("wins", 0); put("bestStreak", 0); put("streak", 0) }.toString(); assertTrue(game.restore(state)); assertTrue(game.apply(GameAction.SECONDARY)); assertTrue(game.render().subtitle.contains("Out of tries")); assertFalse(game.apply(GameAction.PRIMARY)) }
    @Test fun memoryPersistsPartialInput() { val game = MemoryChallengeGame(); assertTrue(game.apply(GameAction.PRIMARY)); val restored = MemoryChallengeGame(); assertTrue(restored.restore(game.encode())); assertTrue(restored.apply(GameAction.SECONDARY)); assertEquals(2, restored.level) }
    @Test fun wordScrambleUsesVisibleChoiceMapping() { val game = WordScrambleGame(java.util.Random(4)); val state = game.render(); assertEquals(4, state.buttons.size); assertEquals(4, state.buttons.toSet().size); val correctIndex = state.buttons.indexOfFirst { it.toCharArray().sorted() == state.subtitle.removePrefix("Unscramble: ").trim().toCharArray().sorted() }; assertTrue(correctIndex in 0..3); assertTrue(game.apply(state.buttonActions[correctIndex])); assertEquals(1, game.score) }
    @Test fun numberSequenceScoresTheActualMissingTerm() { val game = NumberSequenceGame(java.util.Random(8)); val state = JSONObject(game.encode()); val start = state.getInt("start"); val step = state.getInt("step"); val expected = if (state.getBoolean("geometric")) start * step * step * step else start + step * 3; assertEquals(expected, state.getInt("answer")); assertTrue(game.apply(GameAction.PRIMARY)); assertEquals(1, game.score) }
    @Test fun otherGamesResetAndPersist() { listOf(HigherLowerGame(), MemoryChallengeGame(), WordScrambleGame(), NumberSequenceGame()).forEach { game -> game.apply(GameAction.RESET); assertTrue(game.restore(game.encode())) } }
}
