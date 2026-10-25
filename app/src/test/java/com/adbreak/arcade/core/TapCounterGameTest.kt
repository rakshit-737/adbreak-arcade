package com.adbreak.arcade.core

import org.junit.Assert.*
import org.junit.Test

class TapCounterGameTest {
    @Test fun tappingIncrementsAndResetClears() { val game = TapCounterGame(); assertTrue(game.apply(GameAction.PRIMARY)); assertTrue(game.apply(GameAction.PRIMARY)); assertEquals(2, game.score); assertEquals(2, game.best); game.apply(GameAction.RESET); assertEquals(0, game.score); assertEquals(2, game.best) }
    @Test fun stateRoundTrips() { val source = TapCounterGame(); source.apply(GameAction.PRIMARY); source.apply(GameAction.PRIMARY); val restored = TapCounterGame(); assertTrue(restored.restore(source.encode())); assertEquals(source.score, restored.score); assertEquals(source.best, restored.best) }
    @Test fun malformedStateIsRejected() { assertFalse(TapCounterGame().restore("not-json")) }
}
