package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque
import java.util.Random

/** Player-level, deterministic model simulations; no Android/device assumptions. */
class CampaignPolishTest {
    @Test
    fun neonMovementAndDefendCannotFarmAfterDisablingTheDrone() {
        val game = NeonTacticsGame()
        repeat(3) { assertTrue(game.apply(GameAction.PRIMARY)) }
        repeat(2) { assertTrue(game.apply(GameAction.CELL_2)) }
        assertEquals(30, game.score)
        repeat(500) {
            assertTrue(game.apply(GameAction.PRIMARY))
            assertTrue(game.apply(GameAction.CELL_4))
            assertTrue(game.apply(GameAction.CELL_1))
        }
        assertEquals(30, game.score)
        assertEquals(30, game.bestScore)
        assertEquals(30, game.render().record!!.value)
        // A disabled drone's cell is traversable and remains so after restoration.
        assertTrue(game.apply(GameAction.CELL_2))
        val restored = NeonTacticsGame()
        assertTrue(restored.restore(game.encode()))
        assertEquals(game.render(), restored.render())
    }

    @Test
    fun neonOptionalDataMakesARealDetourWhileTheQuickRouteStillWins() {
        val fast = NeonTacticsGame()
        assertTrue(fast.apply(GameAction.CELL_4))
        assertTrue(fast.apply(GameAction.CELL_7))
        assertEquals(GamePhase.WON, fast.render().phase)
        assertEquals(2, fast.turn)
        assertEquals(53, fast.score)

        val salvage = NeonTacticsGame()
        repeat(3) { assertTrue(salvage.apply(GameAction.PRIMARY)) }
        repeat(2) { assertTrue(salvage.apply(GameAction.CELL_2)) }
        listOf(GameAction.CELL_2, GameAction.CELL_3, GameAction.CELL_2, GameAction.CELL_1,
            GameAction.CELL_4, GameAction.CELL_7).forEach { assertTrue(salvage.apply(it)) }
        assertEquals(GamePhase.WON, salvage.render().phase)
        assertEquals(11, salvage.turn)
        assertEquals(83, salvage.score)
        assertTrue(salvage.score > fast.score)
        assertTrue(salvage.playerHp < fast.playerHp)
        assertTrue(salvage.render().feedback.contains("DATA recovered"))
    }

    @Test
    fun neonReachableStatesHaveHonestTelegraphsDisabledControlsAndStableRestores() {
        val queue = ArrayDeque<String>()
        queue.add(NeonTacticsGame().encode())
        val visited = mutableSetOf<String>()
        var wins = 0
        var losses = 0
        while (queue.isNotEmpty()) {
            val encoded = queue.removeFirst()
            val json = JSONObject(encoded)
            val key = listOf("playerCell", "enemyCell", "playerHp", "enemyHp", "turn", "dataRecovered", "over")
                .joinToString { json.get(it).toString() }
            if (!visited.add(key)) continue
            val game = NeonTacticsGame()
            assertTrue(game.restore(encoded))
            assertEquals(encoded, game.encode())
            val view = game.render()
            if (view.isTerminal) {
                if (view.phase == GamePhase.WON) wins++ else losses++
                assertFalse(game.apply(GameAction.PRIMARY))
                assertEquals(encoded, game.encode())
                continue
            }
            (view.buttonActions + view.cellActions).forEach { action ->
                val next = NeonTacticsGame()
                assertTrue(next.restore(encoded))
                val accepted = next.apply(action)
                assertEquals(action !in view.disabledActions, accepted)
                if (!accepted) assertEquals(encoded, next.encode())
                else {
                    assertTrue(next.score in 0..107)
                    if (view.subtitle.contains("ADVANCE")) assertEquals(game.playerHp, next.playerHp)
                    val roundTrip = NeonTacticsGame()
                    assertTrue(roundTrip.restore(next.encode()))
                    assertEquals(next.render(), roundTrip.render())
                    if (next.turn <= 10) queue.add(next.encode())
                }
            }
        }
        assertTrue(wins > 0)
        assertTrue(losses > 0)
        println("Neon exhaustive reachable states through turn 10: ${visited.size}; win states $wins; loss states $losses")
    }

