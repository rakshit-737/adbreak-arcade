package com.adbreak.arcade.notification

import com.adbreak.arcade.core.GameAction
import com.adbreak.arcade.core.GamePhase
import com.adbreak.arcade.core.GameRecord
import com.adbreak.arcade.core.GameRegistry
import com.adbreak.arcade.core.GameViewState
import com.adbreak.arcade.data.PersonalRecord
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class NotificationPolishTest {
    @Test fun envelopesRejectEveryPartialAndStaleCombination() {
        val current = ActionEnvelope("cyber_heist", 42, 7)
        for (game in listOf(null, "pocket_dungeon", "cyber_heist")) {
            for (session in listOf(null, 41L, 42L, 43L)) {
                for (revision in listOf(null, 6L, 7L, 8L)) {
                    assertEquals(game == "cyber_heist" && session == 42L && revision == 7L, current.matches(game, session, revision))
                }
            }
        }
        assertFalse(ActionEnvelope("cyber_heist", 0, 0).matches("cyber_heist", 0, 0))
        assertNotEquals(current.token, current.copy(revision = 8).token)
        assertNotEquals(current.token, current.copy(sessionId = 43).token)
    }

    @Test fun recordComparisonNeverAwardsTiesOrChangesUnits() {
        val best = PersonalRecord(12, "Moves", false)
        assertTrue(best.beatenBy(GameRecord(11, "Moves", false)))
        assertFalse(best.beatenBy(GameRecord(12, "Moves", false)))
        assertFalse(best.beatenBy(GameRecord(13, "Moves", false)))
        assertFalse(best.beatenBy(GameRecord(-1, "Moves", false)))
        assertFalse(best.beatenBy(GameRecord(9, "Score", false)))
        assertFalse(best.beatenBy(GameRecord(20, "Moves", true)))
        val high = PersonalRecord(100, "Score", true)
        assertTrue(high.beatenBy(GameRecord(101, "Score")))
        assertFalse(high.beatenBy(GameRecord(100, "Score")))
        assertEquals(best, PersonalRecord.decode(best.encode()))
        assertNull(PersonalRecord.decode("{}"))
        assertNull(PersonalRecord.decode(best.encode().replace("\"v\":1", "\"v\":99")))
    }

    @Test fun largeTextPagesPreserveAllNineCellIdentities() {
        val state = GameViewState("Board", "Reach the exit", "HP 3", cellButtons = (1..9).map { "$it:tile" },
            cellActions = GameAction.values().filter { it.name.startsWith("CELL_") })
        assertTrue(NotificationSurface.usesBoardActions(state))
        assertEquals((0..8).toList(), NotificationSurface.visibleCells(state, false, 2).toList())
        val pages = (0..2).map { NotificationSurface.visibleCells(state, true, it).toList() }
        assertTrue(pages.all { it.size == 3 })
        assertEquals((0..8).toList(), pages.flatten())
        assertEquals(listOf(6, 7, 8), NotificationSurface.visibleCells(state, true, 99).toList())
        assertTrue(NotificationSurface.usesBoardActions(state.copy(isTerminal = true)))
    }

    @Test fun compactSurfaceKeepsObjectiveAndTerminalOutcomeInsteadOfGenericHints() {
        val state = GameViewState("Ship", "Ion storm · shield cost 2", "Hull 6", feedback = "Repaired 3 hull")
        assertEquals(state.subtitle, NotificationSurface.compactSummary(state, false, ""))
        assertTrue(NotificationSurface.compactSummary(state, false, "Saved session restored").contains("restored"))
        assertEquals("Paused · progress saved", NotificationSurface.compactSummary(state, true, ""))
        assertEquals("Complete", NotificationSurface.phaseLabel(state.copy(isTerminal = true, phase = GamePhase.WON), false))
        assertEquals("Try again", NotificationSurface.phaseLabel(state.copy(isTerminal = true, phase = GamePhase.LOST), false))
        assertEquals(state.subtitle, NotificationSurface.compactSummary(state.copy(isTerminal = true), false, ""))
    }

    @Test fun terminalPresentationRetainsFinalBoardAndAlternatePuzzleControls() {
        val finalBoard = GameViewState("Board", "Draw", "Score 1", buttons = listOf("Replay"),
            buttonActions = listOf(GameAction.NEW_GAME), cellButtons = List(9) { "X" },
            cellActions = GameAction.values().filter { it.name.startsWith("CELL_") }, isTerminal = true, phase = GamePhase.DRAW)
        assertEquals(finalBoard, NotificationSurface.terminalState(finalBoard))
        val puzzle = finalBoard.copy(buttons = listOf("Retry", "New puzzle"), buttonActions = listOf(GameAction.NEW_GAME, GameAction.PRIMARY))
        assertEquals(puzzle, NotificationSurface.terminalState(puzzle))
        assertEquals(listOf(GameAction.NEW_GAME), NotificationSurface.terminalState(finalBoard.copy(buttons = emptyList(), buttonActions = emptyList())).buttonActions)
    }

    @Test fun everyGameSurvivesLegalWalksWithRestorableNotificationContracts() {
        val random = Random(831)
        GameRegistry.definitions.forEach { game ->
            repeat(80) {
                val state = game.render()
                assertEquals("${game.id} top labels", state.buttons.size, state.buttonActions.size)
                assertEquals("${game.id} cell labels", state.cellButtons.size, state.cellActions.size)
                assertTrue("${game.id} control count", state.buttons.size <= 4 && state.cellButtons.size <= 9)
                if (state.cellButtons.size > 3 && !state.isTerminal) {
                    assertTrue("${game.id} board needs room for native actions", state.buttons.size <= 2)
                }
                state.progress?.let { progress ->
                    assertTrue("${game.id} progress $progress", progress.maximum > 0 && progress.current in 0..progress.maximum && progress.label.isNotBlank())
                }
                state.record?.let { assertTrue("${game.id} record", it.value >= 0 && it.label.isNotBlank()) }
                val restored = GameRegistry.create(game.id)
                assertTrue("${game.id} restore", restored.restore(game.encode()))
                assertEquals("${game.id} refresh changes presentation", state, restored.render())
                state.disabledActions.forEach { (action, reason) ->
                    assertTrue("${game.id} disabled reason", reason.isNotBlank())
                    val before = restored.encode()
                    assertFalse("${game.id} disabled $action was accepted", restored.apply(action))
                    assertEquals("${game.id} rejected action mutated state", before, restored.encode())
                }
                val offered = (state.buttonActions + state.cellActions).filterNot { it in state.disabledActions }
                // Some older board models still expose occupied cells; only accepted transitions advance the walk.
                val accepted = offered.filter { action ->
                    val probe = GameRegistry.create(game.id)
                    assertTrue(probe.restore(game.encode()))
                    probe.apply(action)
                }
                assertTrue("${game.id} has no usable notification action", accepted.isNotEmpty())
                assertTrue(game.apply(accepted[random.nextInt(accepted.size)]))
            }
        }
    }
}
