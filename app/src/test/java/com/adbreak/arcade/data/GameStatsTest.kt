package com.adbreak.arcade.data

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
