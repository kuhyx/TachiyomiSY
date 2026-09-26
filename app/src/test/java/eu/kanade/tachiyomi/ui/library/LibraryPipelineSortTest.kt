package eu.kanade.tachiyomi.ui.library

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.track.TrackerManager
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks

internal class LibraryPipelineSortTest {
    private val libraryPreferences = LibraryPreferences(FlowPreferenceStore())
    private val trackerManager = mockk<TrackerManager> { every { getAll(any()) } returns emptyList() }
    private val pipeline = LibraryItemPipeline(
        preferences = mockk(),
        libraryPreferences = libraryPreferences,
        trackerManager = trackerManager,
        sourceManager = mockk(),
        getTracks = mockk(),
    )
    private val items = listOf(
        libItem(libEntry(libManga(1L, title = "b"), total = 2L)),
        libItem(libEntry(libManga(2L, title = "a"), total = 2L)),
        libItem(libEntry(libManga(3L, title = "c"), total = 1L)),
    )
    private val byId = items.associateBy { it.id }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    private fun sorted(category: Category, groupSort: LibrarySort? = null, ids: List<Long> = listOf(1L, 2L, 3L)) =
        with(pipeline) {
            mapOf(category to ids).applySort(byId, emptyMap(), emptySet(), groupSort)
        }.getValue(category)

    private fun categoryFor(type: LibrarySort.Type, direction: LibrarySort.Direction): Category =
        libCategory(1L, flags = LibrarySort(type, direction).flag)

    @Test
    fun ascendingThenAlphabetical() {
        val category = categoryFor(LibrarySort.Type.TotalChapters, LibrarySort.Direction.Ascending)
        sorted(category) shouldContainExactly listOf(3L, 2L, 1L)
    }

    @Test
    fun descendingReversesOnlyTheKey() {
        val category = categoryFor(LibrarySort.Type.TotalChapters, LibrarySort.Direction.Descending)
        sorted(category) shouldContainExactly listOf(2L, 1L, 3L)
    }

    @Test
    fun groupSortOverridesTheCategory() {
        val category = categoryFor(LibrarySort.Type.TotalChapters, LibrarySort.Direction.Ascending)
        val alphabetical = LibrarySort(LibrarySort.Type.Alphabetical, LibrarySort.Direction.Ascending)
        sorted(category, alphabetical, ids = listOf(1L, 2L, 3L, 9L)) shouldContainExactly listOf(2L, 1L, 3L)
    }

    @Test
    fun randomShufflesWithTheSeed() {
        libraryPreferences.randomSortSeed.set(7)
        val category = categoryFor(LibrarySort.Type.Random, LibrarySort.Direction.Ascending)
        val first = sorted(category)
        first shouldContainExactlyInAnyOrder listOf(1L, 2L, 3L)
        sorted(category) shouldBe first
    }

    @Test
    fun lazyLookupsServeTheirTypes() {
        libraryPreferences.sortTagsForLibrary.set(setOf("1|x"))
        sorted(categoryFor(LibrarySort.Type.TrackerMean, LibrarySort.Direction.Ascending)) shouldContainExactly
            listOf(2L, 1L, 3L)
        sorted(categoryFor(LibrarySort.Type.TagList, LibrarySort.Direction.Ascending)) shouldContainExactly
            listOf(2L, 1L, 3L)
    }

    @Test
    fun defaultsComeFromInjekt() {
        startKoin {
            modules(
                module {
                    single { trackerManager }
                    single<SourceManager> { mockk() }
                    single<GetTracks> { mockk() }
                },
            )
        }
        val built = LibraryItemPipeline(mockk<BasePreferences>(), libraryPreferences)
        with(built) { mapOf(libCategory(1L) to listOf(1L)).applySort(byId, emptyMap(), emptySet()) }.size shouldBe 1
    }
}
