package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.track.komga.Komga
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import exh.md.utils.FollowStatus
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TrackStatusTest {

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

    /** The MangaDex table is keyed by an enum: a new [FollowStatus] must land in it, UNFOLLOWED excepted. */
    @Test
    fun followStatusesAllMapped() {
        val mapped = TrackStatus.mdListStatuses.keys
        val expected = FollowStatus.entries.filterNot { it == FollowStatus.UNFOLLOWED }.map { it.long }
        mapped shouldContainExactly expected.toSet()
    }

    @Test
    fun unfollowedIsNotAReadingState() {
        TrackStatus.mdListStatuses[FollowStatus.UNFOLLOWED.long] shouldBe null
    }

    @Test
    fun statusTablesCoverEightTrackers() {
        val tables = TrackStatus.statusTables(manager)
        tables.keys shouldContainExactly setOf(60L, 1L, 2L, 3L, 4L, 5L, 6L, 7L)
        tables.getValue(TrackerManager.KOMGA).keys shouldContainExactly setOf(Komga.READING, Komga.COMPLETED)
    }

    @Test
    fun parseTrackerStatusResolves() {
        TrackStatus.parseTrackerStatus(manager, TrackerManager.MYANIMELIST, MyAnimeList.READING) shouldBe
            TrackStatus.READING
        TrackStatus.parseTrackerStatus(manager, TrackerManager.KOMGA, Komga.COMPLETED) shouldBe
            TrackStatus.COMPLETED
        TrackStatus.parseTrackerStatus(manager, TrackerManager.MDLIST, FollowStatus.RE_READING.long) shouldBe
            TrackStatus.REPEATING
    }

    @Test
    fun parseTrackerStatusUnknown() {
        TrackStatus.parseTrackerStatus(manager, TrackerManager.HIKKA, 0L).shouldBeNull()
        TrackStatus.parseTrackerStatus(manager, TrackerManager.KOMGA, 99L).shouldBeNull()
        TrackStatus.entries.map { it.int } shouldContainExactly (1..7).toList()
    }
}
