package com.adbreak.arcade.data

import android.content.Context
import com.adbreak.arcade.core.ArcadeGame
import com.adbreak.arcade.core.GameRegistry
import org.json.JSONArray
import org.json.JSONObject

class ArcadeStore(context: Context) {
    companion object {
        // All controller instances (Activity, receiver, trigger) share one transaction boundary.
        val sessionLock = Any()
    }
    private val prefs = context.applicationContext.getSharedPreferences("arcade_state", Context.MODE_PRIVATE)
    private val profileKey = "arcade_profile_v1"
    private val lastActionKey = "last_consumed_action"
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

    var sessionInteracted: Boolean
        get() = prefs.getBoolean("session_interacted", false)
        set(value) = prefs.edit().putBoolean("session_interacted", value).apply()

    var smartTriggersEnabled: Boolean
        get() = prefs.getBoolean("smart_triggers_enabled", false)
        set(value) = prefs.edit().putBoolean("smart_triggers_enabled", value).apply()

    var smartTriggerGameId: String
        get() = prefs.getString("smart_trigger_game", selectedGameId)?.takeIf(GameRegistry::isKnown) ?: selectedGameId
        set(value) { if (GameRegistry.isKnown(value)) prefs.edit().putString("smart_trigger_game", value).apply() }

    var smartTriggerCooldownMillis: Long
        get() = prefs.getLong("smart_trigger_cooldown", 60_000L).coerceIn(0L, 15 * 60_000L)
        set(value) = prefs.edit().putLong("smart_trigger_cooldown", value.coerceIn(0L, 15 * 60_000L)).apply()

    fun loadGame(id: String = selectedGameId): ArcadeGame = synchronized(sessionLock) {
        val safeId = id.takeIf(GameRegistry::isKnown) ?: "tap_counter"
        val game = GameRegistry.create(safeId)
        val saved = prefs.getString("state_$safeId", null)
        if (saved != null && game.restore(saved)) return@synchronized game
        // A rejected legacy restore must not leave a partially mutated fresh game.
        val fresh = if (saved == null) game else GameRegistry.create(safeId)
        val editor = prefs.edit().putString("state_$safeId", fresh.encode())
        if (saved != null) editor.putString("unreadable_state_$safeId", saved).putBoolean("recovered_$safeId", true)
        // Save the actual first puzzle before exposing it to a notification or preview.
        check(editor.commit()) { "Unable to save game" }
        fresh
    }

    fun hasSavedGame(id: String): Boolean = prefs.contains("state_$id")

    val sessionNotice: String get() = prefs.getString("session_notice", "").orEmpty()
    val controlsMenu: Boolean get() = prefs.getBoolean("controls_menu", false)
    val cellPage: Int get() = prefs.getInt("cell_page", 0).coerceIn(0, 2)

    fun beginSession(): Boolean = synchronized(sessionLock) {
        val id = selectedGameId
        val restored = hasSavedGame(id)
        val game = loadGame(id)
        prefs.edit()
            .putString("state_$id", game.encode())
            .putString("session_game", id)
            .putLong("session_id", maxOf(System.currentTimeMillis(), sessionId + 1))
            .putLong("render_revision", 0)
            .putBoolean("active", true)
            .putBoolean("paused", false)
            .putBoolean("session_interacted", false)
            .putBoolean("controls_menu", false)
            .putInt("cell_page", 0)
            .putString("session_notice", when {
                prefs.getBoolean("recovered_$id", false) -> "Saved state unreadable · new round"
                restored -> "Saved session restored"
                else -> ""
            })
            .remove("recovered_$id")
            .remove(lastActionKey)
            .commit()
    }

    fun endSession(): Boolean = synchronized(sessionLock) {
        prefs.edit().putBoolean("active", false).putBoolean("paused", false)
            .putBoolean("session_interacted", false).putLong("session_id", sessionId + 1)
            .putLong("render_revision", 0).putBoolean("controls_menu", false)
            .putString("session_notice", "").commit()
    }

    fun saveGame(game: ArcadeGame) = prefs.edit().putString("state_${game.id}", game.encode()).commit()

    /** Atomically persists an accepted gameplay move and its progression side effects. */
    fun saveAcceptedAction(game: ArcadeGame, terminal: Boolean, roundStarted: Boolean, actionToken: String): Boolean = synchronized(sessionLock) {
        if (prefs.getString(lastActionKey, null) == actionToken) return@synchronized false
        val current = statsFor(game.id)
        val next = current.copy(
            plays = current.plays + if (roundStarted) 1 else 0,
            actions = current.actions + 1,
            roundsCompleted = current.roundsCompleted + if (terminal) 1 else 0,
        )
        val profile = readProfile()
        profile.totalXp += (if (current.actions == 0) 10 else 0) + if (terminal) 10 else 0
        profile.recent.remove(game.id)
        profile.recent.add(0, game.id)
        while (profile.recent.size > 5) profile.recent.removeAt(profile.recent.lastIndex)
        val candidate = game.render().record
        val best = recordFor(game.id)
        val newRecord = candidate != null && candidate.value >= 0 && (best == null || best.beatenBy(candidate))
        val editor = prefs.edit()
            .putString("state_${game.id}", game.encode())
            .putString("stats_${game.id}", next.encode())
            .putString(profileKey, profile.encode())
            .putString(lastActionKey, actionToken)
            .putBoolean("session_interacted", true)
            .putLong("render_revision", renderRevision + 1)
            .putString("session_notice", if (newRecord) "Personal best · ${candidate!!.label} ${candidate.value}" else "")
        if (newRecord) editor.putString("record_${game.id}", PersonalRecord.from(candidate!!).encode())
        editor.commit()
    }

    /** Persists a reset/new-round action without counting it as gameplay. */
    fun saveRoundReset(game: ArcadeGame, actionToken: String): Boolean = synchronized(sessionLock) {
        if (prefs.getString(lastActionKey, null) == actionToken) return@synchronized false
        prefs.edit()
            .putString("state_${game.id}", game.encode())
            .putBoolean("paused", false)
            .putBoolean("session_interacted", false)
            .putString(lastActionKey, actionToken)
            .putLong("render_revision", renderRevision + 1)
            .putBoolean("controls_menu", false)
            .putInt("cell_page", 0)
            .putString("session_notice", "")
            .commit()
    }

    fun saveSessionAction(paused: Boolean, actionToken: String): Boolean = synchronized(sessionLock) {
        if (prefs.getString(lastActionKey, null) == actionToken) return@synchronized false
        prefs.edit()
            .putBoolean("paused", paused)
            .putString(lastActionKey, actionToken)
            .putLong("render_revision", renderRevision + 1)
            .putString("session_notice", "")
            .commit()
    }

    fun saveControlsPage(menu: Boolean, page: Int = cellPage): Boolean = synchronized(sessionLock) {
        prefs.edit().putBoolean("controls_menu", menu).putInt("cell_page", page.coerceIn(0, 2))
            .putLong("render_revision", renderRevision + 1).commit()
    }

    fun recordFor(id: String): PersonalRecord? = PersonalRecord.decode(prefs.getString("record_$id", null))

    fun resetGame(id: String) {
        if (GameRegistry.isKnown(id)) prefs.edit().remove("state_$id").apply()
    }

    fun clearGameHistory(id: String) {
        if (!GameRegistry.isKnown(id)) return
        prefs.edit().remove("state_$id").remove("stats_$id").remove("record_$id")
            .remove("unreadable_state_$id").remove("recovered_$id").commit()
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
