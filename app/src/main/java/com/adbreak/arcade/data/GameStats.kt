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
            GameStats(
                plays = json.optInt("plays", 0).coerceAtLeast(0),
                actions = json.optInt("actions", 0).coerceAtLeast(0),
                roundsCompleted = json.optInt("roundsCompleted", 0).coerceAtLeast(0),
            )
        }.getOrDefault(GameStats())
    }
}
