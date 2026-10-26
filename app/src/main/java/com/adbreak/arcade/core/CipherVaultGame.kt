package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.Random

/**
 * A notification-friendly code deduction game. Digits are selected with the
 * nine cell actions and the draft is submitted with a separate action, so the
 * game never needs a text field or a keyboard.
 */
class CipherVaultGame(private val random: Random = Random()) : ArcadeGame {
    data class ClueResult(val exact: Int, val misplaced: Int)

    companion object {
        private const val VERSION = 2
        private const val DIGIT_COUNT = 9
        private const val BASE_CODE_LENGTH = 3
        private const val MAX_DIFFICULTY = 5
        private const val MAX_ATTEMPTS = 6
        private const val MAX_SCORE = 10_000_000

        @JvmStatic
        fun evaluateClues(secret: String, guess: String): ClueResult {
            require(secret.isNotEmpty() && secret.length == guess.length) {
                "Secret and guess must have the same non-zero length"
            }
            require(secret.all { it in '1'..'9' } && guess.all { it in '1'..'9' }) {
                "Cipher digits must be in the range 1..9"
            }

            var exact = 0
            val remainingSecret = IntArray(DIGIT_COUNT)
            val remainingGuess = IntArray(DIGIT_COUNT)
            secret.indices.forEach { index ->
                if (secret[index] == guess[index]) {
                    exact++
                } else {
                    remainingSecret[secret[index] - '1']++
                    remainingGuess[guess[index] - '1']++
                }
            }
            val misplaced = remainingSecret.indices.sumOf { digit ->
                minOf(remainingSecret[digit], remainingGuess[digit])
            }
            return ClueResult(exact, misplaced)
        }

        private fun codeLengthFor(difficulty: Int): Int = BASE_CODE_LENGTH + if (difficulty >= 4) 1 else 0

        private fun digitCountFor(difficulty: Int): Int = difficulty + 3

        private fun attemptLimitFor(difficulty: Int): Int = MAX_ATTEMPTS + (difficulty - 1) / 2

        private fun difficultyFor(solved: Int): Int = (1 + solved).coerceAtMost(MAX_DIFFICULTY)

        private fun hintBudgetFor(difficulty: Int): Int = (3 - difficulty / 2).coerceAtLeast(1)
    }

    override val id = "cipher_vault"
    override val title = "Cipher Vault"
    override val description = "Stage a digit code, read exact and misplaced clues, and crack the vault."

    private var secret = ""
    private var draft = ""
    var difficulty = 1
        private set
    var attempts = 0
        private set
    var hintsUsed = 0
        private set
    var hintsAvailable = 0
        private set
    private var revealed = mutableListOf<Int?>()
    private var roundEnded = false
    private var won = false
    private var feedback = ""
    private var legacyRules = false
    private var history = mutableListOf<String>()
    private var historyStart = 0
    private var historyCursor = 0
    private var cachedCandidates: Int? = null

    private val attemptLimit: Int
        get() = if (legacyRules) MAX_ATTEMPTS else attemptLimitFor(difficulty)

    private val activeDigits: Int
        get() = if (legacyRules) DIGIT_COUNT else digitCountFor(difficulty)

    var score = 0
        private set
    var solved = 0
        private set
    var streak = 0
        private set
    var bestStreak = 0
        private set

    init {
        startRound()
    }

    private fun startRound() {
        legacyRules = false
        difficulty = difficultyFor(solved)
        val length = codeLengthFor(difficulty)
        secret = buildString(length) {
            repeat(length) { append(random.nextInt(activeDigits) + 1) }
        }
        draft = ""
        attempts = 0
        hintsUsed = 0
        hintsAvailable = hintBudgetFor(difficulty)
        revealed = MutableList(length) { null }
        roundEnded = false
        won = false
        history.clear()
        historyStart = 0
        historyCursor = 0
        cachedCandidates = null
        feedback = "Use 1–$activeDigits · repeats allowed · exact = right position; misplaced = wrong position"
    }

    override fun reset() {
        score = 0
        solved = 0
        streak = 0
        bestStreak = 0
        startRound()
    }

    override fun apply(action: GameAction): Boolean {
        if (action == GameAction.RESET) {
            reset()
            return true
        }
        if (action == GameAction.NEW_GAME) {
            if (!roundEnded && (attempts > 0 || hintsUsed > 0 || draft.isNotEmpty())) streak = 0
            startRound()
            return true
        }
        if (roundEnded) return false

        return when (action) {
            GameAction.PRIMARY -> primaryAction()
            GameAction.SECONDARY -> useHint()
            GameAction.CELL_1, GameAction.CELL_2, GameAction.CELL_3,
            GameAction.CELL_4, GameAction.CELL_5, GameAction.CELL_6,
            GameAction.CELL_7, GameAction.CELL_8, GameAction.CELL_9 -> addDigit(action)
            else -> false
        }
    }

