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
        "cyber_heist" to GamePresentation(code, "⌁"),
        "pocket_dungeon" to GamePresentation(GameFamily.FOCUS, "♜"),
        "neon_tactics" to GamePresentation(board, "✣"),
        "cipher_vault" to GamePresentation(code, "◇"),
        "prism_protocol" to GamePresentation(GameFamily.FOCUS, "◈"),
        "starship_survival" to GamePresentation(GameFamily.PULSE, "✦"),
        "micro_tower_command" to GamePresentation(board, "⌂"),
        "pulse_reactor" to GamePresentation(pulse, "◉"),
    )

    fun forGame(id: String): GamePresentation = presentations[id] ?: error("Missing presentation for game id: $id")

    fun has(id: String): Boolean = id in presentations
}
