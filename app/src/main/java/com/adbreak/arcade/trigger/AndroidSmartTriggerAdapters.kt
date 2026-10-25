package com.adbreak.arcade.trigger

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings

/** PackageManager-backed launcher-intent discovery. */
class AndroidLauncherAppDiscovery(private val context: Context) : LauncherAppDiscovery {
    override fun discover(): List<LaunchableApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                LaunchableApp(
                    packageName = packageName,
                    label = resolveInfo.loadLabel(context.packageManager)?.toString().orEmpty(),
                )
            }
    }
}

/** Explicit AppOps check for Settings > Usage access. */
class AndroidUsageAccessChecker(private val context: Context) : UsageAccessChecker {
    override fun hasUsageAccess(): Boolean = runCatching {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            context.applicationInfo.uid,
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)
}

/** UsageStatsManager adapter that exposes only foreground transitions to the domain layer. */
class AndroidUsageEventsReader(private val context: Context) : UsageEventsReader {
    override fun query(beginMillis: Long, endMillis: Long): List<UsageEvent> {
        val manager = context.getSystemService(UsageStatsManager::class.java)
        val rawEvents = manager.queryEvents(beginMillis, endMillis)
        val event = UsageEvents.Event()
        val result = mutableListOf<UsageEvent>()

        while (rawEvents.hasNextEvent()) {
            rawEvents.getNextEvent(event)
            val type = foregroundType(event.eventType) ?: continue
            val packageName = event.packageName?.takeIf { it.isNotBlank() } ?: continue
            result += UsageEvent(packageName, event.timeStamp, type)
        }
        return result
    }

    private fun foregroundType(eventType: Int): UsageEventType? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && eventType == UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.ACTIVITY_RESUMED
        eventType == UsageEvents.Event.MOVE_TO_FOREGROUND -> UsageEventType.MOVE_TO_FOREGROUND
        else -> null
    }
}

/** SharedPreferences-backed, package-name-only local trigger state. */
class SharedPreferencesSmartTriggerState(context: Context) : SmartTriggerStateStore {
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun selectedPackageNames(): Set<String> = readStringSet(KEY_SELECTED_PACKAGES)

    override fun saveSelectedPackageNames(packageNames: Set<String>) {
        preferences.edit().putStringSet(KEY_SELECTED_PACKAGES, packageNames.toSet()).apply()
    }

    override fun lastScanAtMillis(): Long? = readLongOrNull(KEY_LAST_SCAN)

    override fun saveLastScanAtMillis(timestampMillis: Long) {
        preferences.edit().putLong(KEY_LAST_SCAN, timestampMillis).apply()
    }

    override fun lastTriggerAtMillis(): Long? = readLongOrNull(KEY_LAST_TRIGGER)

    override fun saveLastTriggerAtMillis(timestampMillis: Long) {
        preferences.edit().putLong(KEY_LAST_TRIGGER, timestampMillis).apply()
    }

    override fun seenEventKeys(): Set<String> = readStringSet(KEY_SEEN_EVENT_KEYS)

    override fun saveSeenEventKeys(eventKeys: Set<String>) {
        preferences.edit().putStringSet(KEY_SEEN_EVENT_KEYS, eventKeys.toSet()).apply()
    }

    override fun clearRuntimeHistory() {
        preferences.edit()
            .remove(KEY_LAST_SCAN)
            .remove(KEY_LAST_TRIGGER)
            .remove(KEY_SEEN_EVENT_KEYS)
            .apply()
    }

    private fun readStringSet(key: String): Set<String> = preferences.getStringSet(key, emptySet()).orEmpty().toSet()

    private fun readLongOrNull(key: String): Long? = if (preferences.contains(key)) preferences.getLong(key, 0L) else null

    companion object {
        private const val PREFERENCES_NAME = "smart_app_triggers"
        private const val KEY_SELECTED_PACKAGES = "selected_package_names"
        private const val KEY_LAST_SCAN = "last_scan_at_millis"
        private const val KEY_LAST_TRIGGER = "last_trigger_at_millis"
        private const val KEY_SEEN_EVENT_KEYS = "seen_event_keys"
    }
}

object SmartTriggerSettings {
    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}

object SmartAppTriggerServiceFactory {
    fun create(context: Context, config: SmartTriggerConfig = SmartTriggerConfig()): SmartAppTriggerService {
        val appContext = context.applicationContext
        return SmartAppTriggerService(
            ownPackageName = appContext.packageName,
            launcherAppDiscovery = AndroidLauncherAppDiscovery(appContext),
            usageAccessChecker = AndroidUsageAccessChecker(appContext),
            usageEventsReader = AndroidUsageEventsReader(appContext),
            stateStore = SharedPreferencesSmartTriggerState(appContext),
            config = config,
        )
    }
}
