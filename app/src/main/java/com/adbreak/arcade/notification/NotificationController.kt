package com.adbreak.arcade.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
    private val appContext = context.applicationContext
    private val store = ArcadeStore(appContext)

    companion object {
        const val CHANNEL_ID = "arcade_controls"
        const val NOTIFICATION_ID = 7301
        const val EXTRA_ACTION = "action"
        const val EXTRA_GAME_ID = "game_id"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_RENDER_REVISION = "render_revision"
        const val ACTION_STOP = "STOP"
        const val ACTION_NO_OP = "NO_OP"

        private const val INTENT_ACTION = "com.adbreak.arcade.notification.ACTION"
        private const val REQUEST_CODE_NOTIFICATION_RESET = 7310
        private const val REQUEST_CODE_NOTIFICATION_PAUSE = 7311
        private const val REQUEST_CODE_NOTIFICATION_STOP = 7312
        private const val REQUEST_CODE_TOP_BASE = 7320
        private const val REQUEST_CODE_CELL_BASE = 7330
        private const val REQUEST_CODE_NO_OP_TOP_BASE = 7340
        private const val REQUEST_CODE_NO_OP_CELL_BASE = 7350
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, appContext.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                    description = appContext.getString(R.string.notification_channel_description)
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
            )
        }
    }

    fun canPost(): Boolean {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return false
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        return true
    }

    fun show(): Boolean {
        if (!canPost()) {
            stop()
            return false
        }
        val newSession = !store.active || store.sessionGameId != store.selectedGameId || store.sessionId == 0L
        if (newSession) {
            store.sessionId = maxOf(System.currentTimeMillis(), store.sessionId + 1L)
            store.sessionGameId = store.selectedGameId
            store.renderRevision = 0L
            store.sessionInteracted = false
            store.paused = false
        }
        store.active = true
        renderAndNotify()
        return store.active
    }

    fun stop() {
        store.active = false
        store.paused = false
        store.sessionInteracted = false
        store.sessionId += 1L
        store.renderRevision = 0L
        NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
    }

    fun handle(actionName: String?) = handle(actionName, null, null)

    fun handle(actionName: String?, actionGameId: String?, actionSessionId: Long?) {
        handle(actionName, actionGameId, actionSessionId, null)
    }

    fun handle(actionName: String?, actionGameId: String?, actionSessionId: Long?, actionRevision: Long?) {
        val hasEnvelope = actionGameId != null || actionSessionId != null || actionRevision != null
        if (hasEnvelope && (actionGameId != store.sessionGameId || actionSessionId != store.sessionId || actionRevision != null && actionRevision != store.renderRevision)) return
        if (actionName == ACTION_STOP) {
            stop()
            return
        }
        if (actionName.isNullOrBlank() || actionName == ACTION_NO_OP || !store.active) return
        val action = runCatching { GameAction.valueOf(actionName.orEmpty()) }.getOrNull() ?: return
        if (action == GameAction.PAUSE) { store.paused = true; renderAndNotify(); return }
        if (action == GameAction.RESUME) { store.paused = false; renderAndNotify(); return }
        if (store.paused && action != GameAction.RESET) return
        val game = store.loadGame(store.sessionGameId)
        if (game.apply(action)) {
            if (action == GameAction.RESET || action == GameAction.NEW_GAME) store.paused = false
            store.saveGame(game)
            if (action == GameAction.RESET || action == GameAction.NEW_GAME) {
                store.sessionInteracted = false
            } else {
                val roundStarted = !store.sessionInteracted
                store.recordGameAction(game.id, game.render().isTerminal, roundStarted)
                store.sessionInteracted = true
            }
        } else {
            return
        }
        renderAndNotify()
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun renderAndNotify() {
        if (!store.active) return
        if (!canPost()) {
            stop()
            return
        }
        val game = store.loadGame(store.sessionGameId)
        val state = game.render()
        store.renderRevision += 1L
        val gameOver = !store.paused && isGameOver(state)
        val effectiveState = when {
            store.paused -> state.copy(
                subtitle = appContext.getString(R.string.notification_paused_subtitle),
                buttons = emptyList(),
                buttonActions = emptyList(),
                cellButtons = emptyList(),
                cellActions = emptyList()
            )
            gameOver -> gameOverState(state)
            else -> state
        }
        val status = when {
            store.paused -> appContext.getString(R.string.notification_status_paused)
            gameOver -> appContext.getString(R.string.notification_status_game_over)
            state.feedback.equals("Ready", ignoreCase = true) -> appContext.getString(R.string.notification_status_ready)
            state.feedback.isNotBlank() -> state.feedback
            else -> appContext.getString(R.string.notification_status_playing)
        }
        val compactHint = when {
            store.paused -> appContext.getString(R.string.notification_hint_paused)
            gameOver -> appContext.getString(R.string.notification_hint_game_over)
            state.feedback.equals("Ready", ignoreCase = true) -> appContext.getString(R.string.notification_hint_ready)
            else -> appContext.getString(R.string.notification_hint_playing)
        }
        val expanded = buildExpandedViews(effectiveState, status)
        val compact = RemoteViews(appContext.packageName, R.layout.notification_game_compact).apply {
            setTextViewText(R.id.notificationCompactTitle, effectiveState.title)
            setTextViewText(R.id.notificationCompactStatus, status)
            setTextViewText(R.id.notificationCompactScore, effectiveState.scoreLine)
            setTextViewText(R.id.notificationCompactHint, compactHint)
            setViewVisibility(R.id.notificationCompactHint, if (compactHint.isBlank()) View.GONE else View.VISIBLE)
        }
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_arcade)
            .setContentTitle(appContext.getString(R.string.notification_title))
            .setCustomContentView(compact)
            .setCustomBigContentView(expanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_stat_arcade, if (gameOver) appContext.getString(R.string.notification_new_round) else appContext.getString(R.string.notification_reset), pending(GameAction.RESET, REQUEST_CODE_NOTIFICATION_RESET))
            .apply {
                if (!gameOver) {
                    addAction(
                        R.drawable.ic_stat_arcade,
                        if (store.paused) appContext.getString(R.string.notification_resume) else appContext.getString(R.string.notification_pause),
                        pending(if (store.paused) GameAction.RESUME else GameAction.PAUSE, REQUEST_CODE_NOTIFICATION_PAUSE)
                    )
                }
            }
            .addAction(R.drawable.ic_stat_arcade, appContext.getString(R.string.notification_stop), pendingString(ACTION_STOP, REQUEST_CODE_NOTIFICATION_STOP, bindSession = true))

        NotificationManagerCompat.from(appContext).notify(
            NOTIFICATION_ID,
            builder.build()
        )
    }

    private fun buildExpandedViews(state: GameViewState, status: String): RemoteViews = RemoteViews(appContext.packageName, R.layout.notification_game).apply {
        setTextViewText(R.id.notificationGameTitle, state.title)
        setTextViewText(R.id.notificationGameStatus, status)
        setTextViewText(R.id.notificationGameSubtitle, state.subtitle)
        setTextViewText(R.id.notificationGameScore, state.scoreLine)
        setViewVisibility(R.id.notificationGameSubtitle, if (state.cellButtons.isNotEmpty()) View.GONE else View.VISIBLE)

        val topIds = intArrayOf(R.id.notificationButton1, R.id.notificationButton2, R.id.notificationButton3, R.id.notificationButton4)
        topIds.forEachIndexed { index, id ->
            setTextViewText(id, "")
            setContentDescription(id, null)
            setViewVisibility(id, View.GONE)
            setOnClickPendingIntent(id, noOp(REQUEST_CODE_NO_OP_TOP_BASE + index))
        }
        state.buttons.take(4).forEachIndexed { index, label ->
            val id = topIds[index]
            val action = state.buttonActions.getOrNull(index)
            if (label.isNotBlank() && action != null) {
                setTextViewText(id, label)
                setContentDescription(id, label)
                setViewVisibility(id, View.VISIBLE)
                setOnClickPendingIntent(id, pending(action, REQUEST_CODE_TOP_BASE + index))
            }
        }
        setViewVisibility(R.id.notificationTopControls, if (state.buttons.take(4).indices.any { index -> state.buttons[index].isNotBlank() && state.buttonActions.getOrNull(index) != null }) View.VISIBLE else View.GONE)

        val cellIds = intArrayOf(R.id.notificationCell1, R.id.notificationCell2, R.id.notificationCell3, R.id.notificationCell4, R.id.notificationCell5, R.id.notificationCell6, R.id.notificationCell7, R.id.notificationCell8, R.id.notificationCell9)
        cellIds.forEachIndexed { index, id ->
            setTextViewText(id, "")
            setContentDescription(id, null)
            setViewVisibility(id, View.GONE)
            setOnClickPendingIntent(id, noOp(REQUEST_CODE_NO_OP_CELL_BASE + index))
        }
        state.cellButtons.take(9).forEachIndexed { index, label ->
            val id = cellIds[index]
            val action = state.cellActions.getOrNull(index)
            if (label.isNotBlank() && action != null) {
                setTextViewText(id, label)
                setContentDescription(id, cellContentDescription(index, label))
                setViewVisibility(id, View.VISIBLE)
                setOnClickPendingIntent(id, pending(action, REQUEST_CODE_CELL_BASE + index))
            }
        }
        setViewVisibility(R.id.notificationCellControls, if (state.cellButtons.take(9).indices.any { index -> state.cellButtons[index].isNotBlank() && state.cellActions.getOrNull(index) != null }) View.VISIBLE else View.GONE)

    }

    private fun isGameOver(state: GameViewState): Boolean {
        if (state.isTerminal) return true
        if (state.phase == com.adbreak.arcade.core.GamePhase.WON || state.phase == com.adbreak.arcade.core.GamePhase.LOST || state.phase == com.adbreak.arcade.core.GamePhase.DRAW) return true
        return state.buttonActions.any { it == GameAction.NEW_GAME }
    }

    private fun cellContentDescription(index: Int, label: String): String {
        if (label.startsWith("COL", ignoreCase = true)) return "Column ${index + 1}"
        val value = label.substringAfter(':', missingDelimiterValue = label).trim()
        val state = when (value) {
            "·" -> "empty"
            "X" -> "X"
            "O" -> "O"
            "ON" -> "light on"
            "0" -> "off"
            else -> value
        }
        return "Cell ${index + 1}, $state"
    }

    private fun gameOverState(state: GameViewState): GameViewState {
        val newGameIndex = state.buttonActions.indexOf(GameAction.NEW_GAME)
        return if (newGameIndex >= 0 && !state.buttons.getOrNull(newGameIndex).isNullOrBlank()) {
            state.copy(
                buttons = listOf(state.buttons[newGameIndex]),
                buttonActions = listOf(GameAction.NEW_GAME),
                cellButtons = emptyList(),
                cellActions = emptyList()
            )
        } else {
            state.copy(
                buttons = listOf(appContext.getString(R.string.notification_new_round)),
                buttonActions = listOf(GameAction.NEW_GAME),
                cellButtons = emptyList(),
                cellActions = emptyList()
            )
        }
    }

    private fun pending(action: GameAction, requestCode: Int): PendingIntent = pendingString(action.name, requestCode, bindSession = true)

    private fun noOp(requestCode: Int): PendingIntent = pendingString(ACTION_NO_OP, requestCode, bindSession = false)

    private fun pendingString(action: String, requestCode: Int, bindSession: Boolean): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        requestCode,
        Intent(appContext, NotificationActionReceiver::class.java).apply {
            setAction(INTENT_ACTION)
            setPackage(appContext.packageName)
            data = Uri.parse("adbreak://notification/$requestCode/${if (bindSession) store.sessionId else "noop"}/${store.renderRevision}")
            putExtra(EXTRA_ACTION, action)
            if (bindSession) {
                putExtra(EXTRA_GAME_ID, store.sessionGameId)
                putExtra(EXTRA_SESSION_ID, store.sessionId)
                putExtra(EXTRA_RENDER_REVISION, store.renderRevision)
            }
        },
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
