package com.adbreak.arcade.core

object GameRegistry {
    val definitions = listOf(
        TapCounterGame(), QuickMathGame(), TicTacToeGame(), HigherLowerGame(),
        MemoryChallengeGame(), WordScrambleGame(), NumberSequenceGame()
    )
    fun create(id: String): ArcadeGame = when (id) {
        "quick_math" -> QuickMathGame(); "tic_tac_toe" -> TicTacToeGame(); "higher_lower" -> HigherLowerGame(); "memory_challenge" -> MemoryChallengeGame(); "word_scramble" -> WordScrambleGame(); "number_sequence" -> NumberSequenceGame(); else -> TapCounterGame()
    }
    fun isKnown(id: String) = definitions.any { it.id == id }
}
