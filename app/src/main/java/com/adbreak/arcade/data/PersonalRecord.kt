package com.adbreak.arcade.data

import com.adbreak.arcade.core.GameRecord
import org.json.JSONObject

data class PersonalRecord(val value: Int, val label: String, val higherIsBetter: Boolean) {
    fun beatenBy(candidate: GameRecord): Boolean = candidate.value >= 0 &&
        candidate.label == label && candidate.higherIsBetter == higherIsBetter &&
        (if (higherIsBetter) candidate.value > value else candidate.value < value)

    fun encode(): String = JSONObject().apply {
        put("v", 1)
        put("value", value)
        put("label", label)
        put("higherIsBetter", higherIsBetter)
    }.toString()

    companion object {
        fun from(candidate: GameRecord) = PersonalRecord(candidate.value, candidate.label, candidate.higherIsBetter)

        fun decode(encoded: String?): PersonalRecord? = runCatching {
            val json = JSONObject(encoded ?: return null)
            val value = json.getInt("value")
            val label = json.getString("label")
            if (json.getInt("v") != 1 || value < 0 || label.isBlank() || label.length > 80) return null
            PersonalRecord(value, label, json.getBoolean("higherIsBetter"))
        }.getOrNull()
    }
}
