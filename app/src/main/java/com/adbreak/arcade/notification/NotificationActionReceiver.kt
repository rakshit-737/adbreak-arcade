package com.adbreak.arcade.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(NotificationController.EXTRA_ACTION)
        if (action != null) {
            val sessionId = if (intent.hasExtra(NotificationController.EXTRA_SESSION_ID)) {
                intent.getLongExtra(NotificationController.EXTRA_SESSION_ID, Long.MIN_VALUE)
            } else {
                null
            }
            NotificationController(context.applicationContext).handle(
                action,
                intent.getStringExtra(NotificationController.EXTRA_GAME_ID),
                sessionId,
            )
        }
    }
}
