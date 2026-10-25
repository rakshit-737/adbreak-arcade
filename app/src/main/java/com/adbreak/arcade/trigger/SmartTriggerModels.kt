package com.adbreak.arcade.trigger

/** A user-visible app discovered through the launcher intent. */
data class LaunchableApp(
    val packageName: String,
    val label: String,
)

/** Whether a usage event represents an app coming to the foreground. */
enum class UsageEventType {
    ACTIVITY_RESUMED,
    MOVE_TO_FOREGROUND,
    OTHER,
}

/** The small, platform-neutral event shape consumed by the trigger service. */
data class UsageEvent(
    val packageName: String,
    val timestampMillis: Long,
    val type: UsageEventType,
) {
    val key: String
        get() = "$packageName|$timestampMillis|${type.name}"

    fun isForegroundEvent(): Boolean = type == UsageEventType.ACTIVITY_RESUMED || type == UsageEventType.MOVE_TO_FOREGROUND
}

/** An app row suitable for a settings or selection screen. */
data class SelectableLaunchableApp(
    val packageName: String,
    val label: String,
    val selected: Boolean,
)

data class TriggerMatch(
    val packageName: String,
    val eventTimestampMillis: Long,
    val detectedAtMillis: Long,
)

enum class UsageAccessStatus {
    GRANTED,
    DENIED,
}

sealed class SmartTriggerPollResult {
    /** No package is selected, so polling is intentionally inactive. */
    data object NotConfigured : SmartTriggerPollResult()

    /** The first poll establishes a baseline and does not replay historical launches. */
    data object BaselineEstablished : SmartTriggerPollResult()

    /** Usage access is not available; no usage events were queried. */
    data object UsageAccessDenied : SmartTriggerPollResult()

    /** The poll completed without finding an eligible new launch. */
    data class NoTrigger(val inspectedEventCount: Int) : SmartTriggerPollResult()

    /** A selected, non-self app launch passed duplicate and cooldown guards. */
    data class Triggered(val match: TriggerMatch) : SmartTriggerPollResult()
}

data class SmartTriggerConfig(
    val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
    val queryOverlapMillis: Long = DEFAULT_QUERY_OVERLAP_MILLIS,
    val maxLookbackMillis: Long = DEFAULT_MAX_LOOKBACK_MILLIS,
    val maxSeenEventKeys: Int = DEFAULT_MAX_SEEN_EVENT_KEYS,
) {
    init {
        require(cooldownMillis >= 0L) { "cooldownMillis must be non-negative" }
        require(queryOverlapMillis >= 0L) { "queryOverlapMillis must be non-negative" }
        require(maxLookbackMillis > 0L) { "maxLookbackMillis must be positive" }
        require(maxSeenEventKeys > 0) { "maxSeenEventKeys must be positive" }
    }

    companion object {
        const val DEFAULT_COOLDOWN_MILLIS = 60_000L
        const val DEFAULT_QUERY_OVERLAP_MILLIS = 5_000L
        const val DEFAULT_MAX_LOOKBACK_MILLIS = 24 * 60 * 60 * 1_000L
        const val DEFAULT_MAX_SEEN_EVENT_KEYS = 64
    }
}

interface LauncherAppDiscovery {
    fun discover(): List<LaunchableApp>
}

interface UsageAccessChecker {
    fun hasUsageAccess(): Boolean
}

interface UsageEventsReader {
    /** Returns usage events in the inclusive time window, or throws SecurityException when access is denied. */
    fun query(beginMillis: Long, endMillis: Long): List<UsageEvent>
}

/** Local-only state used by the service; the Android implementation is backed by SharedPreferences. */
interface SmartTriggerStateStore {
    fun selectedPackageNames(): Set<String>
    fun saveSelectedPackageNames(packageNames: Set<String>)

    fun lastScanAtMillis(): Long?
    fun saveLastScanAtMillis(timestampMillis: Long)

    fun lastTriggerAtMillis(): Long?
    fun saveLastTriggerAtMillis(timestampMillis: Long)

    fun seenEventKeys(): Set<String>
    fun saveSeenEventKeys(eventKeys: Set<String>)

    fun clearRuntimeHistory()
}

fun interface TriggerClock {
    fun nowMillis(): Long
}
