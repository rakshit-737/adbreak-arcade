package com.adbreak.arcade.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adbreak.arcade.core.GameAction

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationController(context).handle(intent.getStringExtra(NotificationController.EXTRA_ACTION))
    }
}
