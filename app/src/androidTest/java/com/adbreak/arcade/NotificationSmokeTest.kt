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
        val first = manager.activeNotifications.single { it.id == NotificationController.NOTIFICATION_ID }
        val firstActions = first.notification.actions ?: emptyArray()
        assertEquals(3, firstActions.size)
        assertTrue(firstActions.all { it.actionIntent.isImmutable })
        assertNotEquals(firstActions[0].actionIntent, firstActions[1].actionIntent)
        assertNotEquals(firstActions[1].actionIntent, firstActions[2].actionIntent)

        assertTrue(controller.show())
        assertEquals(1, manager.activeNotifications.count { it.id == NotificationController.NOTIFICATION_ID })
        assertEquals(3, manager.activeNotifications.single { it.id == NotificationController.NOTIFICATION_ID }.notification.actions?.size)
    }

    @Test
    fun staleActionAfterStopCannotChangeSavedNotificationState() {
        val controller = NotificationController(context)
        val store = ArcadeStore(context)
        controller.stop()

        controller.handle(GameAction.PAUSE.name)

        assertFalse(store.active)
        assertFalse(store.paused)
    }

    @Test
    fun resetWhilePausedResumesTheActiveGame() {
        val controller = NotificationController(context)
        assumeTrue("Notification permission or channel is unavailable", controller.canPost())
        val store = ArcadeStore(context)

        assertTrue(controller.show())
        controller.handle(GameAction.PAUSE.name)
        assertTrue(store.paused)

        controller.handle(GameAction.RESET.name)

        assertTrue(store.active)
        assertFalse(store.paused)
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