    @Test
    fun heistSafeAndAllCacheStrategiesWinAcrossSeedsWithNineDecisions() {
        repeat(128) { seed ->
            val safe = CyberHeistGame(Random(seed.toLong()))
            val caches = CyberHeistGame(Random(seed.toLong()))
            repeat(3) { mission ->
                listOf(GameAction.PRIMARY, GameAction.CELL_1, GameAction.PRIMARY).forEach {
                    assertTrue(safe.apply(it))
                }
                // Spending two tools on the FIRST breach preserves enough intel
                // for all three risky cache exfils; repeating social loop cannot.
                listOf(GameAction.CELL_1, if (mission == 0) GameAction.SECONDARY else GameAction.CELL_1, GameAction.CELL_2)
                    .forEach { assertTrue(caches.apply(it)) }
                val copy = CyberHeistGame(Random(0))
                assertTrue(copy.restore(caches.encode()))
                assertEquals(caches.render(), copy.render())
            }
            assertEquals(GamePhase.WON, safe.render().phase)
            assertEquals(GamePhase.WON, caches.render().phase)
            assertEquals(9, caches.render().progress!!.current)
            assertEquals(3, JSONObject(caches.encode()).getInt("cachesSecured"))
            assertEquals(3, JSONObject(safe.encode()).getInt("cleanMissions"))
            assertTrue(caches.score > safe.score)
            assertEquals(0, caches.tools)
            assertEquals(0, caches.intel)
        }
    }

    @Test
    fun heistShowsPricesAndPredictableLockdownWithoutMutatingRejectedChoices() {
        val game = CyberHeistGame(Random(2))
        val state = JSONObject(game.encode()).put("tools", 0)
        assertTrue(game.restore(state.toString()))
        assertTrue(game.render().buttons.all { it.contains("A") })
        assertTrue(game.render().disabledActions.containsKey(GameAction.CELL_1))
        val before = game.encode()
        assertFalse(game.apply(GameAction.CELL_1))
        assertEquals(before, game.encode())
        assertTrue(game.apply(GameAction.SECONDARY))
        val restored = CyberHeistGame(Random(1))
        assertTrue(restored.restore(game.encode()))
        assertEquals(game.render(), restored.render())
    }

    @Test
    fun dungeonGuardLoopEndsInFiniteEnemyDamageAndCannotFarmScore() {
        val game = PocketDungeonGame()
        assertTrue(game.apply(GameAction.PRIMARY))
        repeat(2) { assertTrue(game.apply(GameAction.SECONDARY)) }
        assertEquals(0, game.score)
        assertTrue(game.render().subtitle.contains("HEAVY 5"))
        repeat(7) { assertTrue(game.apply(GameAction.SECONDARY)) }
        assertFalse(game.inCombat)
        assertEquals(1, game.enemiesDefeated)
        assertEquals(20, game.score) // 5 finite damage × 2 + 10 enemy loot
        assertEquals(10, game.turns)
    }

    @Test
    fun dungeonHeavyStrikeIsTelegraphedAndParrySurvivesRestoration() {
        val guard = PocketDungeonGame()
        assertTrue(guard.apply(GameAction.PRIMARY))
        repeat(2) { assertTrue(guard.apply(GameAction.SECONDARY)) }
        val attack = PocketDungeonGame()
        assertTrue(attack.restore(guard.encode()))
        assertEquals(guard.render(), attack.render())
        assertTrue(guard.apply(GameAction.SECONDARY))
        assertTrue(attack.apply(GameAction.PRIMARY))
        assertEquals(20, guard.health)
        assertEquals(15, attack.health)
        assertEquals(3, guard.enemyHealth)
        assertEquals(1, attack.enemyHealth)
        assertTrue(guard.render().feedback.contains("parried"))
        assertTrue(guard.render().subtitle.contains("STRIKE 2"))
    }

    @Test
    fun dungeonRelicAndChapelChoicesTradeHealthForEarnedRewards() {
        fun run(relic: Boolean, prayer: Boolean): PocketDungeonGame {
            val game = PocketDungeonGame()
            repeat(12) {
                val action = when {
                    !game.inCombat && game.roomIndex == 1 && !relic -> GameAction.SECONDARY
                    !game.inCombat && game.roomIndex == 2 && prayer -> GameAction.SECONDARY
                    else -> GameAction.PRIMARY
                }
                assertTrue(game.apply(action))
                val restored = PocketDungeonGame()
                assertTrue(restored.restore(game.encode()))
                assertEquals(game.render(), restored.render())
            }
            assertEquals(GamePhase.WON, game.render().phase)
            assertEquals(12, game.turns)
            assertEquals(5, game.render().progress!!.current)
            val end = game.encode()
            assertFalse(game.apply(GameAction.PRIMARY))
            assertEquals(end, game.encode())
            return game
        }
        val relic = run(relic = true, prayer = false)
        val safe = run(relic = false, prayer = false)
        val ward = run(relic = true, prayer = true)
        assertEquals(291, relic.score)
        assertEquals(245, safe.score)
        assertEquals(9, relic.health)
        assertEquals(11, ward.health)
        assertTrue(relic.apply(GameAction.NEW_GAME))
        assertEquals(0, relic.score)
        assertEquals(0, relic.roomIndex)
        assertFalse(JSONObject(relic.encode()).getBoolean("relic"))
    }

