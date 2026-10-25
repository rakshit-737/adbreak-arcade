package com.adbreak.arcade.trigger

import kotlin.math.max

/**
 * Coordinates launcher discovery, local app selection, and lifecycle-driven usage polling.
 *
 * Android does not provide a reliable broadcast for every arbitrary foreground-app change.
 * Call [poll] from an integration-owned lifecycle callback or other permitted scheduling point.
 * This service deliberately does not use an accessibility service, overlay, notification listener,
 * network, or foreground service, and it does not promise immediate background detection.
 */
class SmartAppTriggerService(
    private val ownPackageName: String,
    private val launcherAppDiscovery: LauncherAppDiscovery,
    private val usageAccessChecker: UsageAccessChecker,
    private val usageEventsReader: UsageEventsReader,
    private val stateStore: SmartTriggerStateStore,
    private val clock: TriggerClock = TriggerClock { System.currentTimeMillis() },
    private val config: SmartTriggerConfig = SmartTriggerConfig(),
) {
    /** Returns installed launcher apps with local selection state attached. */
    fun discoverApps(): List<SelectableLaunchableApp> {
        val selected = selectedPackageNames()
        return launcherAppDiscovery.discover()
            .asSequence()
            .filter { it.packageName.isNotBlank() && it.packageName != ownPackageName }
            .map { app -> app.copy(label = app.label.ifBlank { app.packageName }) }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .map { app -> SelectableLaunchableApp(app.packageName, app.label, app.packageName in selected) }
            .toList()
    }

    fun usageAccessStatus(): UsageAccessStatus = if (hasUsageAccess()) UsageAccessStatus.GRANTED else UsageAccessStatus.DENIED

    fun hasUsageAccess(): Boolean = runCatching { usageAccessChecker.hasUsageAccess() }.getOrDefault(false)

    fun selectedPackageNames(): Set<String> = stateStore.selectedPackageNames()
        .asSequence()
        .map(String::trim)
        .filter { it.isNotEmpty() && it != ownPackageName }
        .toSet()

    /** Persists only non-empty, non-self package names on this device. */
    fun setSelectedPackageNames(packageNames: Set<String>) {
        stateStore.saveSelectedPackageNames(
            packageNames.asSequence()
                .map(String::trim)
                .filter { it.isNotEmpty() && it != ownPackageName }
                .toSet(),
        )
    }

    fun clearSelectedPackageNames() = setSelectedPackageNames(emptySet())

    /** Clears the polling baseline, cooldown, and duplicate history without changing selections. */
    fun clearRuntimeHistory() = stateStore.clearRuntimeHistory()

    /**
     * Queries UsageStatsManager-backed events since the last scan and returns at most one trigger.
     * The first call establishes a baseline so an old app launch is not replayed as a new trigger.
     */
    fun poll(): SmartTriggerPollResult {
        val selected = selectedPackageNames()
        if (selected.isEmpty()) return SmartTriggerPollResult.NotConfigured
        if (!hasUsageAccess()) return SmartTriggerPollResult.UsageAccessDenied

        val now = clock.nowMillis()
        val lastScan = stateStore.lastScanAtMillis()
        if (lastScan == null) {
            stateStore.saveLastScanAtMillis(now)
            return SmartTriggerPollResult.BaselineEstablished
        }

        val begin = queryBeginMillis(lastScan, now)
        val events = try {
            usageEventsReader.query(begin, now)
        } catch (_: SecurityException) {
            return SmartTriggerPollResult.UsageAccessDenied
        }
        stateStore.saveLastScanAtMillis(now)

        val seen = stateStore.seenEventKeys().toMutableSet()
        val lastTrigger = stateStore.lastTriggerAtMillis()
        var inspectedCount = 0

        events.asSequence()
            .filter { it.timestampMillis in begin..now }
            .sortedBy { it.timestampMillis }
            .forEach { event ->
                if (!seen.add(event.key)) return@forEach
                inspectedCount += 1

                if (!event.isForegroundEvent()) return@forEach
                if (event.packageName == ownPackageName) return@forEach
                if (event.packageName !in selected) return@forEach
                if (cooldownActive(now, lastTrigger)) return@forEach

                stateStore.saveSeenEventKeys(trimSeenKeys(seen))
                stateStore.saveLastTriggerAtMillis(now)
                return SmartTriggerPollResult.Triggered(
                    TriggerMatch(
                        packageName = event.packageName,
                        eventTimestampMillis = event.timestampMillis,
                        detectedAtMillis = now,
                    ),
                )
            }

        stateStore.saveSeenEventKeys(trimSeenKeys(seen))
        return SmartTriggerPollResult.NoTrigger(inspectedCount)
    }

    private fun queryBeginMillis(lastScan: Long, now: Long): Long {
        if (lastScan > now) return max(0L, now - config.maxLookbackMillis)
        val overlapStart = max(0L, lastScan - config.queryOverlapMillis)
        return max(overlapStart, now - config.maxLookbackMillis)
    }

    private fun cooldownActive(now: Long, lastTrigger: Long?): Boolean {
        if (lastTrigger == null) return false
        if (now < lastTrigger) return true
        return now - lastTrigger < config.cooldownMillis
    }

    private fun trimSeenKeys(keys: Set<String>): Set<String> = keys.toList().takeLast(config.maxSeenEventKeys).toSet()
}
