package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.track.TrackStatus
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.track.model.Track

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
internal class LibraryPipelineGroupingTest {
    private val harness = LibraryHarness()
    private val pipeline by lazy {
        LibraryItemPipeline(
            preferences = harness.basePreferences,
            libraryPreferences = harness.libraryPreferences,
            trackerManager = harness.trackerManager,
            sourceManager = harness.sourceManager,
            getTracks = harness.getTracks,
        )
    }
    private val system = Category(id = 0, name = "", order = 0, flags = 0)
    private val reading = Category(id = 1, name = "Reading", order = 1, flags = 0)
    private val empty = Category(id = 2, name = "Empty", order = 2, flags = 0)

    // 1: "b" in Reading from the local source, ongoing; 2: "a" uncategorised from source 5;
    // 3: "c" in Reading from a nameless source 6, completed.
    private val items by lazy {
        listOf(
            libraryItem(manga(1, "b").copy(source = 0, ogStatus = 1), categories = listOf(1)),
            libraryItem(manga(2, "a").copy(source = 5, ogStatus = 0), categories = listOf(0)),
            libraryItem(manga(3, "c").copy(source = 6, ogStatus = 2), categories = listOf(1)),
        )
    }

    @Before
    fun setUp() {
        startKoin { modules(harness.koinModules()) }
        every { harness.sourceManager.getOrStub(any()) } answers {
            source(firstArg(), if (firstArg<Long>() == 5L) "Five" else "")
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    private fun source(id: Long, name: String): Source {
        val source = mockk<Source>()
        every { source.id } returns id
        every { source.name } returns name
        return source
    }

    private fun group(type: Int, showSystem: Boolean = false) = with(pipeline) {
        items.applyGrouping(listOf(system, reading, empty), showSystem, type).mapKeys { it.key.name }
    }

    @Test
    fun defaultGroupsByCategory() {
        group(LibraryGroup.BY_DEFAULT) shouldBe mapOf("Reading" to listOf(1L, 3L), "Empty" to emptyList())
        group(LibraryGroup.BY_DEFAULT, showSystem = true).keys shouldBe setOf("", "Reading", "Empty")
    }

    @Test
    fun ungroupedIsOneList() {
        group(LibraryGroup.UNGROUPED) shouldBe mapOf("Ungrouped" to listOf(1L, 2L, 3L))
    }

    @Test
    fun groupsBySourceName() {
        group(LibraryGroup.BY_SOURCE) shouldBe
            mapOf("Five" to listOf(2L), "6" to listOf(3L), "Local source" to listOf(1L))
    }

    @Test
    fun groupsByStatus() {
        group(LibraryGroup.BY_STATUS) shouldBe
            mapOf("Ongoing" to listOf(1L), "Completed" to listOf(3L), "Unknown" to listOf(2L))
    }

    @Test
    fun groupsByTrackStatus() {
        mockkObject(TrackStatus.Companion)
        every { TrackStatus.parseTrackerStatus(any(), 7L, 1L) } returns TrackStatus.READING
        every { TrackStatus.parseTrackerStatus(any(), 8L, any()) } returns null
        coEvery { harness.getTracks.await() } returns listOf(
            domainTrack(trackerId = 7L).copy(mangaId = 1L, status = 1L),
            domainTrack(trackerId = 8L).copy(mangaId = 2L),
        )
        group(LibraryGroup.BY_TRACK_STATUS) shouldBe
            mapOf("Reading" to listOf(1L), "Not tracked" to listOf(2L, 3L))
    }

    @Test
    fun unknownGroupingIsEmpty() {
        group(UNKNOWN_GROUP) shouldBe emptyMap()
    }

    @Test
    fun sortFollowsTheCategory() {
        val byId = items.associateBy { it.id }
        val none = emptyMap<Long, List<Track>>()
        val ascending = reading.copy(flags = sort(LibrarySort.Type.Alphabetical, ascending = true).flag)
        val descending = ascending.copy(flags = sort(LibrarySort.Type.Alphabetical, ascending = false).flag)
        val random = empty.copy(flags = sort(LibrarySort.Type.Random, ascending = true).flag)
        with(pipeline) {
            mapOf(ascending to listOf(3L, 1L, 2L)).applySort(byId, none, emptySet())[ascending] shouldBe
                listOf(2L, 1L, 3L)
            mapOf(descending to listOf(1L, 2L)).applySort(byId, none, emptySet())[descending] shouldBe listOf(1L, 2L)
            mapOf(random to listOf(1L, 2L, 3L)).applySort(byId, none, emptySet())[random]!!.sorted() shouldBe
                listOf(1L, 2L, 3L)
            val grouped = mapOf(ascending to listOf(1L, 2L))
            val descending = sort(LibrarySort.Type.Alphabetical, ascending = false)
            val bySort = grouped.applySort(byId, none, emptySet(), descending)
            bySort[ascending] shouldBe listOf(1L, 2L)
        }
    }
}

private const val UNKNOWN_GROUP = 99

private fun sort(type: LibrarySort.Type, ascending: Boolean) =
    LibrarySort(type, if (ascending) LibrarySort.Direction.Ascending else LibrarySort.Direction.Descending)
