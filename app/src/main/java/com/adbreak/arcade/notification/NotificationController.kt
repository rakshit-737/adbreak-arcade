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
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.adbreak.arcade.R
import com.adbreak.arcade.core.GameAction
import com.adbreak.arcade.core.GameViewState
import com.adbreak.arcade.data.ArcadeStore
import com.adbreak.arcade.ui.game.GamePresentationCatalog

class NotificationController(private val context: Context) {
    private val appContext = context.applicationContext
    private val store = ArcadeStore(appContext)
    private val actionLock = ArcadeStore.sessionLock

    companion object {
        const val CHANNEL_ID = "arcade_controls"
        const val NOTIFICATION_ID = 7301
        const val EXTRA_ACTION = "action"
        const val EXTRA_GAME_ID = "game_id"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_RENDER_REVISION = "render_revision"
        const val ACTION_STOP = "STOP"
        const val ACTION_NO_OP = "NO_OP"
        const val ACTION_MENU = "CONTROLS_MENU"
        const val ACTION_BACK = "CONTROLS_BACK"
        private const val ACTION_ROW_PREFIX = "CONTROLS_ROW_"
        private var lastPostedKey: String? = null

        private const val INTENT_ACTION = "com.adbreak.arcade.notification.ACTION"
        private const val REQUEST_CODE_NOTIFICATION_RESET = 7310
        private const val REQUEST_CODE_NOTIFICATION_PAUSE = 7311
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

    fun show(): Boolean = synchronized(actionLock) {
        if (!canPost()) {
            stop()
            return@synchronized false
        }
        val newSession = !store.active || store.sessionGameId != store.selectedGameId || store.sessionId == 0L
        if (newSession && !store.beginSession()) return@synchronized false
        renderAndNotify()
        store.active
    }

    fun stop() = synchronized(actionLock) {
        store.endSession()
        lastPostedKey = null
        NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
    }

    fun handle(actionName: String?) = handle(actionName, null, null, null)

    fun handle(actionName: String?, actionGameId: String?, actionSessionId: Long?) {
        handle(actionName, actionGameId, actionSessionId, null)
    }

    fun handle(actionName: String?, actionGameId: String?, actionSessionId: Long?, actionRevision: Long?) = synchronized(actionLock) {
        if (actionName.isNullOrBlank() || actionName == ACTION_NO_OP) return@synchronized
        val envelope = ActionEnvelope(store.sessionGameId, store.sessionId, store.renderRevision)
        if (!store.active || !envelope.matches(actionGameId, actionSessionId, actionRevision)) return@synchronized
        if (actionName == ACTION_STOP) {
            stop()
            return@synchronized
        }
        if (actionName == ACTION_MENU || actionName == ACTION_BACK) {
            if (store.saveControlsPage(actionName == ACTION_MENU)) renderAndNotify()
            return@synchronized
        }
        if (actionName.startsWith(ACTION_ROW_PREFIX) && store.controlsMenu) {
            val row = actionName.removePrefix(ACTION_ROW_PREFIX).toIntOrNull() ?: return@synchronized
            if (row in 0..2 && store.saveControlsPage(false, row)) renderAndNotify()
            return@synchronized
        }
        val action = runCatching { GameAction.valueOf(actionName) }.getOrNull() ?: return@synchronized
        val token = envelope.token
        if (action == GameAction.PAUSE) {
            if (!store.paused && store.saveSessionAction(true, token)) renderAndNotify()
            return@synchronized
        }
        if (action == GameAction.RESUME) {
            if (store.paused && store.saveSessionAction(false, token)) renderAndNotify()
            return@synchronized
        }
        if (store.paused && action != GameAction.RESET) return@synchronized
        val game = store.loadGame(store.sessionGameId)
        val previous = game.render()
        if (action in previous.disabledActions) return@synchronized
        if (!game.apply(action)) return@synchronized
        val terminal = game.render().isTerminal
        val saved = if (action == GameAction.RESET || action == GameAction.NEW_GAME || previous.isTerminal && !terminal) {
            store.saveRoundReset(game, token)
        } else {
            store.saveAcceptedAction(game, terminal, !store.sessionInteracted, token)
        }
        if (saved) renderAndNotify()
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun renderAndNotify() = synchronized(actionLock) {
        if (!store.active) return@synchronized
        if (!canPost()) {
            stop()
            return@synchronized
        }
        val game = store.loadGame(store.sessionGameId)
        val state = game.render()
        val key = "${store.sessionId}:${store.renderRevision}:${appContext.resources.configuration}:$state"
        val manager = appContext.getSystemService(NotificationManager::class.java)
        if (key == lastPostedKey && manager?.activeNotifications?.any { it.id == NOTIFICATION_ID } == true) return@synchronized
        val gameOver = !store.paused && isGameOver(state)
        val effectiveState = when {
            store.paused -> state.copy(
                subtitle = appContext.getString(R.string.notification_paused_subtitle),
                buttons = emptyList(),
                buttonActions = emptyList(),
                cellButtons = emptyList(),
                cellActions = emptyList()
            )
            gameOver -> NotificationSurface.terminalState(state)
            else -> state
        }
        val status = listOf(store.sessionNotice, state.feedback).filter { it.isNotBlank() }.joinToString(" · ")
        val expanded = buildExpandedViews(effectiveState, status)
        val compactStatus = NotificationSurface.phaseLabel(state, store.paused)
        val compact = RemoteViews(appContext.packageName, R.layout.notification_game_compact).apply {
            setTextViewText(R.id.notificationCompactTitle, effectiveState.title)
            setTextViewText(R.id.notificationCompactStatus, compactStatus)
            setTextViewText(R.id.notificationCompactScore, NotificationSurface.compactSummary(state, store.paused, store.sessionNotice))
            setContentDescription(R.id.notificationCompactScore, "${state.subtitle}. ${state.scoreLine}. $status. Expand to play.")
        }
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_arcade)
            .setContentTitle(appContext.getString(R.string.notification_title))
            .setContentText(state.subtitle)
            .setCustomContentView(compact)
            .setCustomBigContentView(expanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(false)
            .apply {
                if (store.controlsMenu) {
                    addAction(R.drawable.ic_stat_arcade, "Restart", pending(GameAction.RESET, REQUEST_CODE_NOTIFICATION_RESET))
                    if (!gameOver) addAction(R.drawable.ic_stat_arcade, if (store.paused) "Resume" else "Pause", pending(if (store.paused) GameAction.RESUME else GameAction.PAUSE, REQUEST_CODE_NOTIFICATION_PAUSE))
                    addAction(R.drawable.ic_stat_arcade, "Back to game", pendingString(ACTION_BACK, 7360, true))
                } else if (NotificationSurface.usesBoardActions(effectiveState)) {
                    effectiveState.buttons.forEachIndexed { index, label ->
                        val action = effectiveState.buttonActions[index]
                        if (action !in effectiveState.disabledActions) addAction(R.drawable.ic_stat_arcade, label, pending(action, REQUEST_CODE_TOP_BASE + index))
                    }
                    addAction(R.drawable.ic_stat_arcade, "Menu / details", pendingString(ACTION_MENU, 7360, true))
                } else {
                    addAction(R.drawable.ic_stat_arcade, if (gameOver) "Replay" else "Restart", pending(if (gameOver) GameAction.NEW_GAME else GameAction.RESET, REQUEST_CODE_NOTIFICATION_RESET))
                    if (!gameOver) {
                        addAction(
                            R.drawable.ic_stat_arcade,
                            if (store.paused) appContext.getString(R.string.notification_resume) else appContext.getString(R.string.notification_pause),
                            pending(if (store.paused) GameAction.RESUME else GameAction.PAUSE, REQUEST_CODE_NOTIFICATION_PAUSE)
                        )
                    }
                    addAction(R.drawable.ic_stat_arcade, "Menu / details", pendingString(ACTION_MENU, 7360, true))
                }
            }

        NotificationManagerCompat.from(appContext).notify(
            NOTIFICATION_ID,
            builder.build()
        )
        lastPostedKey = key
    }

    private fun buildExpandedViews(state: GameViewState, status: String): RemoteViews = RemoteViews(appContext.packageName, R.layout.notification_game).apply {
        val board = NotificationSurface.usesBoardActions(state)
        val menu = store.controlsMenu
        val largeText = appContext.resources.configuration.fontScale >= 1.3f
        val cells = NotificationSurface.visibleCells(state, largeText, store.cellPage)
        val family = GamePresentationCatalog.forGame(store.sessionGameId).family
        setTextViewText(R.id.notificationGameTitle, state.title)
        setTextViewText(R.id.notificationGameStatus, status)
        setTextColor(R.id.notificationGameStatus, appContext.getColor(when (state.phase) {
            com.adbreak.arcade.core.GamePhase.WON -> R.color.notification_success
            com.adbreak.arcade.core.GamePhase.LOST -> R.color.notification_error
            else -> family.accent
        }))
        val subtitle = if (menu) "${state.subtitle}\n${state.progress?.let { "${it.label}: ${it.current}/${it.maximum}" }.orEmpty()}".trim() else state.subtitle
        setTextViewText(R.id.notificationGameSubtitle, subtitle)
        val score = if (board && largeText && !menu) "Cells ${cells.first + 1}–${cells.last + 1} · Menu for other rows" else state.scoreLine
        setTextViewText(R.id.notificationGameScore, score)
        setContentDescription(R.id.notificationGameSubtitle, state.subtitle)
        setContentDescription(R.id.notificationGameStatus, status)
        setContentDescription(R.id.notificationGameScore, state.scoreLine)
        setInt(R.id.notificationGameSubtitle, "setMaxLines", if (menu) 4 else if (largeText) 1 else 2)
        setInt(R.id.notificationGameStatus, "setMaxLines", if (menu || !board && !largeText) 2 else 1)
        setInt(R.id.notificationGameScore, "setMaxLines", if (menu || !board && !largeText) 2 else 1)
        setTextViewTextSize(R.id.notificationGameScore, TypedValue.COMPLEX_UNIT_SP, if (board) 12f else 14f)
        setViewVisibility(R.id.notificationGameSubtitle, if (state.subtitle.isBlank()) View.GONE else View.VISIBLE)
        val progress = state.progress?.takeIf { it.maximum > 0 && it.current in 0..it.maximum }
        setViewVisibility(R.id.notificationGameProgress, if (progress != null && !menu) View.VISIBLE else View.GONE)
        if (progress != null) {
            setProgressBar(R.id.notificationGameProgress, progress.maximum, progress.current, false)
            setContentDescription(R.id.notificationGameProgress, "${progress.label}: ${progress.current} of ${progress.maximum}")
        }

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
            if (label.isNotBlank() && action != null && !board && !menu) {
                val reason = state.disabledActions[action]
                setTextViewText(id, label)
                setContentDescription(id, if (reason == null) label else "$label, unavailable: $reason")
                setBoolean(id, "setEnabled", reason == null)
                setTextColor(id, appContext.getColor(if (reason == null) R.color.notification_accent else R.color.notification_secondary_text))
                setViewVisibility(id, View.VISIBLE)
                setOnClickPendingIntent(id, if (reason == null) pending(action, REQUEST_CODE_TOP_BASE + index) else noOp(REQUEST_CODE_NO_OP_TOP_BASE + index))
            }
        }
        if (menu) {
            val menuActions = buildList {
                if (board && largeText) repeat((state.cellButtons.size + 2) / 3) { row ->
                    add("Cells ${row * 3 + 1}–${minOf(row * 3 + 3, state.cellButtons.size)}" to "$ACTION_ROW_PREFIX$row")
                }
                add("Stop game" to ACTION_STOP)
            }
            menuActions.forEachIndexed { index, (label, action) ->
                setTextViewText(topIds[index], label)
                setContentDescription(topIds[index], label)
                setBoolean(topIds[index], "setEnabled", true)
                setViewVisibility(topIds[index], View.VISIBLE)
                setOnClickPendingIntent(topIds[index], pendingString(action, 7370 + index, true))
            }
            setViewVisibility(R.id.notificationTopRow2, if (menuActions.size > 2) View.VISIBLE else View.GONE)
        } else setViewVisibility(R.id.notificationTopRow2, if (state.buttons.size > 2 && !board) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.notificationTopControls, if (menu || state.buttons.isNotEmpty() && !board) View.VISIBLE else View.GONE)

        val cellIds = intArrayOf(R.id.notificationCell1, R.id.notificationCell2, R.id.notificationCell3, R.id.notificationCell4, R.id.notificationCell5, R.id.notificationCell6, R.id.notificationCell7, R.id.notificationCell8, R.id.notificationCell9)
        cellIds.forEachIndexed { index, id ->
            setTextViewText(id, "")
            setContentDescription(id, null)
            setViewVisibility(id, View.GONE)
            setOnClickPendingIntent(id, noOp(REQUEST_CODE_NO_OP_CELL_BASE + index))
        }
        cells.forEachIndexed { displayIndex, index ->
            val label = state.cellButtons[index]
            val id = cellIds[displayIndex]
            val action = state.cellActions.getOrNull(index)
            if (label.isNotBlank() && action != null && !menu) {
                val reason = state.disabledActions[action]
                setTextViewText(id, label)
                setContentDescription(id, cellContentDescription(index, label) + if (reason != null) ", unavailable: $reason" else "")
                setBoolean(id, "setEnabled", reason == null)
                setTextColor(id, appContext.getColor(if (reason == null) R.color.notification_primary_text else R.color.notification_secondary_text))
                setViewVisibility(id, View.VISIBLE)
                setOnClickPendingIntent(id, if (reason == null) pending(action, REQUEST_CODE_CELL_BASE + index) else noOp(REQUEST_CODE_NO_OP_CELL_BASE + index))
            }
        }
        setViewVisibility(R.id.notificationCellControls, if (cells.any() && !menu) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.notificationCellRow2, if (cells.count() > 3) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.notificationCellRow3, if (cells.count() > 6) View.VISIBLE else View.GONE)

    }

    private fun isGameOver(state: GameViewState): Boolean {
        if (state.isTerminal) return true
        if (state.phase == com.adbreak.arcade.core.GamePhase.WON || state.phase == com.adbreak.arcade.core.GamePhase.LOST || state.phase == com.adbreak.arcade.core.GamePhase.DRAW) return true
        return false
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
