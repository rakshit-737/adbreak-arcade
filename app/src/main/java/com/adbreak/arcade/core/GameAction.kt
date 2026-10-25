package com.adbreak.arcade.core

enum class GameAction {
    PRIMARY, SECONDARY, RESET, PAUSE, RESUME, NEW_GAME, CELL_1, CELL_2, CELL_3,
    CELL_4, CELL_5, CELL_6, CELL_7, CELL_8, CELL_9
}

data class GameViewState(
    val title: String,
    val subtitle: String,
    val scoreLine: String,
    val buttons: List<String> = emptyList(),
    val buttonActions: List<GameAction> = emptyList(),
    val cellButtons: List<String> = emptyList(),
    val cellActions: List<GameAction> = emptyList(),
    val feedback: String = "",
    val isTerminal: Boolean = false,
)

interface ArcadeGame {
    val id: String
    val title: String
    val description: String
    fun reset()
    fun apply(action: GameAction): Boolean
    fun render(): GameViewState
    fun encode(): String
    fun restore(encoded: String): Boolean
}
