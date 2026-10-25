package com.adbreak.arcade.data

import android.content.Context
import com.adbreak.arcade.core.GameRegistry

class ArcadeStore(context: Context) {
    private val prefs = context.getSharedPreferences("arcade_state", Context.MODE_PRIVATE)
    var selectedGameId: String
        get() = prefs.getString("selected_game", "tap_counter") ?: "tap_counter"
        set(value) = prefs.edit().putString("selected_game", value).apply()
    var active: Boolean
        get() = prefs.getBoolean("active", false)
        set(value) = prefs.edit().putBoolean("active", value).apply()
    var paused: Boolean
        get() = prefs.getBoolean("paused", false)
        set(value) = prefs.edit().putBoolean("paused", value).apply()
    fun loadGame() = GameRegistry.create(selectedGameId).also {
        val saved = prefs.getString("state_${selectedGameId}", null)
        if (saved != null && !it.restore(saved)) prefs.edit().remove("state_${selectedGameId}").apply()
    }
    fun saveGame(game: com.adbreak.arcade.core.ArcadeGame) = prefs.edit().putString("state_${game.id}", game.encode()).apply()
    fun select(id: String) { if (GameRegistry.isKnown(id)) selectedGameId = id }
}
