package eu.kanade.tachiyomi.ui.library

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.ItemPreferences
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.model.Track

internal class LibraryPipelineFilterTest {
    private val pipeline = LibraryItemPipeline(
        preferences = mockk(),
        libraryPreferences = LibraryPreferences(FlowPreferenceStore()),
        trackerManager = mockk(),
        sourceManager = mockk(),
        getTracks = mockk(),
    )

    @BeforeEach
    fun setUp() {
        // isLewd() looks the source up through Injekt.
        startKoin { modules(module { single<SourceManager> { mockk(relaxed = true) } }) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    private fun List<LibraryItem>.filter(
        prefs: ItemPreferences,
        tracks: Map<Long, List<Track>> = emptyMap(),
        trackFilter: Map<Long, TriState> = emptyMap(),
    ): List<Long> = with(pipeline) { applyFilters(tracks, trackFilter, prefs) }.map { it.id }

    // Both non-disabled states of pick applied to items: the ids kept by IS, then by NOT.
    private fun both(items: List<LibraryItem>, pick: (TriState) -> ItemPreferences): Pair<List<Long>, List<Long>> =
        items.filter(pick(TriState.ENABLED_IS)) to items.filter(pick(TriState.ENABLED_NOT))

    @Test
    fun downloadedKeepsLocalOrQueued() {
        val items = listOf(
            libItem(libEntry(libManga(1L)), local = true),
            libItem(libEntry(libManga(2L)), downloads = 2),
            libItem(3L),
        )
        both(items) { itemPrefs().copy(filterDownloaded = it) } shouldBe (listOf(1L, 2L) to listOf(3L))
        items.filter(itemPrefs(globalDownloaded = true)) shouldContainExactly listOf(1L, 2L)
        items.filter(itemPrefs()) shouldContainExactly listOf(1L, 2L, 3L)
    }

    @Test
    fun readingStateFilters() {
        val items = listOf(
            libItem(libEntry(libManga(1L), total = 3L, read = 1L, bookmarks = 1L)),
            libItem(libEntry(libManga(2L), total = 1L, read = 1L)),
        )
        both(items) { itemPrefs().copy(filterUnread = it) } shouldBe (listOf(1L) to listOf(2L))
        both(items) { itemPrefs().copy(filterBookmarked = it) } shouldBe (listOf(1L) to listOf(2L))
        val fresh = items + libItem(3L)
        both(fresh) { itemPrefs().copy(filterStarted = it) } shouldBe (listOf(1L, 2L) to listOf(3L))
    }

    @Test
    fun completedFilter() {
        val done = libItem(libEntry(libManga(1L).copy(ogStatus = SManga.COMPLETED.toLong())))
        val items = listOf(done, libItem(2L))
        both(items) { itemPrefs().copy(filterCompleted = it) } shouldBe (listOf(1L) to listOf(2L))
    }

    @Test
    fun customIntervalOnlyWhenSkipping() {
        val custom = libItem(libEntry(libManga(1L).copy(fetchInterval = -3)))
        val items = listOf(custom, libItem(2L))
        both(items) { itemPrefs(skipOutsideRelease = true).copy(filterIntervalCustom = it) } shouldBe
            (listOf(1L) to listOf(2L))
        items.filter(itemPrefs(skipOutsideRelease = true)) shouldContainExactly listOf(1L, 2L)
    }

    @Test
    fun lewdFilter() {
        val lewd = libItem(libEntry(libManga(1L).copy(ogGenre = listOf("Hentai"))))
        val items = listOf(lewd, libItem(2L))
        both(items) { itemPrefs().copy(filterLewd = it) } shouldBe (listOf(1L) to listOf(2L))
    }

    @Test
    fun trackersIncludeAndExclude() {
        val items = listOf(libItem(1L), libItem(2L), libItem(3L))
        val tracks = mapOf(1L to listOf(track(1L, 10L)), 2L to listOf(track(2L, 20L)))
        items.filter(itemPrefs(), tracks, mapOf(10L to TriState.ENABLED_IS)) shouldContainExactly listOf(1L)
        items.filter(itemPrefs(), tracks, mapOf(20L to TriState.ENABLED_NOT)) shouldContainExactly listOf(1L, 3L)
        val mixed = mapOf(10L to TriState.ENABLED_IS, 20L to TriState.ENABLED_NOT)
        items.filter(itemPrefs(), tracks, mixed) shouldContainExactly listOf(1L)
        items.filter(itemPrefs(), tracks, mapOf(10L to TriState.DISABLED)) shouldContainExactly listOf(1L, 2L, 3L)
    }
}
