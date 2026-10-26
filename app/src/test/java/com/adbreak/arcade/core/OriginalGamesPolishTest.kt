package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class OriginalGamesPolishTest {
    private val factories: List<() -> ArcadeGame> = listOf(
        { TapCounterGame() }, { QuickMathGame(Random(8)) }, { TicTacToeGame() },
        { HigherLowerGame(Random(8)) }, { MemoryChallengeGame(Random(8)) },
        { WordScrambleGame(Random(8)) }, { NumberSequenceGame(Random(8)) },
    )
    private val quizzes: List<() -> ArcadeGame> = listOf(
        { QuickMathGame(Random(8)) }, { WordScrambleGame(Random(8)) }, { NumberSequenceGame(Random(8)) },
    )
    private val numberActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2)

    private fun answer(game: ArcadeGame, correct: Boolean = true) {
        val json = JSONObject(game.encode())
        val expected = if (game is WordScrambleGame) json.getString("word") else json.getInt("answer").toString()
        val view = game.render()
        val index = view.buttons.indexOfFirst { (it == expected) == correct }
        assertTrue("${game.id}: answer is visible", index >= 0)
        assertTrue(game.apply(view.buttonActions[index]))
    }

    private fun assertRestores(game: ArcadeGame, factory: () -> ArcadeGame) {
        val restored = factory()
        assertTrue("${game.id}: save accepted", restored.restore(game.encode()))
        assertEquals("${game.id}: rendered state survives", game.render(), restored.render())
    }

    @Test fun freshGamesHaveHonestProgressNoUnearnedRecordAndStableRendering() {
        factories.forEach { factory ->
            val game = factory()
            val before = game.encode()
            val view = game.render()
            assertNotNull(game.id, view.progress)
            assertEquals(0, view.progress!!.current)
            assertTrue(view.progress!!.maximum > 0)
            assertNull(game.id, view.record)
            repeat(4) { assertEquals(view, game.render()) }
            assertEquals(before, game.encode())
            assertRestores(game, factory)
        }
    }

    @Test fun tapSprintEndsOnceAndMigratesUnlimitedLegacyCountsWithoutFakeRecords() {
        val game = TapCounterGame()
        repeat(20) { index ->
            assertTrue(game.apply(GameAction.PRIMARY))
            assertEquals(index + 1, game.render().progress!!.current)
            if (index < 19) assertNull(game.render().record)
        }
        assertEquals(50, game.score)
        assertEquals(50, game.render().record!!.value)
        val terminal = game.encode()
        assertFalse(game.apply(GameAction.PRIMARY))
        assertEquals(terminal, game.encode())
        assertEquals(1, game.roundsCompleted)
        assertRestores(game) { TapCounterGame() }
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertEquals(0, game.render().progress!!.current)
        assertNull(game.render().record)

        val legacy = TapCounterGame()
        assertTrue(legacy.restore("{\"v\":1,\"score\":120,\"best\":160}"))
        assertEquals(120, legacy.score)
        assertTrue(legacy.render().isTerminal)
        assertNull(legacy.render().record)
        assertRestores(legacy) { TapCounterGame() }
    }

    @Test fun quizSetsHaveTenAnswersAPassBoundaryAndTerminalReplayWithoutExtraPoints() {
        quizzes.forEach { factory ->
            for (right in listOf(6, 7, 10)) {
                val game = factory()
                repeat(10) { index ->
                    assertFalse(game.render().isTerminal)
                    assertNull(game.render().record)
                    answer(game, correct = index < right)
                    assertEquals(index + 1, game.render().progress!!.current)
                    assertRestores(game, factory)
                }
                assertEquals(if (right >= 7) GamePhase.WON else GamePhase.LOST, game.render().phase)
                assertEquals(right, game.render().record!!.value)
                assertEquals(listOf(GameAction.NEW_GAME), game.render().buttonActions)
                val finished = game.encode()
                numberActions.forEach { assertFalse(game.apply(it)) }
                assertEquals(finished, game.encode())
                assertTrue(game.apply(GameAction.NEW_GAME))
                assertFalse(game.render().isTerminal)
                assertEquals(0, game.render().progress!!.current)
                assertNull(game.render().record)
            }
        }
    }

    @Test fun quizLegacyPromptsAndStatsSurviveButOldUnlimitedScoresCannotClaimNewRecords() {
        quizzes.forEach { factory ->
            val old = factory()
            repeat(3) { answer(old) }
            val json = JSONObject(old.encode()).put("v", 3)
            if (old is NumberSequenceGame) json.remove("choices") // v3 always put the answer first.
            val restored = factory()
            assertTrue(restored.restore(json.toString()))
            assertEquals(json.getInt("score"), JSONObject(restored.encode()).getInt("score"))
            assertEquals(0, restored.render().progress!!.current)
            if (old !is NumberSequenceGame) assertEquals(old.render().buttons, restored.render().buttons)
            else assertEquals(json.getInt("answer").toString(), restored.render().buttons.first())
            repeat(10) { answer(restored) }
            assertNull(restored.render().record)
            assertRestores(restored, factory)
            restored.apply(GameAction.NEW_GAME)
            repeat(10) { answer(restored) }
            assertEquals(10, restored.render().record!!.value)
        }
    }

    @Test fun generatedQuizzesStayValidAndAnswerPositionsVaryAcrossEveryFamily() {
        val families = mutableSetOf<String>()
        val sequencePositions = mutableSetOf<Int>()
        val operations = mutableSetOf<String>()
        repeat(40) { seed ->
            val math = QuickMathGame(Random(seed.toLong()))
            val word = WordScrambleGame(Random(seed.toLong()))
            val sequence = NumberSequenceGame(Random(seed.toLong()))
            var previousWord = ""
            repeat(10) {
                val m = JSONObject(math.encode())
                val left = m.getInt("left")
                val right = m.getInt("right")
                val operation = m.getString("operation")
                operations += operation
                val expected = when (operation) {
                    "+" -> left + right
                    "−" -> left - right
                    "×" -> left * right
                    else -> { assertEquals(0, left % right); left / right }
                }
                assertEquals(expected, m.getInt("answer"))
                assertTrue(expected >= 0)

                val w = JSONObject(word.encode())
                val original = w.getString("word")
                assertNotEquals(original, w.getString("scramble"))
                assertNotEquals(previousWord, original)
                assertEquals(original.toList().sorted(), w.getString("scramble").toList().sorted())
                previousWord = original

                val n = JSONObject(sequence.encode())
                families += n.getString("pattern")
                sequencePositions += sequence.render().buttons.indexOf(n.getInt("answer").toString())
                listOf(math, word, sequence).forEach { game ->
                    assertEquals(4, game.render().buttons.toSet().size)
                    assertEquals(4, game.render().buttonActions.size)
                    assertFalse(game.apply(GameAction.CELL_9))
                    answer(game)
                }
            }
        }
        assertEquals(setOf("+", "−", "×", "÷"), operations)
        assertEquals(setOf("arithmetic", "geometric", "alternating", "squares", "mixed"), families)
        assertEquals(setOf(0, 1, 2, 3), sequencePositions)
    }

    @Test fun numberSequenceCannotBePerfectByAlwaysPressingTheFirstButton() {
        val game = NumberSequenceGame(Random(8))
        repeat(10) { assertTrue(game.apply(GameAction.PRIMARY)) }
        assertTrue(game.render().isTerminal)
        assertTrue(game.render().record!!.value < 10)
        assertRestores(game) { NumberSequenceGame(Random(987)) }
    }

    @Test fun midpointStrategyWinsForEverySecretAtEveryDifficulty() {
        for (maximum in 20..50 step 5) {
            val limit = if (maximum > 31) 6 else 5
            for (secret in 1..maximum) {
                val game = HigherLowerGame(Random(1))
                val json = JSONObject(game.encode()).put("rangeMax", maximum)
                    .put("high", maximum).put("secret", secret).put("attempts", limit).put("attemptLimit", limit)
                assertTrue(game.restore(json.toString()))
                var moves = 0
                while (!game.render().isTerminal && moves < limit) {
                    assertTrue(game.apply(GameAction.PRIMARY))
                    assertRestores(game) { HigherLowerGame(Random(2)) }
                    moves++
                }
                assertEquals("range $maximum, secret $secret", GamePhase.WON, game.render().phase)
                assertEquals(maximum - 1, game.render().progress!!.current)
                assertEquals(1, game.render().record!!.value)
                val ended = game.encode()
                assertFalse(game.apply(GameAction.PRIMARY))
                assertEquals(ended, game.encode())
            }
        }
    }

    @Test fun higherLowerStreakSurvivesNarrowingButNotAbandoningAPartialRound() {
        val game = HigherLowerGame(Random(8))
        repeat(10) { round ->
            var moves = 0
            while (!game.render().isTerminal && moves++ < 6) assertTrue(game.apply(GameAction.PRIMARY))
            assertEquals(GamePhase.WON, game.render().phase)
            assertEquals(round + 1, game.bestStreak)
            assertTrue(game.apply(GameAction.NEW_GAME))
        }
        val fixture = JSONObject(game.encode()).put("secret", 50)
        assertTrue(game.restore(fixture.toString()))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertEquals(10, JSONObject(game.encode()).getInt("streak"))
        game.apply(GameAction.RESET)
        assertEquals(0, JSONObject(game.encode()).getInt("streak"))
        assertNull(game.render().record)
    }

    @Test fun higherLowerDisablesDuplicateGuessesAndMigratesLargeLegacyRanges() {
        val game = HigherLowerGame(Random(8))
        val fixture = JSONObject(game.encode()).put("low", 10).put("high", 11).put("secret", 11).put("attempts", 2)
        assertTrue(game.restore(fixture.toString()))
        assertTrue(game.render().disabledActions.getValue(GameAction.SECONDARY).contains("MID"))
        val before = game.encode()
        assertFalse(game.apply(GameAction.SECONDARY))
        assertEquals(before, game.encode())
        assertTrue(game.apply(GameAction.CELL_1))
        assertEquals(GamePhase.WON, game.render().phase)

        val legacy = JSONObject(HigherLowerGame().encode()).put("v", 3)
            .put("rangeMax", 50).put("high", 50).put("secret", 50).put("attempts", 5)
        assertTrue(game.restore(legacy.toString()))
        assertEquals(6, JSONObject(game.encode()).getInt("attempts"))
        assertRestores(game) { HigherLowerGame() }
        repeat(6) { if (!game.render().isTerminal) game.apply(GameAction.PRIMARY) }
        assertEquals(GamePhase.WON, game.render().phase)
    }

    private fun completeMemoryPattern(game: MemoryChallengeGame) {
        val sequence = JSONObject(game.encode()).getJSONArray("sequence")
        for (index in 0 until sequence.length()) {
            assertTrue(game.apply(numberActions[sequence.getInt(index) - 1]))
        }
    }

    @Test fun memoryHasATwentyInputClearEarnedZeroMistakeRecordAndVariedReplay() {
        val game = MemoryChallengeGame(Random(8))
        repeat(5) { level ->
            assertNull(game.render().record)
            completeMemoryPattern(game)
            assertEquals(level + 1, game.render().progress!!.current)
            assertRestores(game) { MemoryChallengeGame(Random(42)) }
        }
        assertEquals(20, JSONObject(game.encode()).getInt("totalInputs"))
        assertEquals(GamePhase.WON, game.render().phase)
        assertEquals(0, game.render().record!!.value)
        assertFalse(game.render().record!!.higherIsBetter)
        val ended = game.encode()
        numberActions.forEach { assertFalse(game.apply(it)) }
        assertEquals(ended, game.encode())
        val openings = mutableSetOf<String>()
        repeat(10) {
            assertTrue(game.apply(GameAction.NEW_GAME))
            assertEquals(0, game.render().progress!!.current)
            assertNull(game.render().record)
            openings += JSONObject(game.encode()).getJSONArray("sequence").toString()
        }
        assertTrue(openings.size > 1)
    }

    @Test fun memoryMistakesRevealThePatternAndCannotFarmScoreOrHideTheCost() {
        val game = MemoryChallengeGame(Random(8))
        repeat(5) {
            val first = JSONObject(game.encode()).getJSONArray("sequence").getInt(0)
            val scoreBefore = game.score
            assertTrue(game.apply(numberActions[first % 4]))
            assertTrue(game.render().subtitle.startsWith("Pattern:"))
            assertTrue(game.render().feedback.contains("needed $first"))
            assertEquals(scoreBefore, game.score)
            assertRestores(game) { MemoryChallengeGame() }
            completeMemoryPattern(game)
        }
        assertEquals(GamePhase.WON, game.render().phase)
        assertEquals(5, game.render().record!!.value)
        assertEquals(200, game.score)
    }

    @Test fun memoryPreservesLongLegacyPatternsAndDoesNotCompareThemWithShortRuns() {
        val legacy = JSONObject(MemoryChallengeGame().encode()).put("v", 3).put("level", 8).put("best", 8)
            .put("sequence", JSONArray(List(9) { 1 })).put("message", "Pattern: 1 1 1 1 1 1 1 1 1")
        val game = MemoryChallengeGame()
        assertTrue(game.restore(legacy.toString()))
        assertEquals(8, game.render().progress!!.maximum)
        assertRestores(game) { MemoryChallengeGame() }
        completeMemoryPattern(game)
        assertEquals(GamePhase.WON, game.render().phase)
        assertNull(game.render().record)
        game.apply(GameAction.NEW_GAME)
        assertEquals(5, game.render().progress!!.maximum)
    }

    @Test fun memoryLossIsTerminalAndSurvivesRefreshWithoutAwardingARecord() {
        val game = MemoryChallengeGame(Random(8))
        repeat(3) { assertTrue(game.apply(GameAction.SECONDARY)) }
        assertEquals(GamePhase.LOST, game.render().phase)
        assertNull(game.render().record)
        assertRestores(game) { MemoryChallengeGame() }
        val before = game.encode()
        assertFalse(game.apply(GameAction.PRIMARY))
        assertEquals(before, game.encode())
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertFalse(game.render().isTerminal)
    }

    @Test fun injectedRandomIsDeterministicThroughACompleteSetAndReplay() {
        quizzes.forEach { factory ->
            val first = factory()
            val second = factory()
            repeat(2) {
                repeat(10) {
                    assertEquals(first.render(), second.render())
                    answer(first)
                    answer(second)
                }
                assertEquals(first.render(), second.render())
                first.apply(GameAction.NEW_GAME)
                second.apply(GameAction.NEW_GAME)
            }
        }
        val first = MemoryChallengeGame(Random(8))
        val second = MemoryChallengeGame(Random(8))
        repeat(2) {
            repeat(5) {
                assertEquals(first.render(), second.render())
                completeMemoryPattern(first)
                completeMemoryPattern(second)
            }
            first.apply(GameAction.NEW_GAME)
            second.apply(GameAction.NEW_GAME)
        }
    }

    private fun drawPath(game: TicTacToeGame): List<GameAction>? {
        if (game.render().isTerminal) return if (game.render().phase == GamePhase.DRAW) emptyList() else null
        val view = game.render()
        for (action in view.cellActions.filter { it !in view.disabledActions }) {
            val next = TicTacToeGame()
            assertTrue(next.restore(game.encode()))
            assertTrue(next.apply(action))
            val tail = drawPath(next)
            if (tail != null) return listOf(action) + tail
        }
        return null
    }

    @Test fun ticTacToeRewardsDrawsPreservesTheFinalBoardAndPreventsResetStreakFarming() {
        val game = TicTacToeGame()
        val path = drawPath(game) ?: error("A perfect opponent must allow a draw")
        assertEquals(5, path.size)
        repeat(2) { round ->
            path.forEach { action ->
                assertTrue(game.apply(action))
                assertRestores(game) { TicTacToeGame() }
            }
            assertEquals(GamePhase.DRAW, game.render().phase)
            assertEquals(round + 1, game.render().record!!.value)
            assertEquals(round + 1, game.score)
            assertEquals(9, game.render().cellButtons.size)
            assertEquals(9, game.render().disabledActions.size)
            val encoded = game.encode()
            game.render().cellActions.forEach { assertFalse(game.apply(it)) }
            assertEquals(encoded, game.encode())
            assertTrue(game.apply(GameAction.NEW_GAME))
        }
        assertTrue(game.apply(path.first()))
        assertTrue(game.render().disabledActions.containsKey(path.first()))
        game.apply(GameAction.RESET)
        path.forEach { assertTrue(game.apply(it)) }
        assertEquals(1, game.render().record!!.value)
    }

    @Test fun newSavedStateFieldsRejectImpossibleProgressWithoutMutatingTheGame() {
        quizzes.forEach { factory ->
            val game = factory()
            val before = game.encode()
            assertFalse(game.restore(JSONObject(before).put("runAttempts", 11).toString()))
            assertFalse(game.restore(JSONObject(before).put("runCorrect", 1).toString()))
            assertEquals(before, game.encode())
        }
        val sequence = NumberSequenceGame(Random(8))
        assertFalse(sequence.restore(JSONObject(sequence.encode()).put("choices", JSONArray(listOf(1, 1, 1, 1))).toString()))
        val memory = MemoryChallengeGame()
        assertFalse(memory.restore(JSONObject(memory.encode()).put("lives", 0).toString()))
        assertFalse(memory.restore(JSONObject(memory.encode()).put("targetLevel", 101).toString()))
    }
}
