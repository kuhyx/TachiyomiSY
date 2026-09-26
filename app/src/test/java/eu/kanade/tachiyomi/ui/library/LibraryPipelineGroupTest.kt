package eu.kanade.tachiyomi.ui.library

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.track.TrackStatus
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.source.local.LocalSource

@RunWith(RobolectricTestRunner::class)
internal class LibraryPipelineGroupTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val trackerManager = mockk<TrackerManager>(relaxed = true)
    private val sourceManager = mockk<SourceManager>()
    private val getTracks = mockk<GetTracks>()
    private val pipeline = LibraryItemPipeline(
        preferences = BasePreferences(app, FlowPreferenceStore()),
        libraryPreferences = LibraryPreferences(FlowPreferenceStore()),
        trackerManager = trackerManager,
        sourceManager = sourceManager,
        getTracks = getTracks,
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun List<LibraryItem>.group(
        type: Int,
        categories: List<Category> = emptyList(),
        system: Boolean = false,
    ): Map<Category, List<Long>> = with(pipeline) { applyGrouping(categories, system, type) }

    private fun Map<Category, List<Long>>.named(): Map<String, List<Long>> = mapKeys { it.key.name }

    private fun source(id: Long, name: String): Source = mockk<Source>().also {
        every { it.id } returns id
        every { it.name } returns name
    }

    @Test
    fun defaultGroupingUsesCategories() {
        val items = listOf(libItem(1L, listOf(0L)), libItem(2L, listOf(1L, 2L)), libItem(3L, listOf(1L)))
        val categories = listOf(libCategory(0L), libCategory(1L), libCategory(2L), libCategory(3L))
        items.group(LibraryGroup.BY_DEFAULT, categories).named() shouldBe
            mapOf("Cat 1" to listOf(2L, 3L), "Cat 2" to listOf(2L), "Cat 3" to emptyList())
        items.group(LibraryGroup.BY_DEFAULT, categories, system = true).keys.map { it.id } shouldContainExactly
            listOf(0L, 1L, 2L, 3L)
    }

    @Test
    fun ungroupedIsOneCategory() {
        val items = listOf(libItem(1L), libItem(2L))
        items.group(LibraryGroup.UNGROUPED).named() shouldBe mapOf("Ungrouped" to listOf(1L, 2L))
        items.group(99).isEmpty() shouldBe true
    }

    @Test
    fun byStatusOrdersKnownFirst() {
        val items = listOf(
            libItem(libEntry(libManga(1L).copy(ogStatus = SManga.COMPLETED.toLong()))),
            libItem(libEntry(libManga(2L).copy(ogStatus = SManga.ONGOING.toLong()))),
            libItem(libEntry(libManga(3L).copy(ogStatus = 42L))),
        )
        val grouped = items.group(LibraryGroup.BY_STATUS)
        grouped.named() shouldBe mapOf("Ongoing" to listOf(2L), "Completed" to listOf(1L), "Unknown" to listOf(3L))
        grouped.keys.map { it.id } shouldContainExactly listOf(SManga.ONGOING + 1L, SManga.COMPLETED + 1L, 43L)
    }

    @Test
    fun bySourceSortsByName() {
        every { sourceManager.getOrStub(1L) } returns source(1L, "beta")
        every { sourceManager.getOrStub(2L) } returns source(2L, "")
        every { sourceManager.getOrStub(LocalSource.ID) } returns source(LocalSource.ID, "Local")
        // A stub that reports another id: its entries sort last.
        every { sourceManager.getOrStub(5L) } returns source(6L, "alpha")
        val items = listOf(
            libItem(libEntry(libManga(1L, source = 1L))),
            libItem(libEntry(libManga(2L, source = 2L))),
            libItem(libEntry(libManga(3L, source = LocalSource.ID))),
            libItem(libEntry(libManga(4L, source = 5L))),
        )
        items.group(LibraryGroup.BY_SOURCE).named().keys.toList() shouldContainExactly
            listOf("2", "beta", "Local source", "alpha")
    }

    @Test
    fun byTrackStatusFallsBackToOther() {
        mockkObject(TrackStatus.Companion)
        every { TrackStatus.parseTrackerStatus(any(), any(), any()) } returns null
        every { TrackStatus.parseTrackerStatus(any(), 1L, any()) } returns TrackStatus.READING
        coEvery { getTracks.await() } returns listOf(track(1L, 1L), track(2L, 9L))
        val items = listOf(libItem(1L), libItem(2L), libItem(3L))
        items.group(LibraryGroup.BY_TRACK_STATUS).named() shouldBe
            mapOf("Reading" to listOf(1L), "Not tracked" to listOf(2L, 3L))
    }
}
