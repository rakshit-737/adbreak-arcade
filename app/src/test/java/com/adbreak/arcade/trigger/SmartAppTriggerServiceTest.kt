package com.adbreak.arcade.trigger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAppTriggerServiceTest {
    @Test
    fun discoverAppsExcludesOwnPackageDeduplicatesAndMarksSelections() {
        val state = InMemoryStateStore(selected = setOf("com.video.app"))
        val service = service(
            state = state,
            apps = listOf(
                LaunchableApp("com.adbreak.arcade", "AdBreak Arcade"),
                LaunchableApp("com.video.app", "Video"),
                LaunchableApp("com.video.app", "Video duplicate"),
                LaunchableApp("com.chat.app", "Chat"),
            ),
        )

        assertEquals(
            listOf(
                SelectableLaunchableApp("com.chat.app", "Chat", false),
                SelectableLaunchableApp("com.video.app", "Video", true),
            ),
            service.discoverApps(),
        )
    }

    @Test
    fun selectedPackageNamesPersistLocallyAndNeverIncludeOwnPackage() {
        val state = InMemoryStateStore()
        val service = service(state = state)

        service.setSelectedPackageNames(setOf(" com.video.app ", "com.adbreak.arcade", ""))

        assertEquals(setOf("com.video.app"), state.selectedPackageNames())
        assertEquals(setOf("com.video.app"), service.selectedPackageNames())
    }

    @Test
    fun firstPollEstablishesBaselineWithoutReplayingHistoricalLaunch() {
        val clock = FakeClock(1_000L)
        val reader = FakeUsageEventsReader(
            listOf(UsageEvent("com.video.app", 900L, UsageEventType.ACTIVITY_RESUMED)),
        )
        val service = service(clock = clock, reader = reader).also {
            it.setSelectedPackageNames(setOf("com.video.app"))
        }

        assertTrue(service.poll() is SmartTriggerPollResult.BaselineEstablished)
        assertEquals(0, reader.queryCount)
    }

    @Test
    fun deniedUsageAccessReturnsExplicitResultAndDoesNotQueryEvents() {
        val access = FakeUsageAccessChecker(granted = false)
        val reader = FakeUsageEventsReader()
        val service = service(access = access, reader = reader).also {
            it.setSelectedPackageNames(setOf("com.video.app"))
        }

        assertEquals(UsageAccessStatus.DENIED, service.usageAccessStatus())
        assertTrue(service.poll() is SmartTriggerPollResult.UsageAccessDenied)
        assertEquals(0, reader.queryCount)
    }

    @Test
    fun matchingForegroundEventTriggersAndDuplicateEventIsIgnored() {
        val clock = FakeClock(2_000L)
        val event = UsageEvent("com.video.app", 2_050L, UsageEventType.ACTIVITY_RESUMED)
        val reader = FakeUsageEventsReader(listOf(event))
        val service = service(clock = clock, reader = reader).also {
            it.setSelectedPackageNames(setOf("com.video.app"))
        }

        service.poll()
        clock.now = 2_100L
        val first = service.poll()
        clock.now = 2_200L
        val duplicate = service.poll()

        assertEquals(
            SmartTriggerPollResult.Triggered(
                TriggerMatch("com.video.app", 2_050L, 2_100L),
            ),
            first,
        )
        assertEquals(SmartTriggerPollResult.NoTrigger(0), duplicate)
    }

    @Test
    fun cooldownSuppressesASecondNewEventUntilItExpires() {
        val clock = FakeClock(1_000L)
        val reader = FakeUsageEventsReader()
        val service = service(
            clock = clock,
            reader = reader,
            config = SmartTriggerConfig(cooldownMillis = 500L, queryOverlapMillis = 0L),
        ).also {
            it.setSelectedPackageNames(setOf("com.video.app"))
        }

        service.poll()
        reader.events = listOf(UsageEvent("com.video.app", 1_100L, UsageEventType.MOVE_TO_FOREGROUND))
        clock.now = 1_100L
        assertTrue(service.poll() is SmartTriggerPollResult.Triggered)

        reader.events = listOf(UsageEvent("com.video.app", 1_200L, UsageEventType.MOVE_TO_FOREGROUND))
        clock.now = 1_200L
        assertEquals(SmartTriggerPollResult.NoTrigger(1), service.poll())

        reader.events = listOf(UsageEvent("com.video.app", 1_700L, UsageEventType.MOVE_TO_FOREGROUND))
        clock.now = 1_700L
        assertTrue(service.poll() is SmartTriggerPollResult.Triggered)
    }

    @Test
    fun ownPackageIsExcludedEvenIfAnEventIsReturnedByUsageStats() {
        val clock = FakeClock(2_000L)
        val reader = FakeUsageEventsReader()
        val service = service(clock = clock, reader = reader).also {
            it.setSelectedPackageNames(setOf("com.adbreak.arcade"))
        }

        assertTrue(service.poll() is SmartTriggerPollResult.NotConfigured)

        service.setSelectedPackageNames(setOf("com.video.app"))
        service.poll()
        reader.events = listOf(UsageEvent("com.adbreak.arcade", 2_100L, UsageEventType.ACTIVITY_RESUMED))
        clock.now = 2_100L

        assertEquals(SmartTriggerPollResult.NoTrigger(1), service.poll())
    }

    @Test
    fun unrelatedAndNonForegroundEventsAreConsumedWithoutTriggering() {
        val clock = FakeClock(1_000L)
        val reader = FakeUsageEventsReader()
        val service = service(clock = clock, reader = reader).also {
            it.setSelectedPackageNames(setOf("com.video.app"))
        }

        service.poll()
        reader.events = listOf(
            UsageEvent("com.other.app", 1_100L, UsageEventType.ACTIVITY_RESUMED),
            UsageEvent("com.video.app", 1_200L, UsageEventType.OTHER),
        )
        clock.now = 1_200L

        assertEquals(SmartTriggerPollResult.NoTrigger(2), service.poll())
    }

    @Test
    fun duplicateHistorySurvivesAServiceRecreation() {
        val state = InMemoryStateStore()
        val clock = FakeClock(1_000L)
        val event = UsageEvent("com.video.app", 1_100L, UsageEventType.ACTIVITY_RESUMED)
        val reader = FakeUsageEventsReader()
        val firstService = service(state = state, clock = clock, reader = reader).also {
            it.setSelectedPackageNames(setOf("com.video.app"))
        }

        firstService.poll()
        reader.events = listOf(event)
        clock.now = 1_100L
        assertTrue(firstService.poll() is SmartTriggerPollResult.Triggered)

        val recreatedService = service(state = state, clock = clock, reader = reader)
        clock.now = 1_200L
        assertEquals(SmartTriggerPollResult.NoTrigger(0), recreatedService.poll())
    }

    @Test
    fun usageReaderSecurityExceptionIsHandledAsPermissionDenied() {
        val clock = FakeClock(1_000L)
        val reader = FakeUsageEventsReader().apply { throwSecurityException = true }
        val service = service(clock = clock, reader = reader).also {
            it.setSelectedPackageNames(setOf("com.video.app"))
        }

        service.poll()
        clock.now = 1_100L

        assertTrue(service.poll() is SmartTriggerPollResult.UsageAccessDenied)
    }

    private fun service(
        state: InMemoryStateStore = InMemoryStateStore(),
        apps: List<LaunchableApp> = emptyList(),
        access: FakeUsageAccessChecker = FakeUsageAccessChecker(),
        reader: FakeUsageEventsReader = FakeUsageEventsReader(),
        clock: FakeClock = FakeClock(1_000L),
        config: SmartTriggerConfig = SmartTriggerConfig(),
    ) = SmartAppTriggerService(
        ownPackageName = "com.adbreak.arcade",
        launcherAppDiscovery = FakeLauncherAppDiscovery(apps),
        usageAccessChecker = access,
        usageEventsReader = reader,
        stateStore = state,
        clock = clock,
        config = config,
    )

    private class FakeLauncherAppDiscovery(private val apps: List<LaunchableApp>) : LauncherAppDiscovery {
        override fun discover(): List<LaunchableApp> = apps
    }

    private class FakeUsageAccessChecker(var granted: Boolean = true) : UsageAccessChecker {
        override fun hasUsageAccess(): Boolean = granted
    }

    private class FakeUsageEventsReader(var events: List<UsageEvent> = emptyList()) : UsageEventsReader {
        var queryCount: Int = 0
        var throwSecurityException: Boolean = false

        override fun query(beginMillis: Long, endMillis: Long): List<UsageEvent> {
            queryCount += 1
            if (throwSecurityException) throw SecurityException("usage access denied")
            return events
        }
    }

    private class FakeClock(var now: Long) : TriggerClock {
        override fun nowMillis(): Long = now
    }

    private class InMemoryStateStore(
        selected: Set<String> = emptySet(),
    ) : SmartTriggerStateStore {
        private var selectedPackages = selected.toSet()
        private var lastScan: Long? = null
        private var lastTrigger: Long? = null
        private var seenKeys = emptySet<String>()

        override fun selectedPackageNames(): Set<String> = selectedPackages

        override fun saveSelectedPackageNames(packageNames: Set<String>) {
            selectedPackages = packageNames.toSet()
        }

        override fun lastScanAtMillis(): Long? = lastScan

        override fun saveLastScanAtMillis(timestampMillis: Long) {
            lastScan = timestampMillis
        }

        override fun lastTriggerAtMillis(): Long? = lastTrigger

        override fun saveLastTriggerAtMillis(timestampMillis: Long) {
            lastTrigger = timestampMillis
        }

        override fun seenEventKeys(): Set<String> = seenKeys

        override fun saveSeenEventKeys(eventKeys: Set<String>) {
            seenKeys = eventKeys.toSet()
        }

        override fun clearRuntimeHistory() {
            lastScan = null
            lastTrigger = null
            seenKeys = emptySet()
        }
    }
}