    /** The primary control clears a partial draft and submits a complete one. */
    private fun primaryAction(): Boolean {
        if (draft.isEmpty()) return false
        if (draft.length < secret.length) {
            draft = ""
            feedback = "Draft cleared"
            return true
        }
        return submitDraft()
    }

    private fun addDigit(action: GameAction): Boolean {
        if (!legacyRules && action == GameAction.CELL_9) {
            if (draft.isEmpty()) {
                if (history.isEmpty()) return false
                historyCursor = (historyCursor + 1) % history.size
                return true
            }
            draft = draft.dropLast(1)
            feedback = "Last digit removed"
            return true
        }
        if (draft.length >= secret.length) return false
        val digit = when (action) {
            GameAction.CELL_1 -> 1
            GameAction.CELL_2 -> 2
            GameAction.CELL_3 -> 3
            GameAction.CELL_4 -> 4
            GameAction.CELL_5 -> 5
            GameAction.CELL_6 -> 6
            GameAction.CELL_7 -> 7
            GameAction.CELL_8 -> 8
            GameAction.CELL_9 -> 9
            else -> return false
        }
        if (digit > activeDigits) return false
        draft += digit.toString()
        feedback = if (draft.length == secret.length) {
            "Guess ready — press SUBMIT"
        } else {
            "Draft ${draft.length}/${secret.length}"
        }
        return true
    }

    private fun useHint(): Boolean {
        if (hintsAvailable <= 0) return false
        val index = revealed.indexOfFirst { it == null }
        if (index < 0) return false
        revealed[index] = secret[index].toString().toInt()
        hintsUsed++
        hintsAvailable--
        cachedCandidates = null
        feedback = "Hint — position ${index + 1} is ${secret[index]}"
        return true
    }

    private fun submitDraft(): Boolean {
        val clues = evaluateClues(secret, draft)
        history += draft
        historyCursor = history.lastIndex
        cachedCandidates = null
        attempts++
        draft = ""

        if (clues.exact == secret.length) {
            won = true
            roundEnded = true
            solved = (solved + 1).coerceAtMost(MAX_SCORE)
            streak = (streak + 1).coerceAtMost(MAX_SCORE)
            bestStreak = maxOf(bestStreak, streak)
            val roundScore = difficulty * 100 +
                (attemptLimit - attempts) * 20 +
                hintsAvailable * 15 +
                (streak - 1) * 25
            score = (score.toLong() + roundScore).coerceAtMost(MAX_SCORE.toLong()).toInt()
            feedback = "Vault opened — $secret · ${clues.exact} exact · +$roundScore"
        } else if (attempts >= attemptLimit) {
            roundEnded = true
            won = false
            streak = 0
            feedback = "Vault sealed — code $secret · ${clues.exact} exact · ${clues.misplaced} misplaced"
        } else {
            feedback = "${clues.exact} exact · ${clues.misplaced} misplaced"
        }
        return true
    }

    private fun displayPattern(): String {
        if (roundEnded) return secret.toList().joinToString(" ")
        return secret.indices.joinToString(" ") { index ->
            draft.getOrNull(index)?.toString()
                ?: "·"
        }
    }

    private fun digitAction(digit: Int): GameAction = when (digit) {
        1 -> GameAction.CELL_1
        2 -> GameAction.CELL_2
        3 -> GameAction.CELL_3
        4 -> GameAction.CELL_4
        5 -> GameAction.CELL_5
        6 -> GameAction.CELL_6
        7 -> GameAction.CELL_7
        8 -> GameAction.CELL_8
        else -> GameAction.CELL_9
    }

