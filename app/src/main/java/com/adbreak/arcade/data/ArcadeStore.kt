package com.adbreak.arcade.data

import android.content.Context
import com.adbreak.arcade.core.ArcadeGame
import com.adbreak.arcade.core.GameRegistry

class ArcadeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("arcade_state", Context.MODE_PRIVATE)
    var selectedGameId: String
        get() = prefs.getString("selected_game", "tap_counter")?.takeIf(GameRegistry::isKnown) ?: "tap_counter"
        set(value) { if (GameRegistry.isKnown(value)) prefs.edit().putString("selected_game", value).apply() }
    var active: Boolean
        get() = prefs.getBoolean("active", false)
        set(value) = prefs.edit().putBoolean("active", value).apply()
    var paused: Boolean
        get() = prefs.getBoolean("paused", false)
        set(value) = prefs.edit().putBoolean("paused", value).apply()

    var sessionId: Long
        get() = prefs.getLong("session_id", 0L)
        set(value) = prefs.edit().putLong("session_id", value).apply()

    var sessionGameId: String
        get() = prefs.getString("session_game", selectedGameId)?.takeIf(GameRegistry::isKnown) ?: selectedGameId
        set(value) = prefs.edit().putString("session_game", value).apply()

    var smartTriggersEnabled: Boolean
        get() = prefs.getBoolean("smart_triggers_enabled", false)
        set(value) = prefs.edit().putBoolean("smart_triggers_enabled", value).apply()

    var smartTriggerGameId: String
        get() = prefs.getString("smart_trigger_game", selectedGameId)?.takeIf(GameRegistry::isKnown) ?: selectedGameId
        set(value) { if (GameRegistry.isKnown(value)) prefs.edit().putString("smart_trigger_game", value).apply() }

    var smartTriggerCooldownMillis: Long
        get() = prefs.getLong("smart_trigger_cooldown", 60_000L).coerceIn(0L, 15 * 60_000L)
        set(value) = prefs.edit().putLong("smart_trigger_cooldown", value.coerceIn(0L, 15 * 60_000L)).apply()

    fun loadGame(id: String = selectedGameId): ArcadeGame {
        val safeId = id.takeIf(GameRegistry::isKnown) ?: "tap_counter"
        return GameRegistry.create(safeId).also {
            val saved = prefs.getString("state_$safeId", null)
            if (saved != null && !it.restore(saved)) prefs.edit().remove("state_$safeId").apply()
        }
    }

    fun saveGame(game: ArcadeGame) = prefs.edit().putString("state_${game.id}", game.encode()).commit()

    fun resetGame(id: String) {
        if (GameRegistry.isKnown(id)) prefs.edit().remove("state_$id").apply()
    }

    fun select(id: String) { if (GameRegistry.isKnown(id)) selectedGameId = id }
}
