package com.adbreak.arcade.notification

/** The same rule is used by the receiver path and the JVM adversarial tests. */
data class ActionEnvelope(val gameId: String, val sessionId: Long, val revision: Long) {
    val token: String get() = "$sessionId:$revision:$gameId"

    fun matches(game: String?, session: Long?, render: Long?): Boolean =
        sessionId > 0 && game == gameId && session == sessionId && render == revision
}
