package com.adbreak.arcade.data

import android.content.Context
import com.adbreak.arcade.core.ArcadeGame
import com.adbreak.arcade.core.GameRegistry
import org.json.JSONArray
import org.json.JSONObject

class ArcadeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("arcade_state", Context.MODE_PRIVATE)
    private val profileKey = "arcade_profile_v1"
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

    var renderRevision: Long
        get() = prefs.getLong("render_revision", 0L)
        set(value) = prefs.edit().putLong("render_revision", value).apply()

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

    fun clearGameHistory(id: String) {
        if (!GameRegistry.isKnown(id)) return
        prefs.edit().remove("state_$id").remove("stats_$id").commit()
        updateProfile { profile ->
            profile.recent.remove(id)
            profile
        }
    }

    fun select(id: String) { if (GameRegistry.isKnown(id)) selectedGameId = id }

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    fun isFavorite(id: String): Boolean = readProfile().favorites.contains(id)

    fun toggleFavorite(id: String): Boolean {
        if (!GameRegistry.isKnown(id)) return false
        var selected = false
        updateProfile { profile ->
            if (profile.favorites.contains(id)) profile.favorites.remove(id) else profile.favorites.add(id)
            selected = profile.favorites.contains(id)
            profile
        }
        return selected
    }

    fun favoriteGameIds(): List<String> = readProfile().favorites.filter(GameRegistry::isKnown)

    fun recentGameIds(): List<String> = readProfile().recent.filter(GameRegistry::isKnown)

    fun arcadeLevel(): Int = (readProfile().totalXp / 100) + 1

    fun arcadeXpIntoLevel(): Int = readProfile().totalXp % 100

    fun totalXp(): Int = readProfile().totalXp

    fun statsFor(id: String): GameStats = GameStats.decode(prefs.getString("stats_$id", null))

    /** Records only accepted gameplay moves; preparing or rendering a notification is not progression. */
    fun recordGameAction(id: String, terminal: Boolean) {
        if (!GameRegistry.isKnown(id)) return
        val current = statsFor(id)
        val next = current.copy(
            plays = current.plays + if (current.actions == 0) 1 else 0,
            actions = current.actions + 1,
            roundsCompleted = current.roundsCompleted + if (terminal) 1 else 0,
        )
        val editor = prefs.edit().putString("stats_$id", next.encode())
        val profile = readProfile()
        profile.totalXp += (if (current.actions == 0) 10 else 0) + if (terminal) 10 else 0
        profile.recent.remove(id)
        profile.recent.add(0, id)
        while (profile.recent.size > 5) profile.recent.removeAt(profile.recent.lastIndex)
        editor.putString(profileKey, profile.encode()).commit()
    }

    private data class Profile(
        var totalXp: Int = 0,
        val favorites: MutableSet<String> = linkedSetOf(),
        val recent: MutableList<String> = mutableListOf(),
    ) {
        fun encode(): String = JSONObject().apply {
            put("v", 1)
            put("totalXp", totalXp.coerceAtLeast(0))
            put("favorites", JSONArray(favorites.filter(GameRegistry::isKnown)))
            put("recent", JSONArray(recent.filter(GameRegistry::isKnown).distinct().take(5)))
        }.toString()
    }

    private fun readProfile(): Profile = runCatching {
        val json = JSONObject(prefs.getString(profileKey, null) ?: return Profile())
        val profile = Profile(totalXp = json.optInt("totalXp", 0).coerceAtLeast(0))
        json.optJSONArray("favorites")?.let { array ->
            for (index in 0 until array.length()) array.optString(index).takeIf(GameRegistry::isKnown)?.let(profile.favorites::add)
        }
        json.optJSONArray("recent")?.let { array ->
            for (index in 0 until array.length()) array.optString(index).takeIf(GameRegistry::isKnown)?.let(profile.recent::add)
        }
        profile
    }.getOrDefault(Profile())

    private fun updateProfile(transform: (Profile) -> Profile) {
        prefs.edit().putString(profileKey, transform(readProfile()).encode()).apply()
    }
}
