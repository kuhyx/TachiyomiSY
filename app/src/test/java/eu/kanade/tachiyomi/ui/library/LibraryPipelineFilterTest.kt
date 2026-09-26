package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.track.domainTrack
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.TriState


@RunWith(RobolectricTestRunner::class)
internal class LibraryPipelineFilterTest {
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

    // 1: local, unread, completed, custom interval; 2: downloaded, fully read, bookmarked; 3: lewd by its tags.
    private val items by lazy {
        listOf(
            libraryItem(manga(1).copy(source = 0, ogStatus = 2, fetchInterval = -1), totalChapters = 2)
                .copy(isLocal = true),
            libraryItem(manga(2), totalChapters = 2, readCount = 2, downloadCount = 1).let {
                it.copy(libraryManga = it.libraryManga.copy(bookmarkCount = 1))
            },
            libraryItem(manga(3).copy(ogGenre = listOf("hentai"))),
        )
    }

    @Before
    fun setUp() {
        startKoin { modules(harness.koinModules()) }
    }

    @After
    fun tearDown() = stopKoin()

    private fun filtered(
        prefs: LibraryScreenModel.ItemPreferences = itemPreferences(),
        tracks: Map<Long, TriState> = emptyMap(),
    ): List<Long> = with(pipeline) {
        items.applyFilters(mapOf(1L to listOf(domainTrack(trackerId = 7L))), tracks, prefs).map { it.id }
    }

    @Test
    fun noFiltersKeepEverything() {
        filtered() shouldBe listOf(1L, 2L, 3L)
    }

    @Test
    fun downloadedMeansLocalOrDownloaded() {
        filtered(itemPreferences(filterDownloaded = TriState.ENABLED_IS)) shouldBe listOf(1L, 2L)
        filtered(itemPreferences(filterDownloaded = TriState.ENABLED_NOT)) shouldBe listOf(3L)
        filtered(itemPreferences(globalFilterDownloaded = true)) shouldBe listOf(1L, 2L)
    }

    @Test
    fun readingStateFilters() {
        filtered(itemPreferences(filterUnread = TriState.ENABLED_IS)) shouldBe listOf(1L)
        filtered(itemPreferences(filterStarted = TriState.ENABLED_IS)) shouldBe listOf(2L)
        filtered(itemPreferences(filterBookmarked = TriState.ENABLED_IS)) shouldBe listOf(2L)
        filtered(itemPreferences(filterCompleted = TriState.ENABLED_IS)) shouldBe listOf(1L)
    }

    @Test
    fun excludingReadingStates() {
        filtered(itemPreferences(filterUnread = TriState.ENABLED_NOT)) shouldBe listOf(2L, 3L)
        filtered(itemPreferences(filterStarted = TriState.ENABLED_NOT)) shouldBe listOf(1L, 3L)
        filtered(itemPreferences(filterBookmarked = TriState.ENABLED_NOT)) shouldBe listOf(1L, 3L)
        filtered(itemPreferences(filterCompleted = TriState.ENABLED_NOT)) shouldBe listOf(2L, 3L)
    }

    @Test
    fun intervalOnlyWhenRestricted() {
        filtered(itemPreferences(filterIntervalCustom = TriState.ENABLED_IS)) shouldBe listOf(1L, 2L, 3L)
        val restricted = itemPreferences(filterIntervalCustom = TriState.ENABLED_IS, skipOutsideReleasePeriod = true)
        filtered(restricted) shouldBe listOf(1L)
        filtered(restricted.copy(filterIntervalCustom = TriState.ENABLED_NOT)) shouldBe listOf(2L, 3L)
        filtered(restricted.copy(filterIntervalCustom = TriState.DISABLED)) shouldBe listOf(1L, 2L, 3L)
    }

    @Test
    fun defaultsComeFromInjekt() {
        val fromInjekt = LibraryItemPipeline(harness.basePreferences, harness.libraryPreferences)
        with(fromInjekt) { items.applyFilters(emptyMap(), emptyMap(), itemPreferences()).size shouldBe 3 }
    }

    @Test
    fun lewdFilter() {
        filtered(itemPreferences(filterLewd = TriState.ENABLED_IS)) shouldBe listOf(3L)
        filtered(itemPreferences(filterLewd = TriState.ENABLED_NOT)) shouldBe listOf(1L, 2L)
    }

    @Test
    fun trackerFilters() {
        filtered(tracks = mapOf(7L to TriState.DISABLED)) shouldBe listOf(1L, 2L, 3L)
        filtered(tracks = mapOf(7L to TriState.ENABLED_IS)) shouldBe listOf(1L)
        filtered(tracks = mapOf(7L to TriState.ENABLED_NOT)) shouldBe listOf(2L, 3L)
        filtered(tracks = mapOf(7L to TriState.ENABLED_NOT, 8L to TriState.ENABLED_IS)) shouldBe emptyList()
    }
}

internal fun itemPreferences(
    globalFilterDownloaded: Boolean = false,
    filterDownloaded: TriState = TriState.DISABLED,
    filterUnread: TriState = TriState.DISABLED,
    filterStarted: TriState = TriState.DISABLED,
    filterBookmarked: TriState = TriState.DISABLED,
    filterCompleted: TriState = TriState.DISABLED,
    filterIntervalCustom: TriState = TriState.DISABLED,
    filterLewd: TriState = TriState.DISABLED,
    skipOutsideReleasePeriod: Boolean = false,
): LibraryScreenModel.ItemPreferences = LibraryScreenModel.ItemPreferences(
    downloadBadge = true,
    unreadBadge = true,
    localBadge = true,
    languageBadge = true,
    skipOutsideReleasePeriod = skipOutsideReleasePeriod,
    globalFilterDownloaded = globalFilterDownloaded,
    filterDownloaded = filterDownloaded,
    filterUnread = filterUnread,
    filterStarted = filterStarted,
    filterBookmarked = filterBookmarked,
    filterCompleted = filterCompleted,
    filterIntervalCustom = filterIntervalCustom,
    filterLewd = filterLewd,
)

