package com.adbreak.arcade.ui.game

import androidx.annotation.ColorRes
import com.adbreak.arcade.R

enum class GameFamily(val label: String, @get:ColorRes val accent: Int) {
    PULSE("Pulse", R.color.arcade_family_pulse),
    LOGIC("Logic", R.color.arcade_family_logic),
    WORDS("Words", R.color.arcade_family_words),
    BOARD("Board", R.color.arcade_family_board),
    CODE("Code", R.color.arcade_family_code),
    FOCUS("Focus", R.color.arcade_family_focus),
}

data class GamePresentation(
    val family: GameFamily,
    val motif: String,
)

object GamePresentationCatalog {
    private val pulse = GameFamily.PULSE
    private val logic = GameFamily.LOGIC
    private val words = GameFamily.WORDS
    private val board = GameFamily.BOARD
    private val code = GameFamily.CODE
    private val focus = GameFamily.FOCUS

    private val presentations = mapOf(
        "tap_counter" to GamePresentation(pulse, "＋"),
        "quick_math" to GamePresentation(logic, "∑"),
        "tic_tac_toe" to GamePresentation(board, "▦"),
        "higher_lower" to GamePresentation(logic, "↕"),
        "memory_challenge" to GamePresentation(focus, "•••"),
        "word_scramble" to GamePresentation(words, "Aa"),
        "number_sequence" to GamePresentation(logic, "↗"),
        "rock_paper_scissors" to GamePresentation(pulse, "✦"),
        "odd_one_out" to GamePresentation(logic, "◉"),
        "emoji_decode" to GamePresentation(words, "☺"),
        "trivia_blitz" to GamePresentation(focus, "?"),
        "mastermind_mini" to GamePresentation(code, "⌘"),
        "nim" to GamePresentation(code, "••"),
        "connect_three" to GamePresentation(board, "≡"),
        "guess_the_pattern" to GamePresentation(logic, "⌁"),
        "anagram_challenge" to GamePresentation(words, "ABC"),
        "lights_out_mini" to GamePresentation(board, "◌"),
    )

    fun forGame(id: String): GamePresentation = presentations[id] ?: GamePresentation(logic, "•")
}