    override fun render(): GameViewState {
        val pattern = displayPattern()
        val controls = when {
            roundEnded -> listOf("NEW VAULT" to GameAction.NEW_GAME)
            else -> listOf(
                (if (draft.length == secret.length) "SUBMIT" else "CLEAR DRAFT") to GameAction.PRIMARY,
                "HINT ($hintsAvailable)" to GameAction.SECONDARY,
            )
        }
        return GameViewState(
            title = title,
            subtitle = if (roundEnded) {
                if (won) "Vault opened · code $secret" else "Vault sealed · code $secret"
            } else {
                "Match ${secret.length} digits (1–$activeDigits) · known ${revealed.joinToString("") { it?.toString() ?: "·" }} · ${if (legacyRules) "legacy vault" else "${candidateCount()} candidates"}"
            },
            scoreLine = "Guess $pattern  ·  Attempts $attempts/$attemptLimit  ·  Hints $hintsAvailable  ·  Score $score  ·  Streak $streak",
            buttons = controls.map { it.first },
            buttonActions = controls.map { it.second },
            cellButtons = if (roundEnded) emptyList() else (1..DIGIT_COUNT).map {
                if (!legacyRules && it == 9) {
                    if (draft.isEmpty() && history.isNotEmpty()) "CLUE ›" else "⌫ EDIT"
                } else it.toString()
            },
            cellActions = if (roundEnded) emptyList() else (1..DIGIT_COUNT).map(::digitAction),
            feedback = buildString {
                // Keep the selected clue FIRST: the notification may show only one
                // status line. CLUE cycles the saved log without spending a guess.
                if (history.isNotEmpty()) {
                    val guess = history[historyCursor]
                    val clue = evaluateClues(secret, guess)
                    append("Clue ${historyStart + historyCursor + 1}: $guess · ${clue.exact} exact ${clue.misplaced} misplaced\n")
                }
                append(feedback)
                if (hintsUsed > 0) append("\nKnown: ${revealed.joinToString(" ") { it?.toString() ?: "·" }}")
                if (historyStart > 0) append("\n$historyStart earlier guesses unavailable in legacy save")
                if (history.isNotEmpty()) {
                    append("\nHistory (E exact / M misplaced): ")
                    append(history.joinToString("; ") { guess ->
                        val clue = evaluateClues(secret, guess)
                        "$guess=${clue.exact}E${clue.misplaced}M"
                    })
                }
            },
            isTerminal = roundEnded,
            phase = when {
                !roundEnded -> GamePhase.PLAYING
                won -> GamePhase.WON
                else -> GamePhase.LOST
            },
            progress = GameProgress(
                maxOf(revealed.count { it != null }, history.maxOfOrNull { evaluateClues(secret, it).exact } ?: 0),
                secret.length,
                "Best exact positions · ${attemptLimit - attempts} guesses left",
            ),
            record = bestStreak.takeIf { it > 0 }?.let { GameRecord(it, "Solved vault streak") },
            disabledActions = if (roundEnded) emptyMap() else buildMap {
                if (draft.isEmpty()) put(GameAction.PRIMARY, "Choose digits first")
                if (hintsAvailable == 0) put(GameAction.SECONDARY, "No hints left")
                (1..DIGIT_COUNT).forEach { digit ->
                    val reason = when {
                        !legacyRules && digit == 9 -> if (draft.isEmpty() && history.isEmpty()) "No draft or clues yet" else null
                        digit > activeDigits -> "This vault uses 1–$activeDigits"
                        draft.length == secret.length -> "Submit or edit this guess first"
                        else -> null
                    }
                    if (reason != null) put(digitAction(digit), reason)
                }
            },
        )
    }

    /** Counts only candidates consistent with information already earned by the player. */
    private fun candidateCount(): Int {
        cachedCandidates?.let { return it }
        val clues = history.map { evaluateClues(secret, it) }
        var count = 0
        fun visit(prefix: String) {
            if (prefix.length == secret.length) {
                if (history.indices.all { evaluateClues(prefix, history[it]) == clues[it] }) count++
                return
            }
            val known = revealed[prefix.length]
            if (known != null) visit(prefix + known) else (1..activeDigits).forEach { visit(prefix + it) }
        }
        visit("")
        cachedCandidates = count
        return count
    }

    override fun encode(): String = JSONObject().apply {
        put("v", VERSION)
        put("secret", secret)
        put("draft", draft)
        put("difficulty", difficulty)
        put("attempts", attempts)
        put("hintsUsed", hintsUsed)
        put("hintsAvailable", hintsAvailable)
        put("revealed", JSONArray().apply { revealed.forEach { put(it?.toString() ?: "") } })
        put("roundEnded", roundEnded)
        put("won", won)
        put("score", score)
        put("solved", solved)
        put("streak", streak)
        put("bestStreak", bestStreak)
        put("feedback", feedback)
        put("rules", if (legacyRules) 1 else 2)
        put("history", JSONArray(history))
        put("historyStart", historyStart)
        put("historyCursor", historyCursor)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..VERSION) return false
        val rules = if (version == 1) 1 else json.optInt("rules", -1)
        if (rules !in 1..2) return false
        val restoredLegacy = rules == 1

