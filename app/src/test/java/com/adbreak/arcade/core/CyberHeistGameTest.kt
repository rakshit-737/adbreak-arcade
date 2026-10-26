package com.adbreak.arcade.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class CyberHeistGameTest {
    @Test
    fun seededCampaignsExposeStableNotificationSafeStages() {
        val first = CyberHeistGame(Random(41))
        val second = CyberHeistGame(Random(41))

        assertEquals(first.render(), second.render())
        assertEquals(first.encode(), second.encode())
        assertEquals("cyber_heist", first.id)
        assertEquals(4, first.render().buttonActions.size)
        assertEquals(
            listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2),
            first.render().buttonActions,
        )
        assertTrue(first.render().subtitle.contains("Mission 1/3"))
        assertTrue(first.render().scoreLine.contains("Security 0/"))
    }

    @Test
    fun safeStagedRouteAdvancesEveryMissionAndWinsCampaign() {
        val game = CyberHeistGame(Random(7))

        repeat(CyberHeistGame.TOTAL_MISSIONS) {
            assertTrue(game.apply(GameAction.PRIMARY)) // passive scan
            assertTrue(game.apply(GameAction.CELL_1)) // social loop
            assertTrue(game.apply(GameAction.PRIMARY)) // clean exit
        }

        assertEquals(CyberHeistGame.TOTAL_MISSIONS, game.missionsCompleted)
        assertEquals(GamePhase.WON, game.render().phase)
        assertTrue(game.render().isTerminal)
        assertTrue(game.render().feedback.contains("Campaign clear"))
        assertEquals(listOf("REPLAY"), game.render().buttons)
        assertFalse(game.apply(GameAction.PRIMARY))
        assertTrue(game.apply(GameAction.NEW_GAME))
        assertEquals(0, game.missionsCompleted)
        assertEquals(0, game.stageIndex)
        assertFalse(game.render().isTerminal)
    }

    @Test
    fun pauseInvalidAndUnaffordableActionsAreSafe() {
        val game = CyberHeistGame(Random(12))
        val initial = game.encode()

        assertFalse(game.apply(GameAction.CELL_9))
        assertEquals(initial, game.encode())
        assertTrue(game.apply(GameAction.PAUSE))
        val paused = game.encode()
        assertEquals(GamePhase.PAUSED, game.render().phase)
        assertFalse(game.apply(GameAction.PRIMARY))
        assertEquals(paused, game.encode())
        assertTrue(game.apply(GameAction.RESUME))
        assertFalse(game.apply(GameAction.RESUME))

        val exhausted = JSONObject(game.encode())
            .put("stageIndex", 1)
            .put("stealth", 0)
            .put("tools", 0)
            .put("intel", 0)
        assertFalse(game.restore(exhausted.toString()))
        assertEquals(GamePhase.PLAYING, game.render().phase)
    }

    @Test
    fun securityLockdownAndResourceExhaustionAreTerminal() {
        val securityGame = CyberHeistGame(Random(18))
        val securityState = JSONObject(securityGame.encode())
        val limit = securityState.getJSONArray("missions").getJSONObject(0).getInt("securityLimit")
        securityState.put("security", limit - 2).put("stageIndex", 1).put("tools", 1).put("stealth", 0).put("intel", 1)
        assertTrue(securityGame.restore(securityState.toString()))
        assertTrue(securityGame.apply(GameAction.CELL_2)) // brute force: +3 security
        assertEquals(GamePhase.LOST, securityGame.render().phase)
        assertTrue(securityGame.render().feedback.contains("Security lockdown"))
        assertFalse(securityGame.apply(GameAction.CELL_2))

        val resourceGame = CyberHeistGame(Random(19))
        val resourceState = JSONObject(resourceGame.encode())
            .put("stageIndex", 1)
            .put("stealth", 0)
            .put("tools", 1)
            .put("intel", 0)
        assertTrue(resourceGame.restore(resourceState.toString()))
        assertTrue(resourceGame.apply(GameAction.CELL_2)) // leaves no affordable extract action
        assertEquals(GamePhase.LOST, resourceGame.render().phase)
        assertTrue(resourceGame.render().feedback.contains("Resources exhausted"))
    }

    @Test
    fun snapshotsRoundTripAndRejectMalformedStateAtomically() {
        val source = CyberHeistGame(Random(23))
        assertTrue(source.apply(GameAction.PRIMARY))
        val encoded = source.encode()

        val restored = CyberHeistGame(Random(99))
        assertTrue(restored.restore(encoded))
        assertEquals(source.render(), restored.render())
        assertEquals(encoded, restored.encode())

        val before = restored.render()
        assertFalse(restored.restore("not json"))
        assertFalse(restored.restore(JSONObject(encoded).put("tools", -1).toString()))
        assertFalse(restored.restore(JSONObject(encoded).put("stageIndex", CyberHeistGame.STAGES_PER_MISSION).toString()))
        assertEquals(before, restored.render())
    }
}
