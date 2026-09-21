package eu.kanade.tachiyomi.data.track

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TrackerManagerTest {

    private val koin = TrackKoin()
    private lateinit var manager: TrackerManager

    @BeforeEach
    fun setUp() {
        koin.start()
        manager = TrackerManager()
    }

    @AfterEach
    fun tearDown() {
        koin.stop()
    }

    @Test
    fun trackersCarryTheirIds() {
        manager.trackers.map { it.id } shouldContainExactly listOf(60L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L)
        manager.get(TrackerManager.KOMGA) shouldBe manager.komga
        manager.get(999L).shouldBeNull()
        manager.getAll(setOf(TrackerManager.HIKKA, TrackerManager.MDLIST)) shouldContainExactly
            listOf(manager.mdList, manager.hikka)
    }

    @Test
    fun loggedInFollowsCredentials() = runTest {
        manager.loggedInTrackers() shouldBe emptyList()
        manager.loggedInTrackersFlow().first() shouldBe emptyList()

        koin.trackPreferences.setCredentials(manager.kavita, "user", "pass")
        manager.loggedInTrackers() shouldContainExactly listOf(manager.kavita)
        manager.loggedInTrackersFlow().first() shouldContainExactly listOf(manager.kavita)
    }
}