        val restoredSecret = json.optString("secret", "")
        val restoredDraft = json.optString("draft", "")
        val restoredDifficulty = json.optInt("difficulty", -1)
        val restoredAttempts = json.optInt("attempts", -1)
        val restoredHintsUsed = json.optInt("hintsUsed", -1)
        val restoredHintsAvailable = json.optInt("hintsAvailable", -1)
        val restoredArray = json.optJSONArray("revealed") ?: return false
        val restoredRevealed = mutableListOf<Int?>()
        for (index in 0 until restoredArray.length()) {
            val value = restoredArray.optString(index, "")
            if (value.isBlank()) {
                restoredRevealed += null
            } else {
                if (value.length != 1 || value[0] !in '1'..'9') return false
                restoredRevealed += value.toInt()
            }
        }
        val restoredEnded = json.optBoolean("roundEnded", false)
        val restoredWon = json.optBoolean("won", false)
        val restoredScore = json.optInt("score", -1)
        val restoredSolved = json.optInt("solved", -1)
        val restoredStreak = json.optInt("streak", -1)
        val restoredBest = json.optInt("bestStreak", -1)
        val restoredFeedback = json.optString("feedback", "")
        val restoredHistoryStart = if (version == 1) restoredAttempts else json.optInt("historyStart", -1)
        val restoredHistory = if (version == 1) mutableListOf() else {
            val array = json.optJSONArray("history") ?: return false
            MutableList(array.length()) { array.optString(it, "") }
        }
        val restoredCursor = if (version == 1) 0 else json.optInt("historyCursor", -1)
        val expectedLength = if (restoredDifficulty in 1..MAX_DIFFICULTY) {
            if (restoredLegacy) BASE_CODE_LENGTH + restoredDifficulty else codeLengthFor(restoredDifficulty)
        } else {
            -1
        }
        val expectedHintBudget = if (restoredDifficulty in 1..MAX_DIFFICULTY) {
            hintBudgetFor(restoredDifficulty)
        } else {
            -1
        }
        val expectedDifficulty = if (restoredEnded && restoredWon) {
            difficultyFor((restoredSolved - 1).coerceAtLeast(0))
        } else {
            difficultyFor(restoredSolved.coerceAtLeast(0))
        }
        val expectedAttempts = if (restoredLegacy) MAX_ATTEMPTS else attemptLimitFor(restoredDifficulty)
        val expectedDigits = if (restoredLegacy) DIGIT_COUNT else digitCountFor(restoredDifficulty)

        if (restoredDifficulty !in 1..MAX_DIFFICULTY ||
            restoredDifficulty != expectedDifficulty ||
            restoredSecret.length != expectedLength ||
            restoredSecret.any { it !in '1'..('0' + expectedDigits) } ||
            restoredDraft.length > restoredSecret.length ||
            restoredDraft.any { it !in '1'..('0' + expectedDigits) } ||
            restoredRevealed.size != restoredSecret.length ||
            restoredRevealed.indices.any { index ->
                val value = restoredRevealed[index]
                value != null && value != restoredSecret[index].toString().toInt()
            } ||
            restoredHintsUsed !in 0..expectedHintBudget ||
            restoredHintsAvailable != expectedHintBudget - restoredHintsUsed ||
            restoredRevealed.count { it != null } != restoredHintsUsed ||
            restoredAttempts !in 0..expectedAttempts ||
            restoredScore !in 0..MAX_SCORE ||
            restoredSolved !in 0..MAX_SCORE ||
            restoredStreak !in 0..restoredBest ||
            restoredBest !in 0..MAX_SCORE ||
            restoredFeedback.isBlank() ||
            restoredWon && !restoredEnded ||
            restoredEnded != (restoredWon || restoredAttempts == expectedAttempts) ||
            restoredWon && restoredStreak <= 0 ||
            !restoredWon && restoredEnded && restoredStreak != 0 ||
            restoredEnded && restoredDraft.isNotEmpty() ||
            restoredEnded && restoredAttempts == 0 ||
            restoredWon && restoredSolved == 0
        ) return false
        if (restoredHistoryStart !in 0..restoredAttempts ||
            restoredCursor !in 0..restoredHistory.lastIndex.coerceAtLeast(0) ||
            !restoredLegacy && restoredHistoryStart != 0 ||
            restoredHistory.size + restoredHistoryStart != restoredAttempts ||
            restoredHistory.any { guess -> guess.length != expectedLength || guess.any { it !in '1'..('0' + expectedDigits) } } ||
            restoredHistory.dropLast(1).any { it == restoredSecret } ||
            restoredHistory.isNotEmpty() && (restoredHistory.last() == restoredSecret) != restoredWon
        ) return false

        secret = restoredSecret
        draft = restoredDraft
        difficulty = restoredDifficulty
        attempts = restoredAttempts
        hintsUsed = restoredHintsUsed
        hintsAvailable = restoredHintsAvailable
        revealed = restoredRevealed
        roundEnded = restoredEnded
        won = restoredWon
        score = restoredScore
        solved = restoredSolved
        streak = restoredStreak
        bestStreak = restoredBest
        feedback = restoredFeedback
        legacyRules = restoredLegacy
        history = restoredHistory
        historyStart = restoredHistoryStart
        historyCursor = restoredCursor
        cachedCandidates = null
        true
    }.getOrDefault(false)
}
