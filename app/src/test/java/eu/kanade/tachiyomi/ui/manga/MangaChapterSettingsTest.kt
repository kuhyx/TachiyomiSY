package eu.kanade.tachiyomi.ui.manga

import io.mockk.coVerify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class MangaChapterSettingsTest {
    private val harness = MangaHarness()
    private val flags get() = harness.parts.setMangaChapterFlags

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun loadingStateChangesNothing() {
        val settings = harness.loading().chapterSettings
        settings.setUnreadFilter(TriState.ENABLED_IS)
        settings.setDownloadedFilter(TriState.ENABLED_IS)
        settings.setBookmarkedFilter(TriState.ENABLED_IS)
        settings.setDisplayMode(1L)
        settings.setSorting(1L)
        settings.setCurrentSettingsAsDefault(applyToExisting = true)
        settings.resetToDefaultSettings()
        coVerify(exactly = 0) { flags.awaitSetUnreadFilter(any(), any()) }
    }

    @Test
    fun unreadFilterMapsEveryState() {
        val settings = harness.loaded().chapterSettings
        settings.setUnreadFilter(TriState.DISABLED)
        settings.setUnreadFilter(TriState.ENABLED_IS)
        settings.setUnreadFilter(TriState.ENABLED_NOT)
        coVerify(timeout = 5_000) { flags.awaitSetUnreadFilter(any(), Manga.SHOW_ALL) }
        coVerify(timeout = 5_000) { flags.awaitSetUnreadFilter(any(), Manga.CHAPTER_SHOW_UNREAD) }
        coVerify(timeout = 5_000) { flags.awaitSetUnreadFilter(any(), Manga.CHAPTER_SHOW_READ) }
    }

    @Test
    fun downloadFilterMapsEveryState() {
        val settings = harness.loaded().chapterSettings
        settings.setDownloadedFilter(TriState.DISABLED)
        settings.setDownloadedFilter(TriState.ENABLED_IS)
        settings.setDownloadedFilter(TriState.ENABLED_NOT)
        coVerify(timeout = 5_000) { flags.awaitSetDownloadedFilter(any(), Manga.SHOW_ALL) }
        coVerify(timeout = 5_000) { flags.awaitSetDownloadedFilter(any(), Manga.CHAPTER_SHOW_DOWNLOADED) }
        coVerify(timeout = 5_000) { flags.awaitSetDownloadedFilter(any(), Manga.CHAPTER_SHOW_NOT_DOWNLOADED) }
    }

    @Test
    fun bookmarkFilterMapsEveryState() {
        val settings = harness.loaded().chapterSettings
        settings.setBookmarkedFilter(TriState.DISABLED)
        settings.setBookmarkedFilter(TriState.ENABLED_IS)
        settings.setBookmarkedFilter(TriState.ENABLED_NOT)
        coVerify(timeout = 5_000) { flags.awaitSetBookmarkFilter(any(), Manga.SHOW_ALL) }
        coVerify(timeout = 5_000) { flags.awaitSetBookmarkFilter(any(), Manga.CHAPTER_SHOW_BOOKMARKED) }
        coVerify(timeout = 5_000) { flags.awaitSetBookmarkFilter(any(), Manga.CHAPTER_SHOW_NOT_BOOKMARKED) }
    }

    @Test
    fun displayAndSortArePersisted() {
        val settings = harness.loaded().chapterSettings
        settings.setDisplayMode(Manga.CHAPTER_DISPLAY_NUMBER)
        settings.setSorting(Manga.CHAPTER_SORTING_NUMBER)
        settings.setExcludedScanlators(setOf("x"))
        coVerify(timeout = 5_000) { flags.awaitSetDisplayMode(any(), Manga.CHAPTER_DISPLAY_NUMBER) }
        coVerify(timeout = 5_000) { flags.awaitSetSortingModeOrFlipOrder(any(), Manga.CHAPTER_SORTING_NUMBER) }
        coVerify(timeout = 5_000) { harness.parts.setExcludedScanlators.await(1L, setOf("x")) }
    }

    @Test
    fun defaultsAreSavedAndApplied() {
        val model = harness.loaded()
        model.chapterSettings.setCurrentSettingsAsDefault(applyToExisting = false)
        eventually { model.snackbarHostState.currentSnackbarData != null }
        model.snackbarHostState.currentSnackbarData?.dismiss()
        coVerify(exactly = 0) { harness.parts.setMangaDefaultChapterFlags.awaitAll() }
        model.chapterSettings.setCurrentSettingsAsDefault(applyToExisting = true)
        coVerify(timeout = 5_000) { harness.parts.setMangaDefaultChapterFlags.awaitAll() }
        model.chapterSettings.resetToDefaultSettings()
        coVerify(timeout = 5_000) { harness.parts.setMangaDefaultChapterFlags.await(any()) }
    }
}
