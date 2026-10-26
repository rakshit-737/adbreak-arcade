package com.adbreak.arcade.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.json.JSONObject

class GameStatsTest {
    @Test
    fun statsRoundTripPreservesProgressionCounters() {
        val source = GameStats(plays = 2, actions = 11, roundsCompleted = 1)
        assertEquals(source, GameStats.decode(source.encode()))
    }

    @Test
    fun malformedStatsFallBackToEmpty() {
        assertEquals(GameStats(), GameStats.decode("not-json"))
    }

    @Test
    fun futureOrInconsistentStatsFallBackToEmpty() {
        assertEquals(GameStats(), GameStats.decode(JSONObject().apply {
            put("v", 99)
            put("plays", 1)
            put("actions", 1)
            put("roundsCompleted", 1)
        }.toString()))
        assertEquals(GameStats(), GameStats.decode(JSONObject().apply {
            put("v", 1)
            put("plays", 0)
            put("actions", 1)
            put("roundsCompleted", 2)
        }.toString()))
    }
}
