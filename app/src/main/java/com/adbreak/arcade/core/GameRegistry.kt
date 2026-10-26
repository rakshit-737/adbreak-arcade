package com.adbreak.arcade.core

object GameRegistry {
    private val factories: LinkedHashMap<String, () -> ArcadeGame> = linkedMapOf(
        "tap_counter" to { TapCounterGame() },
        "quick_math" to { QuickMathGame() },
        "tic_tac_toe" to { TicTacToeGame() },
        "higher_lower" to { HigherLowerGame() },
        "memory_challenge" to { MemoryChallengeGame() },
        "word_scramble" to { WordScrambleGame() },
        "number_sequence" to { NumberSequenceGame() },
        "rock_paper_scissors" to { RockPaperScissorsGame() },
        "odd_one_out" to { OddOneOutGame() },
        "emoji_decode" to { EmojiDecodeGame() },
        "trivia_blitz" to { TriviaBlitzGame() },
        "mastermind_mini" to { MastermindMiniGame() },
        "nim" to { NimGame() },
        "connect_three" to { ConnectThreeGame() },
        "guess_the_pattern" to { GuessThePatternGame() },
        "anagram_challenge" to { AnagramChallengeGame() },
        "lights_out_mini" to { LightsOutMiniGame() },
        "cyber_heist" to { CyberHeistGame() },
        "pocket_dungeon" to { PocketDungeonGame() },
        "neon_tactics" to { NeonTacticsGame() },
        "cipher_vault" to { CipherVaultGame() },
        "prism_protocol" to { PrismProtocolGame() },
        "starship_survival" to { StarshipSurvivalGame() },
        "micro_tower_command" to { MicroTowerCommandGame() },
        "pulse_reactor" to { PulseReactorGame() },
    )

    /** Fresh instances prevent a library preview from mutating a running game. */
    val definitions: List<ArcadeGame>
        get() = factories.values.map { it() }

    fun create(id: String): ArcadeGame = factories[id]?.invoke() ?: error("Unknown game id: $id")

    fun isKnown(id: String) = id in factories
}
