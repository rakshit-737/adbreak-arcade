package com.adbreak.arcade.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.adbreak.arcade.R
import com.adbreak.arcade.core.GameAction
import com.adbreak.arcade.core.GameViewState
import com.adbreak.arcade.data.ArcadeStore

class NotificationController(private val context: Context) {
    private val store = ArcadeStore(context)

    companion object {
        const val CHANNEL_ID = "arcade_controls"
        const val NOTIFICATION_ID = 7301
        const val EXTRA_ACTION = "action"
        const val ACTION_STOP = "STOP"
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                    description = context.getString(R.string.notification_channel_description)
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
    }

    fun canPost(): Boolean {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        return true
    }

    fun show(): Boolean {
        if (!canPost()) { store.active = false; return false }
        store.active = true
        store.paused = false
        store.saveGame(store.loadGame())
        renderAndNotify()
        return true
    }

    fun stop() {
        store.active = false
        store.paused = false
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun handle(actionName: String?) {
        if (actionName == ACTION_STOP) { stop(); return }
        val action = runCatching { GameAction.valueOf(actionName.orEmpty()) }.getOrNull() ?: return
        if (action == GameAction.PAUSE) { store.paused = true; if (store.active) renderAndNotify(); return }
        if (action == GameAction.RESUME) { store.paused = false; if (store.active) renderAndNotify(); return }
        if (store.paused && action != GameAction.RESET) return
        val game = store.loadGame()
        if (game.apply(action)) {
            if (action == GameAction.RESET || action == GameAction.NEW_GAME) store.paused = false
            store.saveGame(game)
        }
        if (store.active) renderAndNotify()
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun renderAndNotify() {
        if (!canPost()) { store.active = false; return }
        val game = store.loadGame()
        val state = game.render()
        val effectiveState = if (store.paused) state.copy(
            subtitle = "Paused — resume when you are ready",
            buttons = emptyList(),
            buttonActions = emptyList(),
            cellButtons = emptyList(),
            cellActions = emptyList()
        ) else state
        val expanded = buildExpandedViews(effectiveState)
        val compact = RemoteViews(context.packageName, R.layout.notification_game_compact).apply {
            setTextViewText(R.id.notificationCompactTitle, effectiveState.title)
            setTextViewText(R.id.notificationCompactScore, effectiveState.scoreLine)
            setTextViewText(R.id.notificationCompactHint, if (store.paused) "Paused" else "Expand to play")
        }
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_arcade)
                .setContentTitle(context.getString(R.string.notification_title))
                .setCustomContentView(compact)
                .setCustomBigContentView(expanded)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .addAction(R.drawable.ic_stat_arcade, context.getString(R.string.notification_reset), pending(GameAction.RESET))
                .addAction(R.drawable.ic_stat_arcade, if (store.paused) context.getString(R.string.notification_resume) else context.getString(R.string.notification_pause), pending(if (store.paused) GameAction.RESUME else GameAction.PAUSE))
                .addAction(R.drawable.ic_stat_arcade, context.getString(R.string.notification_stop), pendingString(ACTION_STOP))
                .setOngoing(true)
                .setSilent(true)
                .setAutoCancel(false)
                .build()
        )
    }

    private fun buildExpandedViews(state: GameViewState): RemoteViews = RemoteViews(context.packageName, R.layout.notification_game).apply {
        setTextViewText(R.id.notificationGameTitle, state.title)
        setTextViewText(R.id.notificationGameSubtitle, state.subtitle)
        setTextViewText(R.id.notificationGameScore, state.scoreLine)

        val topIds = intArrayOf(R.id.notificationButton1, R.id.notificationButton2, R.id.notificationButton3, R.id.notificationButton4)
        topIds.forEach { id -> setTextViewText(id, ""); setViewVisibility(id, View.GONE) }
        state.buttons.take(4).forEachIndexed { index, label ->
            val id = topIds[index]
            setTextViewText(id, label)
            setViewVisibility(id, if (label.isBlank()) View.GONE else View.VISIBLE)
            state.buttonActions.getOrNull(index)?.let { setOnClickPendingIntent(id, pending(it)) }
        }
        setViewVisibility(R.id.notificationTopControls, if (state.buttons.any { it.isNotBlank() }) View.VISIBLE else View.GONE)

        val cellIds = intArrayOf(R.id.notificationCell1, R.id.notificationCell2, R.id.notificationCell3, R.id.notificationCell4, R.id.notificationCell5, R.id.notificationCell6, R.id.notificationCell7, R.id.notificationCell8, R.id.notificationCell9)
        cellIds.forEach { id -> setTextViewText(id, ""); setViewVisibility(id, View.GONE) }
        state.cellButtons.take(9).forEachIndexed { index, label ->
            val id = cellIds[index]
            setTextViewText(id, label)
            setViewVisibility(id, if (label.isBlank()) View.GONE else View.VISIBLE)
            state.cellActions.getOrNull(index)?.let { setOnClickPendingIntent(id, pending(it)) }
        }
        setViewVisibility(R.id.notificationCellControls, if (state.cellButtons.any { it.isNotBlank() }) View.VISIBLE else View.GONE)

    }

    private fun pending(action: GameAction): PendingIntent = PendingIntent.getBroadcast(
        context, action.ordinal, Intent(context, NotificationActionReceiver::class.java).putExtra(EXTRA_ACTION, action.name), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun pendingString(action: String): PendingIntent = PendingIntent.getBroadcast(
        context, 9000, Intent(context, NotificationActionReceiver::class.java).putExtra(EXTRA_ACTION, action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
