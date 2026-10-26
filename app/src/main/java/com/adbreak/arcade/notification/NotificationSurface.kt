package com.adbreak.arcade.notification

import com.adbreak.arcade.core.GamePhase
import com.adbreak.arcade.core.GameAction
import com.adbreak.arcade.core.GameViewState

/** Layout decisions without Android dependencies, so every registered state can be checked. */
object NotificationSurface {
    fun usesBoardActions(state: GameViewState): Boolean =
        state.cellButtons.size > 3 && state.buttons.size <= 2

    /** Preserve final boards and distinct Retry/New Puzzle controls supplied by the model. */
    fun terminalState(state: GameViewState): GameViewState =
        if (GameAction.NEW_GAME in state.buttonActions) state else state.copy(
            buttons = state.buttons.take(3) + "Replay",
            buttonActions = state.buttonActions.take(3) + GameAction.NEW_GAME,
        )

    fun visibleCells(state: GameViewState, largeText: Boolean, page: Int): IntRange {
        if (state.cellButtons.isEmpty()) return IntRange.EMPTY
        if (!largeText || !usesBoardActions(state)) return state.cellButtons.indices
        val start = (page.coerceIn(0, (state.cellButtons.size - 1) / 3) * 3)
        return start..minOf(start + 2, state.cellButtons.lastIndex)
    }

    fun phaseLabel(state: GameViewState, paused: Boolean): String = when {
        paused -> "Paused"
        state.phase == GamePhase.WON -> "Complete"
        state.phase == GamePhase.LOST -> "Try again"
        state.phase == GamePhase.DRAW -> "Draw"
        state.isTerminal -> "Run ended"
        else -> "Playing"
    }

    fun compactSummary(state: GameViewState, paused: Boolean, notice: String): String = when {
        paused -> "Paused · progress saved"
        notice.isNotBlank() -> "$notice · ${state.subtitle}"
        state.isTerminal -> state.subtitle.ifBlank { state.feedback }
        else -> state.subtitle.ifBlank { state.feedback }
    }
}
