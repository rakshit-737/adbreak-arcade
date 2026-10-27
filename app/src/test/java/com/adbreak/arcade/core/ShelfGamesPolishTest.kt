package com.adbreak.arcade.core

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class ShelfGamesPolishTest {
    private val choices = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2)
    private val cells = (0..8).map { GameAction.values()[GameAction.CELL_1.ordinal + it] }

    private fun roundTrip(game: ArcadeGame, create: () -> ArcadeGame): ArcadeGame {
        val saved = game.encode()
        return create().also {
            assertTrue("${game.id}: restore", it.restore(saved))
            assertEquals("${game.id}: view", game.render(), it.render())
            assertEquals("${game.id}: stable encoding", saved, it.encode())
        }
    }

    @Test
    fun allNineThrowsHaveCorrectRulesAndFiveThrowResults() {
        for (arcade in 0..2) for (player in 0..2) {
            val game = RockPaperScissorsGame(Random(1))
            val saved = JSONObject(game.encode()).put("computerMove", arcade).put("movePlan", arcade.toString().repeat(5))
            assertTrue(game.restore(saved.toString()))
            repeat(5) { assertTrue(game.apply(choices[player])) }
            val state = game.render()
            val expected = when {
                player == arcade -> GamePhase.DRAW
                (player == 0 && arcade == 2) || (player == 1 && arcade == 0) || (player == 2 && arcade == 1) -> GamePhase.WON
                else -> GamePhase.LOST
            }
            assertEquals(expected, state.phase)
            assertEquals(if (expected == GamePhase.WON) 5 else 0, state.record!!.value)
            assertEquals(GameProgress(5, 5, "Throws played"), state.progress)
            val terminal = game.encode()
            assertFalse(game.apply(choices[player]))
            assertEquals(terminal, game.encode())
            roundTrip(game) { RockPaperScissorsGame(Random(9)) }
        }
    }

    @Test
    fun precommittedThrowsSurviveRestoreWithDifferentRandomSource() {
        val game = RockPaperScissorsGame(Random(42))
        game.apply(GameAction.PRIMARY)
        game.apply(GameAction.SECONDARY)
        val restored = roundTrip(game) { RockPaperScissorsGame(Random(999)) }
        for (action in listOf(GameAction.CELL_1, GameAction.PRIMARY, GameAction.SECONDARY)) {
            assertTrue(game.apply(action))
            assertTrue(restored.apply(action))
            assertEquals(game.render(), restored.render())
            assertEquals(game.encode(), restored.encode())
        }
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertFalse(game.render().isTerminal)
        assertEquals(0, game.render().progress!!.current)
    }

    private data class Quiz(val create: () -> ArcadeGame, val answers: List<String>, val indexKey: String = "puzzleIndex")
    private val quizzes = listOf(
        Quiz({ OddOneOutGame() }, listOf("ROSE", "CIRCLE", "9", "MARCH", "7")),
        Quiz({ EmojiDecodeGame() }, listOf("Hot dog", "Sunflower", "Iced tea", "Honey bee")),
        Quiz({ TriviaBlitzGame() }, listOf("Mars", "6", "Carbon dioxide", "Pacific", "Cheetah", "Piano", "2", "Triangle"), "questionIndex"),
        Quiz({ GuessThePatternGame() }, listOf("8", "G", "▲▲▲▲", "5"))
    )

    @Test
    fun everyQuizCardScoresCorrectlyAcrossAllReplayRotations() {
        for (quiz in quizzes) {
            val game = quiz.create()
            repeat(quiz.answers.size) {
                val visited = mutableSetOf<Int>()
                repeat(quiz.answers.size) { count ->
                    val index = JSONObject(game.encode()).getInt(quiz.indexKey)
                    assertTrue(visited.add(index))
                    val view = game.render()
                    val answer = view.buttons.indexOf(quiz.answers[index])
                    assertTrue("${game.id}: correct answer offered", answer >= 0)
                    assertTrue(game.apply(view.buttonActions[answer]))
                    assertEquals(count + 1, game.render().progress!!.current)
                    assertTrue(game.render().feedback.startsWith("Correct"))
                    roundTrip(game, quiz.create)
                }
                assertEquals(GamePhase.WON, game.render().phase)
                assertEquals(quiz.answers.size, game.render().record!!.value)
                val saved = game.encode()
                assertFalse(game.apply(GameAction.PRIMARY))
                assertEquals(saved, game.encode())
                assertTrue(game.apply(GameAction.NEW_GAME))
                assertEquals(0, game.render().progress!!.current)
                assertNull(game.render().record)
            }
        }
    }

    @Test
    fun wrongQuizAnswersExplainAndStillFinishTheDeck() {
        for (quiz in quizzes) {
            val game = quiz.create()
            for (answer in quiz.answers) {
                val view = game.render()
                val wrong = view.buttons.indexOfFirst { it != answer }
                assertTrue(game.apply(view.buttonActions[wrong]))
                assertTrue(game.render().feedback.length > answer.length)
            }
            assertEquals(GamePhase.DRAW, game.render().phase)
            assertEquals(0, game.render().record!!.value)
            assertTrue(game.render().subtitle.contains("complete"))
        }
    }

    @Test
    fun fibonacciAndPhotosynthesisHaveUnambiguousCorrectAnswers() {
        val pattern = GuessThePatternGame()
        repeat(3) { pattern.apply(GameAction.PRIMARY) }
        val state = pattern.render()
        assertTrue(state.subtitle.contains("sum previous two"))
        pattern.apply(state.buttonActions[state.buttons.indexOf("5")])
        assertTrue(pattern.render().feedback.contains("2 + 3 = 5"))
        assertEquals(1, pattern.render().record!!.value)
        val trivia = TriviaBlitzGame()
        repeat(2) { trivia.apply(GameAction.PRIMARY) }
        assertTrue(trivia.render().subtitle.contains("photosynthesis"))
    }

    @Test
    fun legacyQuizCountersAndPendingTriviaQuestionSurviveMigrationTwice() {
        for (quiz in quizzes) {
            val game = quiz.create()
            val old = JSONObject(game.encode()).apply {
                remove("startIndex")
                remove("bounded")
                put("attempts", quiz.answers.size * 2)
                put("score", 2)
                put("streak", 1)
                put("best", 2)
            }
            assertTrue(game.restore(old.toString()))
            assertFalse(game.render().isTerminal)
            assertNull(game.render().record)
            roundTrip(game, quiz.create)
            game.apply(GameAction.NEW_GAME)
            assertEquals(0, game.render().progress!!.current)
        }
        val trivia = TriviaBlitzGame()
        val old = JSONObject(trivia.encode()).apply {
            remove("startIndex")
            remove("bounded")
            put("v", 1)
            put("attempts", 4)
        }
        assertTrue(trivia.restore(old.toString()))
        assertTrue(trivia.render().subtitle.contains("Red Planet"))
        roundTrip(trivia) { TriviaBlitzGame() }
        assertTrue(trivia.apply(GameAction.CELL_1))
        assertTrue(trivia.render().feedback.startsWith("Correct"))
        roundTrip(trivia) { TriviaBlitzGame() }
    }

    @Test
    fun mastermindCountsDuplicatesAcrossAll4096Pairs() {
        val codes = (1..4).flatMap { a -> (1..4).flatMap { b -> (1..4).map { c -> "$a$b$c" } } }
        for (secret in codes) for (guess in codes) {
            val remainingSecret = mutableListOf<Char>()
            val remainingGuess = mutableListOf<Char>()
            var exact = 0
            for (index in 0..2) {
                if (secret[index] == guess[index]) exact++
                else {
                    remainingSecret += secret[index]
                    remainingGuess += guess[index]
                }
            }
            var misplaced = 0
            for (digit in remainingGuess) if (remainingSecret.remove(digit)) misplaced++
            assertEquals("$secret / $guess", exact to misplaced, MastermindMiniGame.match(secret, guess))
        }
    }

    @Test
    fun mastermindKeepsCluesRejectsDuplicateGuessesAndChangesCodeOnReplay() {
        val game = MastermindMiniGame()
        for (secret in listOf("123", "241", "314", "432")) {
            val initial = game.render()
            val wrong = initial.buttons.indices.filter { initial.buttons[it] != secret }
            for (index in wrong) {
                assertTrue(game.apply(initial.buttonActions[index]))
                val state = game.render()
                assertEquals(initial.buttons, state.buttons)
                assertTrue(state.feedback.contains(initial.buttons[index]))
                assertTrue(state.disabledActions.containsKey(initial.buttonActions[index]))
                val saved = game.encode()
                assertFalse(game.apply(initial.buttonActions[index]))
                assertEquals(saved, game.encode())
                roundTrip(game) { MastermindMiniGame() }
            }
            assertTrue(game.apply(initial.buttonActions[initial.buttons.indexOf(secret)]))
            assertEquals(GamePhase.WON, game.render().phase)
            assertEquals(4, game.render().record!!.value)
            assertFalse(game.render().record!!.higherIsBetter)
            assertEquals(3, game.render().progress!!.current)
            roundTrip(game) { MastermindMiniGame() }
            game.apply(GameAction.NEW_GAME)
            assertNotEquals(initial.buttons, game.render().buttons)
        }
    }

    @Test
    fun legacyMastermindCanFinishItsRemainingAttempt() {
        val game = MastermindMiniGame()
        val old = JSONObject(game.encode()).apply {
            remove("guessed")
            put("attempts", 3)
        }
        assertTrue(game.restore(old.toString()))
        assertTrue(game.apply(GameAction.SECONDARY))
        assertEquals(GamePhase.LOST, game.render().phase)
        assertTrue(game.render().feedback.contains("Code was 123"))
        roundTrip(game) { MastermindMiniGame() }
    }

    @Test
    fun nimEverySmallPileAndActionMatchesTheLastStoneRule() {
        for (stones in 1..15) for (take in 1..3) {
            val game = NimGame()
            val saved = JSONObject(game.encode()).put("startingStones", 15).put("stones", stones)
            assertTrue(game.restore(saved.toString()))
            val before = game.encode()
            if (take > stones) {
                assertTrue(game.render().disabledActions.containsKey(choices[take - 1]))
                assertFalse(game.apply(choices[take - 1]))
                assertEquals(before, game.encode())
                continue
            }
            assertTrue(game.apply(choices[take - 1]))
            val left = stones - take
            val reply = when {
                left == 0 -> 0
                left <= 3 -> left
                left % 4 == 0 -> 1
                else -> left % 4
            }
            assertEquals(left - reply, JSONObject(game.encode()).getInt("stones"))
            assertEquals(if (left == 0) GamePhase.WON else if (left == reply) GamePhase.LOST else GamePhase.PLAYING, game.render().phase)
            roundTrip(game) { NimGame() }
        }
    }

    @Test
    fun allNimOpeningsAreWinnableByLeavingMultiplesOfFour() {
        val game = NimGame()
        for (start in 13..15) {
            assertEquals(start, JSONObject(game.encode()).getInt("stones"))
            while (!game.render().isTerminal) {
                val stones = JSONObject(game.encode()).getInt("stones")
                val take = stones % 4
                assertTrue(take in 1..3)
                assertTrue(game.apply(choices[take - 1]))
            }
            assertEquals(GamePhase.WON, game.render().phase)
            assertEquals(1, game.render().record!!.value)
            assertEquals(start, game.render().progress!!.current)
            val saved = game.encode()
            assertFalse(game.apply(GameAction.PRIMARY))
            assertEquals(saved, game.encode())
            assertTrue(game.apply(GameAction.NEW_GAME))
        }
        assertEquals(13, JSONObject(game.encode()).getInt("stones"))
    }

    private fun lineWinner(board: String, mark: Char): Boolean {
        for (row in 0..2) for (column in 0..2) {
            for ((dr, dc) in listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)) {
                val endRow = row + 2 * dr
                val endColumn = column + 2 * dc
                if (endRow in 0..2 && endColumn in 0..2 &&
                    (0..2).all { board[(row + it * dr) * 3 + column + it * dc] == mark }) return true
            }
        }
        return false
    }

    @Test
    fun connectThreeWinnerRulesCoverAll19683Boards() {
        repeat(19683) { value ->
            var number = value
            val board = CharArray(9) {
                val mark = ".XO"[number % 3]
                number /= 3
                mark
            }.concatToString()
            for (mark in "XO") assertEquals(board, lineWinner(board, mark), ConnectThreeGame.hasWinner(board, mark))
        }
        assertFalse(ConnectThreeGame.hasWinner(".........", '.'))
        assertFalse(ConnectThreeGame.hasWinner("XXX", 'X'))
    }

    @Test
    fun everyReachableConnectThreeBranchRespectsGravityAndDisabledColumns() {
        val seen = mutableSetOf<String>()
        fun visit(game: ArcadeGame) {
            val json = JSONObject(game.encode())
            val board = json.getString("board")
            if (!seen.add("${json.getInt("opening")}:$board")) return
            roundTrip(game) { ConnectThreeGame() }
            for (column in 0..2) for (row in 0..1) {
                if (board[row * 3 + column] != '.') assertNotEquals('.', board[(row + 1) * 3 + column])
            }
            val view = game.render()
            assertTrue(view.progress!!.current in 0..3)
            if (view.isTerminal) {
                val phase = if (lineWinner(board, 'X')) GamePhase.WON else if (lineWinner(board, 'O')) GamePhase.LOST else GamePhase.DRAW
                assertEquals(phase, view.phase)
                assertNotNull(view.record)
                val saved = game.encode()
                for (action in cells.take(3)) assertFalse(game.apply(action))
                assertEquals(saved, game.encode())
                return
            }
            assertEquals(board.count { it == 'X' }, board.count { it == 'O' })
            for (column in 0..2) {
                val branch = ConnectThreeGame()
                assertTrue(branch.restore(game.encode()))
                if (board[column] != '.') {
                    assertTrue(view.disabledActions.containsKey(cells[column]))
                    val saved = branch.encode()
                    assertFalse(branch.apply(cells[column]))
                    assertEquals(saved, branch.encode())
                } else {
                    assertFalse(view.disabledActions.containsKey(cells[column]))
                    assertTrue(branch.apply(cells[column]))
                    val next = JSONObject(branch.encode()).getString("board")
                    val row = (2 downTo 0).first { board[it * 3 + column] == '.' }
                    assertEquals('X', next[row * 3 + column])
                    assertEquals(board.count { it == 'X' } + 1, next.count { it == 'X' })
                    visit(branch)
                }
            }
        }
        val game = ConnectThreeGame()
        repeat(3) {
            visit(game)
            game.apply(GameAction.NEW_GAME)
        }
        assertTrue(seen.size > 20)
    }

    private fun press(board: String, cell: Int): String = board.mapIndexed { position, value ->
        if (kotlin.math.abs(position / 3 - cell / 3) + kotlin.math.abs(position % 3 - cell % 3) <= 1) {
            if (value == '0') '1' else '0'
        } else value
    }.joinToString("")

    private fun lightSolutions(): Map<String, List<Int>> {
        val paths = mutableMapOf("000000000" to emptyList<Int>())
        val queue = java.util.ArrayDeque<String>()
        queue.add("000000000")
        while (queue.isNotEmpty()) {
            val board = queue.removeFirst()
            for (cell in 0..8) {
                val next = press(board, cell)
                if (next !in paths) {
                    paths[next] = paths.getValue(board) + cell
                    queue.add(next)
                }
            }
        }
        return paths
    }

    @Test
    fun all512LightBoardsHaveExactDistancesAndCorrectReversiblePresses() {
        val solutions = lightSolutions()
        assertEquals(512, solutions.size)
        for ((board, solution) in solutions) {
            assertEquals(board, solution.size, LightsOutMiniGame.minimumMoves(board))
            if (board == "000000000") continue
            for (cell in 0..8) {
                val game = LightsOutMiniGame()
                val saved = JSONObject(game.encode()).put("board", board)
                assertTrue(game.restore(saved.toString()))
                assertTrue(game.apply(cells[cell]))
                val expected = press(board, cell)
                assertEquals(expected, JSONObject(game.encode()).getString("board"))
                assertEquals(expected == "000000000", game.render().isTerminal)
                assertEquals(expected.count { it == '0' }, game.render().progress!!.current)
                if (cell == 0) roundTrip(game) { LightsOutMiniGame() }
                if (!game.render().isTerminal) {
                    assertTrue(game.apply(cells[cell]))
                    assertEquals(board, JSONObject(game.encode()).getString("board"))
                } else assertFalse(game.apply(cells[cell]))
            }
        }
    }

    @Test
    fun lightReplayOffersSixDistinctComparableParThreePuzzles() {
        val game = LightsOutMiniGame()
        val solutions = lightSolutions()
        val boards = mutableSetOf<String>()
        repeat(6) {
            val board = JSONObject(game.encode()).getString("board")
            assertTrue(boards.add(board))
            val solution = solutions.getValue(board)
            assertEquals(3, solution.size)
            for (cell in solution) assertTrue(game.apply(cells[cell]))
            assertEquals(GamePhase.WON, game.render().phase)
            assertEquals(GameRecord(3, "Moves / par-3 puzzle", false), game.render().record)
            roundTrip(game) { LightsOutMiniGame() }
            assertTrue(game.apply(GameAction.NEW_GAME))
            assertEquals(board, JSONObject(game.encode()).getString("board"))
            assertTrue(game.apply(GameAction.PRIMARY))
        }
        assertEquals(6, JSONObject(game.encode()).getInt("wins"))
        assertEquals("100010001", JSONObject(game.encode()).getString("board"))
    }

    @Test
    fun everyAnagramUsesItsOwnLettersKeepsProgressAndHasABoundedResult() {
        val game = AnagramChallengeGame()
        for (word in listOf("SPARK", "FRAME", "LIGHT", "OCEAN", "BRISK")) {
            assertTrue(game.render().subtitle.contains(" · "))
            for (position in word.indices) {
                var state = game.render()
                val unused = word.drop(position).map { it.toString() }
                assertTrue(state.buttons.all { it in unused })
                if (position == 1) {
                    val wrong = state.buttons.indexOfFirst { it != word[position].toString() }
                    assertTrue(game.apply(state.buttonActions[wrong]))
                    assertEquals(position, game.render().progress!!.current)
                    assertTrue(game.render().feedback.contains("kept"))
                    state = game.render()
                }
                if (position == 4) {
                    assertEquals(1, state.buttons.size)
                    val before = game.encode()
                    assertFalse(game.apply(GameAction.SECONDARY))
                    assertEquals(before, game.encode())
                }
                val answer = state.buttons.indexOf(word[position].toString())
                assertTrue(answer >= 0)
                assertTrue(game.apply(state.buttonActions[answer]))
                roundTrip(game) { AnagramChallengeGame() }
            }
            assertEquals(GamePhase.WON, game.render().phase)
            assertEquals(1, game.render().record!!.value)
            assertFalse(game.render().record!!.higherIsBetter)
            assertEquals(5, game.render().progress!!.current)
            assertTrue(game.render().feedback.contains(word))
            val before = game.encode()
            assertFalse(game.apply(GameAction.PRIMARY))
            assertEquals(before, game.encode())
            assertTrue(game.apply(GameAction.NEW_GAME))
        }
    }

    @Test
    fun anagramFailureRevealsCluedAnswerAndLegacyPrefixStillRestores() {
        val game = AnagramChallengeGame()
        val wrong = game.render().buttonActions[game.render().buttons.indexOfFirst { it != "S" }]
        repeat(3) { assertTrue(game.apply(wrong)) }
        assertEquals(GamePhase.LOST, game.render().phase)
        assertTrue(game.render().feedback.contains("SPARK"))
        assertTrue(game.render().feedback.contains("fire"))
        assertNull(game.render().record)
        roundTrip(game) { AnagramChallengeGame() }
        val legacy = JSONObject(AnagramChallengeGame().encode()).apply {
            remove("bounded")
            remove("mistakes")
            put("progress", "SP")
            put("attempts", 2)
        }
        assertTrue(game.restore(legacy.toString()))
        assertEquals(2, game.render().progress!!.current)
        roundTrip(game) { AnagramChallengeGame() }
    }

    @Test
    fun invalidAddedStateFieldsAreRejectedAtomically() {
        val fixtures = listOf<Triple<() -> ArcadeGame, String, Any>>(
            Triple({ RockPaperScissorsGame(Random(1)) }, "movePlan", "99999"),
            Triple({ OddOneOutGame() }, "startIndex", 99),
            Triple({ EmojiDecodeGame() }, "startIndex", -1),
            Triple({ TriviaBlitzGame() }, "startIndex", 8),
            Triple({ MastermindMiniGame() }, "guessed", 16),
            Triple({ NimGame() }, "startingStones", 12),
            Triple({ ConnectThreeGame() }, "opening", 3),
            Triple({ GuessThePatternGame() }, "startIndex", 4),
            Triple({ AnagramChallengeGame() }, "mistakes", 4),
            Triple({ LightsOutMiniGame() }, "puzzleIndex", 6)
        )
        for ((create, field, value) in fixtures) {
            val game = create()
            val saved = game.encode()
            val broken = JSONObject(saved).put(field, value).toString()
            assertFalse("${game.id}: invalid $field", game.restore(broken))
            assertEquals(saved, game.encode())
            assertFalse(game.restore("{}"))
            assertEquals(saved, game.encode())
            val state = game.render()
            assertTrue(state.buttons.size <= 4)
            assertEquals(state.buttons.size, state.buttonActions.size)
            assertTrue(state.cellButtons.size <= 9)
            assertEquals(state.cellButtons.size, state.cellActions.size)
            val progress = state.progress!!
            assertTrue(progress.current in 0..progress.maximum)
            assertTrue(state.disabledActions.keys.all { it in state.buttonActions + state.cellActions })
            assertNull(state.record)
        }
    }
}