    @Test
    fun dungeonResourceBoundariesStayVisibleAndRejectWithoutMutation() {
        val game = PocketDungeonGame()
        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.render().disabledActions.containsKey(GameAction.CELL_1))
        val full = game.encode()
        assertFalse(game.apply(GameAction.CELL_1))
        assertEquals(full, game.encode())
        repeat(2) { assertTrue(game.apply(GameAction.PRIMARY)) }
        assertTrue(game.restore(JSONObject(game.encode()).put("health", 3).toString()))
        val fragile = game.encode()
        assertTrue(game.render().disabledActions.containsKey(GameAction.PRIMARY))
        assertFalse(game.apply(GameAction.PRIMARY))
        assertEquals(fragile, game.encode())
        assertTrue(game.apply(GameAction.SECONDARY))
        assertEquals(3, game.health)
        assertTrue(game.apply(GameAction.PRIMARY))
        assertEquals(11, game.health)
        assertTrue(game.restore(JSONObject(game.encode()).put("potions", 0).toString()))
        assertTrue(game.render().disabledActions.containsKey(GameAction.SECONDARY))
        val empty = game.encode()
        assertFalse(game.apply(GameAction.SECONDARY))
        assertEquals(empty, game.encode())
    }

    @Test
    fun cipherHistoryAndKnownPositionsRemainVisibleWhileEditingAndBrowsing() {
        val game = CipherVaultGame(Random(1))
        val initial = JSONObject(game.encode()).put("secret", "123")
        assertTrue(game.restore(initial.toString()))
        stage(game, "111")
        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.render().feedback.startsWith("Clue 1: 111 · 1 exact 0 misplaced"))
        stage(game, "222")
        assertTrue(game.render().feedback.startsWith("Clue 1: 111"))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.render().feedback.startsWith("Clue 2: 222"))
        val attempts = game.attempts
        assertTrue(game.apply(GameAction.CELL_9)) // browse, without spending a guess
        assertEquals(attempts, game.attempts)
        assertTrue(game.render().feedback.startsWith("Clue 1: 111"))
        assertTrue(game.render().feedback.contains("222=1E0M"))
        assertTrue(game.apply(GameAction.SECONDARY))
        assertTrue(game.render().subtitle.contains("known 1··"))
        stage(game, "124")
        assertTrue(game.apply(GameAction.CELL_9)) // edit a FULL guess, not an accidental submit
        assertEquals("12", JSONObject(game.encode()).getString("draft"))
        assertTrue(game.render().feedback.startsWith("Clue 1: 111"))
        val restored = CipherVaultGame(Random(4))
        assertTrue(restored.restore(game.encode()))
        assertEquals(game.render(), restored.render())
        assertTrue(restored.apply(GameAction.CELL_3))
        assertTrue(restored.apply(GameAction.PRIMARY))
        assertEquals(GamePhase.WON, restored.render().phase)
        assertTrue(restored.render().feedback.contains("111=1E0M; 222=1E0M; 123=3E0M"))
        val terminal = restored.encode()
        assertFalse(restored.apply(GameAction.CELL_9))
        assertEquals(terminal, restored.encode())
    }

    @Test
    fun cipherRoundStreakRewardsSolvedVaultsRatherThanOnlyFirstGuesses() {
        val game = CipherVaultGame(Random(8))
        stage(game, JSONObject(game.encode()).getString("secret"))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.NEW_GAME))
        val code = JSONObject(game.encode()).getString("secret")
        stage(game, (if (code.first() == '1') "2" else "1") + code.drop(1))
        assertTrue(game.apply(GameAction.PRIMARY))
        assertEquals(1, game.streak)
        stage(game, code)
        assertTrue(game.apply(GameAction.PRIMARY))
        assertEquals(2, game.streak)
        assertEquals(2, game.render().record!!.value)
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertTrue(game.apply(GameAction.SECONDARY))
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertEquals(0, game.streak) // abandoning a used vault cannot preserve a streak
    }

    @Test
    fun clueEvaluatorExhaustsAllNineDigitTriplesAndFourPositionEqualityPatterns() {
        // Independent oracle counts total symbol overlap, then removes exact hits.
        fun check(secret: String, guess: String) {
            assertEquals("$secret / $guess", clueOracle(secret, guess), CipherVaultGame.evaluateClues(secret, guess))
        }
        val triples = codes(9, 3)
        triples.forEach { secret -> triples.forEach { guess -> check(secret, guess) } }
        var patterns = 0
        // Restricted-growth strings cover EVERY equality pattern of eight slots
        // (two 4-digit codes), including eight distinct symbols and every duplicate.
        fun equalityPatterns(prefix: String, greatest: Int) {
            if (prefix.length == 8) {
                check(prefix.take(4), prefix.drop(4))
                patterns++
            } else {
                (1..minOf(9, greatest + 1)).forEach { equalityPatterns(prefix + it, maxOf(greatest, it)) }
            }
        }
        equalityPatterns("1", 1)
        assertEquals(4_140, patterns)
        val legacy = codes(2, 8)
        legacy.forEach { secret -> legacy.forEach { guess -> check(secret, guess) } }
        println("Clue oracle checked 531441 nine-digit triple pairs, 4140 exhaustive 4-slot equality patterns, and 65536 legacy 8-slot binary pairs")
    }

    @Test
    fun clueEvaluatorRejectsInvalidDomains() {
        listOf("" to "", "12" to "123", "120" to "123", "abc" to "123", "123" to "9x9").forEach { (secret, guess) ->
            var rejected = false
            try {
                CipherVaultGame.evaluateClues(secret, guess)
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertTrue("$secret / $guess must be rejected", rejected)
        }
    }

    @Test
    fun cipherDeductionPolicySolvesEveryBeginnerCodeAndEveryHardestHintedSuffix() {
        // Guesses are chosen by minimax partitions of visible clue information,
        // never by peeking at the live model's secret. Every branch is played.
        listOf(1, 5).forEach { level ->
            val alphabet = level + 3
            val tails = codes(alphabet, 3)
            val plan = deductionTree(tails)
            var maxGuesses = 0
            tails.forEach { tail ->
                val knownPrefix = if (level == 5) "1" else ""
                val game = CipherVaultGame(Random(0))
                val initial = JSONObject(game.encode())
                    .put("secret", knownPrefix + tail)
                    .put("difficulty", level)
                    .put("solved", level - 1)
                    .put("hintsAvailable", if (level == 5) 1 else 3)
                    .put("revealed", JSONArray(List(knownPrefix.length + 3) { "" }))
                assertTrue(game.restore(initial.toString()))
                if (level == 5) assertTrue(game.apply(GameAction.SECONDARY))
                var node = plan
                while (!game.render().isTerminal) {
                    stage(game, knownPrefix + node.guess)
                    assertTrue(game.apply(GameAction.PRIMARY))
                    val restored = CipherVaultGame(Random(9))
                    assertTrue(restored.restore(game.encode()))
                    assertEquals(game.render(), restored.render())
                    if (!game.render().isTerminal) {
                        val displayed = Regex("(\\d+) exact (\\d+) misplaced").find(game.render().feedback)!!
                        val result = CipherVaultGame.ClueResult(displayed.groupValues[1].toInt() - knownPrefix.length, displayed.groupValues[2].toInt())
                        node = node.branches.getValue(result)
                    }
                }
                assertEquals("level $level code ${knownPrefix + tail}", GamePhase.WON, game.render().phase)
                maxGuesses = maxOf(maxGuesses, game.attempts)
            }
            assertTrue(maxGuesses <= if (level == 5) 8 else 6)
            println("Cipher level $level: all ${tails.size} ${if (level == 5) "hinted suffixes" else "codes without hints"} solved; maximum $maxGuesses guesses")
        }
    }

    @Test
    fun versionOneCampaignSavesMigrateAtomicallyWithoutImportingFarmedRecords() {
        val neon = NeonTacticsGame()
        repeat(3) { assertTrue(neon.apply(GameAction.PRIMARY)) }
        repeat(2) { assertTrue(neon.apply(GameAction.CELL_2)) }
        val oldNeon = JSONObject(neon.encode()).put("v", 1).put("score", 900_000).put("bestScore", 900_000)
        oldNeon.remove("dataRecovered")
        assertTrue(neon.restore(oldNeon.toString()))
        assertEquals(30, neon.score)
        assertEquals(30, neon.bestScore)

        val dungeon = PocketDungeonGame()
        assertTrue(dungeon.apply(GameAction.PRIMARY))
        val oldDungeon = JSONObject(dungeon.encode()).put("v", 1).put("score", 999_999)
        listOf("combatBeat", "ward", "relic").forEach { oldDungeon.remove(it) }
        assertTrue(dungeon.restore(oldDungeon.toString()))
        assertTrue(dungeon.inCombat)
        assertEquals(0, dungeon.score)

        val heist = CyberHeistGame(Random(4))
        assertTrue(heist.apply(GameAction.PRIMARY))
        val oldHeist = JSONObject(heist.encode()).put("v", 1)
        listOf("cacheMapped", "cachesSecured", "cleanMissions").forEach { oldHeist.remove(it) }
        val heistScore = heist.score
        assertTrue(heist.restore(oldHeist.toString()))
        assertEquals(heistScore, heist.score)

        val cipher = CipherVaultGame(Random(1))
        val oldCipher = JSONObject(cipher.encode()).put("v", 1).put("secret", "9129")
            .put("draft", "9").put("attempts", 2).put("revealed", JSONArray(listOf("", "", "", "")))
        listOf("rules", "history", "historyStart", "historyCursor").forEach { oldCipher.remove(it) }
        assertTrue(cipher.restore(oldCipher.toString()))
        assertTrue(cipher.render().feedback.contains("2 earlier guesses unavailable"))
        assertTrue(cipher.apply(GameAction.CELL_1))
        assertTrue(cipher.apply(GameAction.CELL_2))
        assertTrue(cipher.apply(GameAction.CELL_9)) // still digit NINE in a legacy round
        assertTrue(cipher.apply(GameAction.PRIMARY))
        assertEquals(GamePhase.WON, cipher.render().phase)
        val stable = CipherVaultGame(Random(2))
        assertTrue(stable.restore(cipher.encode()))
        assertEquals(cipher.render(), stable.render())
        assertTrue(cipher.apply(GameAction.NEW_GAME))
        assertEquals(3, JSONObject(cipher.encode()).getString("secret").length)
        assertEquals(2, JSONObject(cipher.encode()).getInt("rules"))

        listOf<ArcadeGame>(neon, dungeon, heist, cipher).forEach { game ->
            val before = game.encode()
            assertEquals(2, JSONObject(before).getInt("v"))
            assertFalse(game.restore(JSONObject(before).put("v", 999).toString()))
            assertEquals(before, game.encode())
        }
    }

    @Test
    fun corruptedNewMechanicSnapshotsAreRejectedAtomically() {
        val fixtures = listOf(
            NeonTacticsGame() to ("score" to 10),
            PocketDungeonGame() to ("combatBeat" to 4),
            CyberHeistGame(Random(1)) to ("cachesSecured" to 1),
            CipherVaultGame(Random(1)) to ("historyCursor" to 2),
        )
        fixtures.forEach { (game, bad) ->
            val before = game.encode()
            assertFalse(game.restore(JSONObject(before).put(bad.first, bad.second).toString()))
            assertEquals(before, game.encode())
        }
    }

    private data class DeductionNode(val guess: String, val branches: Map<CipherVaultGame.ClueResult, DeductionNode>)

    private fun deductionTree(possible: List<String>): DeductionNode {
        val guess = possible.minWithOrNull(compareBy<String> { candidate ->
            possible.groupingBy { clueOracle(it, candidate) }.eachCount().values.maxOrNull()!!
        }.thenBy { it })!!
        val branches = possible.groupBy { clueOracle(it, guess) }
            .filterKeys { it.exact != guess.length }
            .mapValues { deductionTree(it.value) }
        return DeductionNode(guess, branches)
    }

    private fun clueOracle(secret: String, guess: String): CipherVaultGame.ClueResult {
        val exact = secret.indices.count { secret[it] == guess[it] }
        val overlap = secret.toSet().sumOf { digit -> minOf(secret.count { it == digit }, guess.count { it == digit }) }
        return CipherVaultGame.ClueResult(exact, overlap - exact)
    }

    private fun codes(alphabet: Int, length: Int): List<String> =
        if (length == 0) listOf("") else codes(alphabet, length - 1).flatMap { prefix -> (1..alphabet).map { prefix + it } }

    private fun stage(game: CipherVaultGame, code: String) {
        code.forEach { digit -> assertTrue(game.apply(GameAction.values()[GameAction.CELL_1.ordinal + (digit - '1')])) }
    }
}
