package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrismProtocolGameTest {
    @Test
    fun handcraftedLevelsAreValidAndTheirExactSolutionsHitTheTarget() {
        PrismProtocolGame.levels.forEach { level ->
            assertTrue("${level.name} should validate", PrismProtocolGame.validateLevel(level))
            var orientations = level.mirrors.map { it.orientation }
            assertNotEquals(PrismProtocolGame.BeamStatus.TARGET, PrismProtocolGame.propagate(level, orientations).status)
            level.solution.forEach { mirrorIndex ->
                orientations = orientations.toMutableList().also {
                    it[mirrorIndex] = it[mirrorIndex].rotatedQuarterTurn()
                }
            }
            assertEquals(
                PrismProtocolGame.BeamStatus.TARGET,
                PrismProtocolGame.propagate(level, orientations).status,
            )
        }
    }

    @Test
    fun propagationReflectsBlocksAndPreventsLoops() {
        val blockerLevel = PrismProtocolGame.Level(
            name = "Block test",
            width = 5,
            height = 5,
            source = PrismProtocolGame.Cell(2, 0),
            sourceDirection = PrismProtocolGame.Direction.RIGHT,
            target = PrismProtocolGame.Cell(0, 4),
            mirrors = emptyList(),
            blockers = setOf(PrismProtocolGame.Cell(2, 2)),
        )
        assertEquals(PrismProtocolGame.BeamStatus.BLOCKED, PrismProtocolGame.propagate(blockerLevel).status)

        val loopLevel = PrismProtocolGame.levels[2]
        val loop = PrismProtocolGame.propagate(loopLevel)
        assertEquals(PrismProtocolGame.BeamStatus.LOOP, loop.status)
        assertTrue(loop.path.isNotEmpty())
    }

    @Test
    fun stagedRotationRequiresSelectionAndSupportsSolveFailAndReplay() {
        val game = PrismProtocolGame()
        assertFalse(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.CELL_1))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertEquals(GamePhase.WON, game.render().phase)
        assertTrue(game.render().isTerminal)
        assertFalse(game.apply(GameAction.CELL_1))

        assertTrue(game.apply(GameAction.NEW_GAME))
        assertFalse(game.render().isTerminal)
        assertEquals(1, game.wins)
        assertTrue(game.apply(GameAction.CELL_1))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertEquals(GamePhase.PLAYING, game.render().phase)
        assertTrue(game.apply(GameAction.CELL_2))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertEquals(GamePhase.WON, game.render().phase)
    }

    @Test
    fun moveLimitProducesLostStateAndReplayCanRetryIt() {
        val game = PrismProtocolGame(1)
        repeat(game.level.moveLimit) {
            assertTrue(game.apply(GameAction.CELL_3))
            assertTrue(game.apply(GameAction.PRIMARY))
        }
        assertEquals(GamePhase.LOST, game.render().phase)
        assertFalse(game.apply(GameAction.CELL_1))
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertEquals(GamePhase.PLAYING, game.render().phase)
        assertEquals(0, game.moves)
    }

    @Test
    fun persistenceRoundTripsSelectionAndRejectsInvalidStateWithoutMutation() {
        val source = PrismProtocolGame()
        assertTrue(source.apply(GameAction.CELL_2))
        val encoded = source.encode()
        val restored = PrismProtocolGame(2)
        assertTrue(restored.restore(encoded))
        assertEquals(source.render(), restored.render())
        assertEquals(encoded, restored.encode())

        val before = source.render()
        val malformed = JSONObject(encoded).put("rotations", JSONArray(listOf(9, 0, 0)))
        assertFalse(source.restore(malformed.toString()))
        assertEquals(before, source.render())
        assertFalse(source.restore("{}"))
    }

    @Test
    fun invalidLevelsAreRejectedByValidationAndPropagation() {
        val overlap = PrismProtocolGame.Level(
            name = "invalid",
            width = 5,
            height = 5,
            source = PrismProtocolGame.Cell(0, 0),
            sourceDirection = PrismProtocolGame.Direction.RIGHT,
            target = PrismProtocolGame.Cell(0, 4),
            mirrors = listOf(PrismProtocolGame.Mirror(PrismProtocolGame.Cell(0, 0), PrismProtocolGame.MirrorOrientation.SLASH)),
            solution = listOf(0),
        )
        assertFalse(PrismProtocolGame.validateLevel(overlap))
        assertEquals(PrismProtocolGame.BeamStatus.INVALID, PrismProtocolGame.propagate(overlap).status)
    }
}
