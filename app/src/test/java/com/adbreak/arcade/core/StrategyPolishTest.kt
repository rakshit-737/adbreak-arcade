package com.adbreak.arcade.core

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class StrategyPolishTest {
    private val flightActions = listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2)

    private fun pulseAction(pulse: Int) = flightActions[pulse - 1]

    private fun pattern(game: PulseReactorGame): List<Int> {
        val values = JSONObject(game.encode()).getJSONArray("sequence")
        return List(values.length()) { values.getInt(it) }
    }

    @Test
    fun everyPrismConfigurationHasBoundedUniqueBeamStatesAndKnownMinimumSolution() {
        PrismProtocolGame.levels.forEachIndexed { index, level ->
            var minimum = Int.MAX_VALUE
            for (mask in 0 until (1 shl level.mirrors.size)) {
                val orientations = level.mirrors.mapIndexed { mirror, value ->
                    if (mask and (1 shl mirror) == 0) value.orientation else value.orientation.rotatedQuarterTurn()
                }
                val beam = PrismProtocolGame.propagate(level, orientations)
                assertNotEquals(PrismProtocolGame.BeamStatus.INVALID, beam.status)
                assertEquals(beam.states.size, beam.states.toSet().size)
                assertTrue(beam.states.size <= level.width * level.height * 4)
                assertTrue(beam.path.all { it.row in 0 until level.height && it.column in 0 until level.width })
                if (beam.reachedTarget) minimum = minOf(minimum, Integer.bitCount(mask))
            }
            assertEquals("${level.name} minimum", level.solution.size, minimum)
            if (index >= 3) assertTrue(minimum >= 4)
        }
    }

    @Test
    fun allPrismLevelsWinThroughPublicControlsAndRestoreAtEveryStep() {
        PrismProtocolGame.levels.forEachIndexed { index, level ->
            val game = PrismProtocolGame(index)
            assertNull(game.render().record)
            assertTrue(GameAction.PRIMARY in game.render().disabledActions)
            level.solution.forEachIndexed { step, mirror ->
                assertTrue(game.apply(GameAction.values()[GameAction.CELL_1.ordinal + mirror]))
                val restored = PrismProtocolGame()
                assertTrue(restored.restore(game.encode()))
                assertEquals(game.render(), restored.render())
                val rotate = if (step % 2 == 0) GameAction.PRIMARY else GameAction.SECONDARY
                assertTrue(game.apply(rotate))
                assertTrue(restored.apply(rotate))
                assertEquals(game.encode(), restored.encode())
            }
            assertEquals(GamePhase.WON, game.render().phase)
            assertEquals(GameProgress(1, 1, "Target linked"), game.render().progress)
            assertEquals(GameRecord(1, "Protocols solved"), game.render().record)
            val completed = game.encode()
            assertFalse(game.apply(GameAction.PRIMARY))
            assertEquals(completed, game.encode())
            assertTrue(game.apply(GameAction.NEW_GAME))
            val afterReplay = PrismProtocolGame()
            assertTrue(afterReplay.restore(game.encode()))
            assertEquals(game.render(), afterReplay.render())
        }
    }

    @Test
    fun prismRejectsOffBoardMirrorsAndExhaustedFlipsOfferRetry() {
        val invalid = PrismProtocolGame.levels.first().copy(
            mirrors = listOf(PrismProtocolGame.Mirror(PrismProtocolGame.Cell(99, 1), PrismProtocolGame.MirrorOrientation.SLASH)),
        )
        assertFalse(PrismProtocolGame.validateLevel(invalid))
        assertEquals(PrismProtocolGame.BeamStatus.INVALID, PrismProtocolGame.propagate(invalid).status)
        val game = PrismProtocolGame(4)
        repeat(game.level.moveLimit) {
            assertTrue(game.apply(GameAction.CELL_6))
            assertTrue(game.apply(GameAction.PRIMARY))
        }
        assertEquals(GamePhase.LOST, game.render().phase)
        assertNull(game.render().record)
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertEquals(4, game.levelIndex)
        assertEquals(0, game.moves)
    }

    @Test
    fun starshipRepeatedSafeButtonsNowLoseWhileHazardAwareRoutePreservesHull() {
        listOf(GameAction.PRIMARY, GameAction.CELL_1, GameAction.CELL_2).forEach { action ->
            val game = StarshipSurvivalGame()
            while (!game.render().isTerminal) assertTrue(game.apply(action))
            assertEquals("Repeated $action", GamePhase.LOST, game.render().phase)
            assertNull(game.render().record)
        }
        val game = StarshipSurvivalGame()
        repeat(6) { step ->
            val restored = StarshipSurvivalGame()
            assertTrue(restored.restore(game.encode()))
            val action = if (step % 2 == 0) GameAction.CELL_2 else GameAction.CELL_1
            assertTrue(game.apply(action))
            assertTrue(restored.apply(action))
            assertEquals(game.render(), restored.render())
        }
        assertEquals(GamePhase.WON, game.render().phase)
        assertEquals(12, game.hull)
        assertEquals(45, game.render().record!!.value)
        assertEquals(GameProgress(6, 6, "Sectors crossed"), game.render().progress)
    }

    @Test
    fun exhaustiveFlightChoicesHaveFiniteScoringNoDeadControlsAndMultipleWinningRoutes() {
        var wins = 0
        var losses = 0
        var maximum = 0
        var maximumWin = 0
        var bestRoute = emptyList<GameAction>()
        fun visit(game: StarshipSurvivalGame, route: List<GameAction> = emptyList()) {
            val state = game.render()
            if (state.isTerminal) {
                if (state.phase == GamePhase.WON) wins++ else losses++
                if (state.phase == GamePhase.WON && game.score > maximumWin) {
                    maximumWin = game.score
                    bestRoute = route
                }
                maximum = maxOf(maximum, game.score)
                assertTrue(game.eventIndex in 1..6)
                assertTrue(game.score <= game.eventIndex * 10)
                val before = game.encode()
                flightActions.forEach { assertFalse(game.apply(it)) }
                assertEquals(before, game.encode())
                return
            }
            var accepted = 0
            flightActions.forEach { action ->
                val next = StarshipSurvivalGame()
                assertTrue(next.restore(game.encode()))
                val before = next.encode()
                if (next.apply(action)) {
                    accepted++
                    assertFalse(action in state.disabledActions)
                    val risk = state.buttons[state.buttonActions.indexOf(action)].substringBefore('\n').substringAfter(' ')
                    if (risk == "BREACH") assertEquals(0, next.hull)
                    else assertEquals("Visible hull forecast for $action", risk.removeSuffix("HP").toInt(), next.hull - game.hull)
                    assertEquals(game.eventIndex + 1, next.eventIndex)
                    visit(next, route + action)
                } else {
                    assertTrue(action in state.disabledActions)
                    assertEquals(before, next.encode())
                }
            }
            assertTrue("No live dead-end", accepted > 0)
        }
        visit(StarshipSurvivalGame())
        assertTrue(wins > 10)
        assertTrue(losses > 10)
        assertTrue(maximum in 45..60)
        println("Flight exploration: $wins wins, $losses losses, maximum score $maximum; best escape $maximumWin via $bestRoute")
    }

    @Test
    fun shipRepairCannotFarmFullHealthAndForgedScoreIsRejectedAtomically() {
        val game = StarshipSurvivalGame()
        val before = game.encode()
        assertTrue(GameAction.SECONDARY in game.render().disabledActions)
        assertFalse(game.apply(GameAction.SECONDARY))
        assertEquals(before, game.encode())
        assertFalse(game.restore(JSONObject(before).put("score", 999999).toString()))
        assertEquals(before, game.encode())
    }

    private fun finishDefence(game: MicroTowerCommandGame) {
        repeat(40) {
            if (!game.render().isTerminal) assertTrue(game.apply(GameAction.SECONDARY))
        }
        assertTrue("Defence should finish in 40 turns", game.render().isTerminal)
    }

    @Test
    fun towerNeedsLayeredDefenceAndAllWavesCanBeWonWithoutLeaking() {
        val empty = MicroTowerCommandGame()
        finishDefence(empty)
        assertEquals(GamePhase.LOST, empty.render().phase)
        assertEquals(0, empty.health)
        assertNull(empty.render().record)
        MicroTowerCommandGame.BUILD_CELLS.forEach { cell ->
            val single = MicroTowerCommandGame()
            assertTrue(single.apply(GameAction.values()[GameAction.CELL_1.ordinal + cell]))
            finishDefence(single)
            assertEquals("Single unupgraded dart at ${cell + 1}", GamePhase.LOST, single.render().phase)
        }
        val layered = MicroTowerCommandGame()
        listOf(GameAction.CELL_2, GameAction.CELL_4, GameAction.CELL_6).forEach { assertTrue(layered.apply(it)) }
        while (!layered.render().isTerminal) {
            val restored = MicroTowerCommandGame()
            assertTrue(restored.restore(layered.encode()))
            assertEquals(layered.render(), restored.render())
            assertTrue(layered.apply(GameAction.SECONDARY))
            assertTrue(restored.apply(GameAction.SECONDARY))
            assertEquals(layered.encode(), restored.encode())
        }
        assertEquals(GamePhase.WON, layered.render().phase)
        assertEquals(6, layered.health)
        assertEquals(GameProgress(3, 3, "Waves cleared"), layered.render().progress)
        assertEquals(GameRecord(6, "Base saved"), layered.render().record)
        println("Tower: no-build loss ${empty.turnCount} turns; three-dart perfect win ${layered.turnCount} turns")
        assertTrue(layered.apply(GameAction.NEW_GAME))
        assertEquals(12, layered.resources)
        assertNull(layered.render().record)
    }

    @Test
    fun towerRangePreviewMatchesActualFirstTurnFireAndUpgradeCosts() {
        val distant = MicroTowerCommandGame()
        assertTrue(distant.render().cellButtons[7].contains("[7–9]"))
        assertTrue(GameAction.CELL_1 in distant.render().disabledActions)
        assertTrue(distant.apply(GameAction.CELL_8))
        assertTrue(distant.apply(GameAction.SECONDARY))
        assertEquals(8, distant.resources) // Starting grunt is out of range, no reward.
        assertEquals(1, JSONObject(distant.encode()).getJSONArray("enemies").length())

        val near = MicroTowerCommandGame()
        assertTrue(near.apply(GameAction.CELL_2))
        assertTrue(near.render().cellButtons[1].contains("↑3g [1–3]"))
        assertTrue(near.apply(GameAction.SECONDARY))
        assertEquals(10, near.resources)
        assertEquals(0, JSONObject(near.encode()).getJSONArray("enemies").length())
        assertTrue(near.apply(GameAction.CELL_2))
        assertTrue(near.render().cellButtons[1].contains("↑5g [1–5]"))
        assertTrue(near.apply(GameAction.CELL_2))
        assertTrue(near.render().cellButtons[1].contains("MAX [1–7]"))
        assertTrue(GameAction.CELL_2 in near.render().disabledActions)
        assertTrue(GameAction.CELL_4 in near.render().disabledActions)
        val gold = near.resources
        val before = near.encode()
        assertFalse(near.apply(GameAction.CELL_2))
        assertEquals(gold, near.resources)
        assertEquals(before, near.encode())
    }

    @Test fun towerTurnGuardCanRestoreItsOwnTerminalSnapshot() {
        val game = MicroTowerCommandGame()
        assertTrue(game.restore(JSONObject(game.encode()).put("turns", 10000).toString()))
        assertTrue(game.apply(GameAction.SECONDARY))
        assertEquals(GamePhase.LOST, game.render().phase)
        val restored = MicroTowerCommandGame()
        assertTrue(restored.restore(game.encode()))
        assertEquals(game.render(), restored.render())
    }

    @Test
    fun frostAndUpgradedDartAreASecondViableDefence() {
        val game = MicroTowerCommandGame()
        assertTrue(game.apply(GameAction.CELL_2))
        assertTrue(game.apply(GameAction.CELL_2))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.render().cellButtons[3].contains("5g [1–7]"))
        assertTrue(game.apply(GameAction.CELL_4))
        finishDefence(game)
        assertEquals(GamePhase.WON, game.render().phase)
        assertEquals(6, game.health)
        println("Tower: upgraded dart + frost perfect win ${game.turnCount} turns")
    }

    @Test
    fun pulseMistakeRevealsPatternAndRestoreKeepsRecoveryOpportunity() {
        val game = PulseReactorGame(Random(7))
        val sequence = pattern(game)
        assertTrue(game.apply(pulseAction(sequence[0])))
        assertFalse(game.render().subtitle.contains("Memorize"))
        assertTrue(game.apply(pulseAction(sequence[1] % 4 + 1)))
        assertTrue(game.render().subtitle.contains("Memorize ${sequence.joinToString(" ")}"))
        assertEquals(0, game.streak)
        assertEquals(1, game.mistakes)
        val restored = PulseReactorGame(Random(999))
        assertTrue(restored.restore(game.encode()))
        sequence.forEach {
            assertTrue(game.apply(pulseAction(it)))
            assertTrue(restored.apply(pulseAction(it)))
        }
        assertEquals(2, game.level)
        assertEquals(game.encode(), restored.encode())
    }

    @Test
    fun pulseGenerationAndRestoreStayEquivalentThroughAllTenDifficultyLevels() {
        val firstPatterns = mutableSetOf<List<Int>>()
        repeat(32) { seed ->
            val game = PulseReactorGame(Random(seed.toLong()))
            firstPatterns += pattern(game)
            var taps = 0
            repeat(10) { level ->
                val sequence = pattern(game)
                assertEquals(level + 3, sequence.size)
                assertTrue(sequence.all { it in 1..4 })
                assertTrue(game.apply(pulseAction(sequence.first())))
                taps++
                val restored = PulseReactorGame(Random(999))
                assertTrue(restored.restore(game.encode()))
                sequence.drop(1).forEach {
                    assertTrue(game.apply(pulseAction(it)))
                    assertTrue(restored.apply(pulseAction(it)))
                    taps++
                    assertEquals(game.encode(), restored.encode())
                }
            }
            assertEquals(75, taps)
            assertEquals(GamePhase.WON, game.render().phase)
            assertEquals(1446, game.score)
            assertEquals(GameProgress(10, 10, "Patterns completed"), game.render().progress)
            assertEquals(GameRecord(10, "Best streak"), game.render().record)
            val won = game.encode()
            assertFalse(game.apply(GameAction.PRIMARY))
            assertEquals(won, game.encode())
            assertTrue(game.apply(GameAction.NEW_GAME))
            assertEquals(0, game.score)
            assertEquals(10, game.best)
        }
        assertTrue("Seeded runs should vary", firstPatterns.size > 15)
    }

    @Test
    fun pulseRetriesNeverAwardExtraPointsAndThreeMistakesLose() {
        val game = PulseReactorGame(Random(11))
        repeat(10) {
            val sequence = pattern(game)
            val before = game.score
            repeat(2) { assertTrue(game.apply(pulseAction(sequence.first() % 4 + 1))) }
            assertEquals(before, game.score)
            sequence.forEach { assertTrue(game.apply(pulseAction(it))) }
        }
        assertEquals(GamePhase.WON, game.render().phase)
        assertEquals(495, game.score)
        assertEquals(1, game.best)
        assertTrue(game.apply(GameAction.NEW_GAME))
        val wrong = pulseAction(pattern(game).first() % 4 + 1)
        repeat(3) { assertTrue(game.apply(wrong)) }
        assertEquals(GamePhase.LOST, game.render().phase)
        assertEquals(0, game.score)
        assertEquals(0, game.render().progress!!.current)
    }

    @Test
    fun pulseMigratesLegacyHiddenMistakesAndRejectsUnearnedScores() {
        val game = PulseReactorGame(Random(12))
        assertTrue(game.apply(pulseAction(pattern(game).first() % 4 + 1)))
        val legacy = JSONObject(game.encode()).put("v", 1).put("patternVisible", false)
        legacy.remove("runSeed")
        val restored = PulseReactorGame(Random(99))
        assertTrue(restored.restore(legacy.toString()))
        assertTrue(restored.render().subtitle.contains("Memorize"))
        assertEquals(2, JSONObject(restored.encode()).getInt("v"))
        val before = restored.encode()
        assertFalse(restored.restore(JSONObject(before).put("score", 1000000000).toString()))
        assertFalse(restored.restore(JSONObject(before).put("streak", 999).put("best", 999).toString()))
        assertEquals(before, restored.encode())
    }
}
