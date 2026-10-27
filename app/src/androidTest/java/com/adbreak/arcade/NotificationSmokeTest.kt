package com.adbreak.arcade

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adbreak.arcade.core.GameAction
import com.adbreak.arcade.core.HigherLowerGame
import com.adbreak.arcade.data.ArcadeStore
import com.adbreak.arcade.notification.NotificationController
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Random

@RunWith(AndroidJUnit4::class)
class NotificationSmokeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun isolatedState() {
        NotificationController(context).stop()
        context.getSharedPreferences("arcade_state", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun cleanUp() {
        NotificationController(context).stop()
        ArcadeStore(context).apply {
            selectedGameId = "tap_counter"
            active = false
            paused = false
        }
    }

    @Test
    fun notificationControllerCreatesStableQuietChannelWithoutCrash() {
        val controller = NotificationController(context)
        controller.ensureChannel()
        controller.ensureChannel()

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(NotificationController.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel?.importance)
        assertFalse(channel?.shouldVibrate() ?: true)
        assertEquals(null, channel?.sound)
    }

    @Test
    fun showIsIdempotentAndUsesOneStableNotificationWithImmutableActions() {
        val controller = NotificationController(context)
        assumeTrue("Notification permission or channel is unavailable", controller.canPost())
        val manager = context.getSystemService(NotificationManager::class.java)

        assertTrue(controller.show())
        val revision = ArcadeStore(context).renderRevision
        val first = manager.activeNotifications.single { it.id == NotificationController.NOTIFICATION_ID }
        val firstActions = first.notification.actions ?: emptyArray()
        assertEquals(3, firstActions.size)
        assertTrue(firstActions.all { it.actionIntent.isImmutable })
        assertNotEquals(firstActions[0].actionIntent, firstActions[1].actionIntent)
        assertNotEquals(firstActions[1].actionIntent, firstActions[2].actionIntent)

        assertTrue(controller.show())
        assertEquals(revision, ArcadeStore(context).renderRevision)
        assertEquals(1, manager.activeNotifications.count { it.id == NotificationController.NOTIFICATION_ID })
        assertEquals(3, manager.activeNotifications.single { it.id == NotificationController.NOTIFICATION_ID }.notification.actions?.size)
    }

    @Test fun firstRandomizedPuzzleIsSavedBeforeItsFirstAction() {
        val store = ArcadeStore(context)
        com.adbreak.arcade.core.GameRegistry.definitions.forEach { definition ->
            val initial = store.loadGame(definition.id)
            val restored = ArcadeStore(context).loadGame(definition.id)
            assertEquals(definition.id, initial.render(), restored.render())
            assertEquals(definition.id, initial.encode(), restored.encode())
        }
    }

    @Test fun duplicateAndStaleActionsCannotChangeStateStatsOrRecordsAcrossControllers() {
        val controller = NotificationController(context)
        assumeTrue(controller.canPost())
        val store = ArcadeStore(context)
        store.select("tap_counter")
        assertTrue(controller.show())
        val gameId = store.sessionGameId
        val session = store.sessionId
        val revision = store.renderRevision
        controller.handle(GameAction.PRIMARY.name, gameId, session, revision)
        val after = store.loadGame(gameId).encode()
        val stats = store.statsFor(gameId)
        val xp = store.totalXp()
        val record = store.recordFor(gameId)
        assertEquals(revision + 1, store.renderRevision)
        NotificationController(context).handle(GameAction.PRIMARY.name, gameId, session, revision)
        assertEquals(after, store.loadGame(gameId).encode())
        assertEquals(stats, store.statsFor(gameId))
        assertEquals(xp, store.totalXp())
        assertEquals(record, store.recordFor(gameId))
        controller.stop()
        assertTrue(controller.show())
        controller.handle(GameAction.PRIMARY.name, gameId, session, store.renderRevision)
        assertEquals(after, store.loadGame(gameId).encode())
    }

    @Test fun acceptedMoveAndRevisionAreCommittedTogetherBeforeAnyNotificationRefresh() {
        val store = ArcadeStore(context)
        store.select("tap_counter")
        assertTrue(store.beginSession())
        val game = store.loadGame()
        assertTrue(game.apply(GameAction.PRIMARY))
        val token = "${store.sessionId}:${store.renderRevision}:${store.sessionGameId}"
        assertTrue(store.saveAcceptedAction(game, game.render().isTerminal, true, token))
        val restoredStore = ArcadeStore(context)
        assertEquals(1L, restoredStore.renderRevision)
        assertEquals(game.render(), restoredStore.loadGame().render())
        assertFalse(restoredStore.saveAcceptedAction(game, game.render().isTerminal, true, token))
        assertEquals(1, restoredStore.statsFor(game.id).actions)
    }

    @Test fun recordsSurviveReplayAndOnlyClearWithExplicitHistoryRemoval() {
        val store = ArcadeStore(context)
        store.select("tap_counter")
        assertTrue(store.beginSession())
        val realGame = store.loadGame()
        // Exercise the shared record transaction independently of any game's scoring policy.
        val recorded = object : com.adbreak.arcade.core.ArcadeGame by realGame {
            override fun render() = realGame.render().copy(record = com.adbreak.arcade.core.GameRecord(40, "Run score"))
        }
        val token = "${store.sessionId}:${store.renderRevision}:${store.sessionGameId}"
        assertTrue(store.saveAcceptedAction(recorded, false, true, token))
        assertEquals(40, ArcadeStore(context).recordFor(realGame.id)?.value)
        realGame.reset()
        assertTrue(store.saveRoundReset(realGame, "${store.sessionId}:${store.renderRevision}:${store.sessionGameId}"))
        assertEquals(40, store.recordFor(realGame.id)?.value)
        store.resetGame(realGame.id)
        assertEquals(40, store.recordFor(realGame.id)?.value)
        store.clearGameHistory(realGame.id)
        assertEquals(null, store.recordFor(realGame.id))
    }

    @Test fun unreadableSnapshotsRecoverHonestlyWithoutDiscardingOriginalBytes() {
        val prefs = context.getSharedPreferences("arcade_state", Context.MODE_PRIVATE)
        prefs.edit().putString("state_tap_counter", "{future-format}").commit()
        val store = ArcadeStore(context)
        assertTrue(store.beginSession())
        assertTrue(store.sessionNotice.contains("unreadable"))
        assertEquals("{future-format}", prefs.getString("unreadable_state_tap_counter", null))
        val game = store.loadGame()
        assertEquals(game.render(), ArcadeStore(context).loadGame().render())
    }

    @Test fun boardMenuAndRowSelectionNeverAdvanceGameplayOrProgression() {
        val controller = NotificationController(context)
        assumeTrue(controller.canPost())
        val store = ArcadeStore(context)
        store.select("neon_tactics")
        assertTrue(controller.show())
        val before = store.loadGame().encode()
        controller.handle(NotificationController.ACTION_MENU, store.sessionGameId, store.sessionId, store.renderRevision)
        assertTrue(store.controlsMenu)
        controller.handle("CONTROLS_ROW_2", store.sessionGameId, store.sessionId, store.renderRevision)
        assertFalse(store.controlsMenu)
        assertEquals(2, store.cellPage)
        assertEquals(before, store.loadGame().encode())
        assertEquals(0, store.statsFor("neon_tactics").actions)
        assertEquals(0, store.totalXp())
    }

    @Test fun allRegisteredNotificationsUseInflatableRemoteViewsAndCurrentObjectives() {
        val controller = NotificationController(context)
        assumeTrue(controller.canPost())
        val store = ArcadeStore(context)
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        com.adbreak.arcade.core.GameRegistry.definitions.forEach { game ->
            store.select(game.id)
            assertTrue(game.id, controller.show())
            val posted = context.getSystemService(NotificationManager::class.java).activeNotifications
                .single { it.id == NotificationController.NOTIFICATION_ID }.notification
            instrumentation.runOnMainSync {
                val parent = android.widget.FrameLayout(context)
                val expanded = posted.bigContentView.apply(context, parent)
                val compact = posted.contentView.apply(context, parent)
                assertEquals(game.title, expanded.findViewById<android.widget.TextView>(R.id.notificationGameTitle).text.toString())
                assertTrue(compact.findViewById<android.widget.TextView>(R.id.notificationCompactScore).text.isNotBlank())
                assertNotNull(expanded.findViewById<android.view.View>(R.id.notificationGameProgress))
            }
        }
    }

    @Test
    fun staleActionAfterStopCannotChangeSavedNotificationState() {
        val controller = NotificationController(context)
        val store = ArcadeStore(context)
        controller.stop()

        controller.handle(GameAction.PAUSE.name, store.sessionGameId, store.sessionId, store.renderRevision)

        assertFalse(store.active)
        assertFalse(store.paused)
    }

    @Test
    fun resetWhilePausedResumesTheActiveGame() {
        val controller = NotificationController(context)
        assumeTrue("Notification permission or channel is unavailable", controller.canPost())
        val store = ArcadeStore(context)

        assertTrue(controller.show())
        controller.handle(GameAction.PAUSE.name, store.sessionGameId, store.sessionId, store.renderRevision)
        assertTrue(store.paused)

        controller.handle(GameAction.RESET.name, store.sessionGameId, store.sessionId, store.renderRevision)

        assertTrue(store.active)
        assertFalse(store.paused)
    }

    @Test
    fun gameplayWithoutACompleteEnvelopeIsRejected() {
        val controller = NotificationController(context)
        assumeTrue("Notification permission or channel is unavailable", controller.canPost())
        val store = ArcadeStore(context)
        assertTrue(controller.show())
        val before = store.loadGame(store.sessionGameId).encode()
        controller.handle(GameAction.PRIMARY.name)
        assertEquals(before, store.loadGame(store.sessionGameId).encode())
    }

    @Test
    fun terminalStateRemovesPauseActionAndKeepsNewRoundAction() {
        val controller = NotificationController(context)
        assumeTrue("Notification permission or channel is unavailable", controller.canPost())
        val store = ArcadeStore(context)
        store.selectedGameId = "higher_lower"
        val terminalGame = HigherLowerGame(Random(1))
        val terminalState = JSONObject().apply {
            put("v", 1)
            put("low", 1)
            put("high", 20)
            put("secret", 20)
            put("attempts", 1)
            put("message", "Guess 1–20")
            put("roundEnded", false)
            put("wins", 0)
            put("bestStreak", 0)
            put("streak", 0)
        }.toString()
        assertTrue(terminalGame.restore(terminalState))
        assertTrue(terminalGame.apply(GameAction.SECONDARY))
        store.saveGame(terminalGame)

        assertTrue(controller.show())
        val notification = context.getSystemService(NotificationManager::class.java)
            .activeNotifications.single { it.id == NotificationController.NOTIFICATION_ID }

        assertEquals(2, notification.notification.actions?.size)
    }
}
