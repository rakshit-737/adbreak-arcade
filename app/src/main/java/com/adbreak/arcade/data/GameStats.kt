package com.adbreak.arcade.data

import org.json.JSONObject

data class GameStats(
    val plays: Int = 0,
    val actions: Int = 0,
    val roundsCompleted: Int = 0,
) {
    fun encode(): String = JSONObject().apply {
        put("v", 1)
        put("plays", plays)
        put("actions", actions)
        put("roundsCompleted", roundsCompleted)
    }.toString()

    companion object {
        fun decode(encoded: String?): GameStats = runCatching {
            if (encoded.isNullOrBlank()) return GameStats()
            val json = JSONObject(encoded)
            if (json.optInt("v", -1) != 1) return GameStats()
            val plays = json.optInt("plays", -1)
            val actions = json.optInt("actions", -1)
            val roundsCompleted = json.optInt("roundsCompleted", -1)
            if (plays !in 0..1_000_000_000 || actions !in 0..1_000_000_000 ||
                roundsCompleted !in 0..actions) return GameStats()
            GameStats(plays, actions, roundsCompleted)
        }.getOrDefault(GameStats())
    }
}
