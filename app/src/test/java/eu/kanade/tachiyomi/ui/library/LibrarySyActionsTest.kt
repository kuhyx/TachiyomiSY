package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.ui.manga.eventually
import exh.favorites.FavoritesSyncStatus
import exh.md.utils.FollowStatus
import exh.md.utils.MdUtil
import exh.md.utils.getEnabledMangaDex
import exh.recs.batch.SearchStatus
import exh.source.EH_SOURCE_ID
import exh.source.mangaDexSourceIds
import exh.source.nHentaiSourceIds
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.CustomMangaInfo

@RunWith(RobolectricTestRunner::class)
internal class LibrarySyActionsTest {
    private val harness = LibraryHarness()

    @Before
    fun setUp() {
        harness.start()
        harness.categories.value = listOf(libCategory(1L))
    }

    @After
    fun tearDown() {
        harness.stop()
        unmockkAll()
    }

    @Test
    fun cleanTitlesStripsDecorations() {
        val decorated = libManga(1L, "[Group] Name (Extra) {x}", source = EH_SOURCE_ID)
        val piped = libManga(2L, "Circle | Real", source = EH_SOURCE_ID)
        val clean = libManga(3L, "Clean", source = EH_SOURCE_ID)
        val bracketsOnly = libManga(4L, "[Only]", source = EH_SOURCE_ID)
        val plain = libManga(5L, "[Plain]")
        val entries = listOf(decorated, piped, clean, bracketsOnly, plain).map { libEntry(it) }
        val model = harness.selecting(*entries.toTypedArray())
        model.cleanTitles()
        verify { harness.setCustomMangaInfo.set(CustomMangaInfo(id = 1L, title = "Name")) }
        verify { harness.setCustomMangaInfo.set(CustomMangaInfo(id = 2L, title = "Real")) }
        verify { harness.setCustomMangaInfo.set(CustomMangaInfo(id = 4L, title = null)) }
        verify(exactly = 3) { harness.setCustomMangaInfo.set(any()) }
        model.state.value.selection.shouldBeEmpty()
    }

    @Test
    fun cleanTitlesForNhentai() {
        val previous = nHentaiSourceIds
        try {
            nHentaiSourceIds = listOf(55L)
            harness.selecting(libEntry(libManga(1L, "[G] Doujin", source = 55L))).cleanTitles()
            verify { harness.setCustomMangaInfo.set(CustomMangaInfo(id = 1L, title = "Doujin")) }
        } finally {
            nHentaiSourceIds = previous
        }
    }

    @Test
    fun cleanTitlesKeepsOtherEdits() {
        val edited = spyk(libManga(1L, "[G] Name", source = EH_SOURCE_ID)) {
            every { author } returns "a"
            every { artist } returns "b"
            every { thumbnailUrl } returns "c"
            every { description } returns "d"
            every { genre } returns listOf("e")
            every { status } returns 2L
        }
        harness.selecting(libEntry(edited)).cleanTitles()
        val expected = CustomMangaInfo(
            id = 1L,
            title = "Name",
            author = "a",
            artist = "b",
            thumbnailUrl = "c",
            description = "d",
            genre = listOf("e"),
            status = 2L,
        )
        verify { harness.setCustomMangaInfo.set(expected) }
    }

    @Test
    fun mangaDexNeedsAnEnabledSource() {
        mockkStatic("exh.md.utils.MdSourcesKt")
        every { MdUtil.getEnabledMangaDex(any(), any()) } returns null
        val model = harness.selecting(libEntry(libManga(1L)))
        model.syncMangaToDex()
        model.await { it.selection.isEmpty() }
    }

    @Test
    fun mangaDexFollowsTheSelection() {
        val previous = mangaDexSourceIds
        try {
            mangaDexSourceIds = listOf(66L)
            mockkStatic("exh.md.utils.MdSourcesKt")
            val dex = mockk<MangaDex> { coEvery { updateFollowStatus(any(), any()) } returns true }
            every { MdUtil.getEnabledMangaDex(any(), any()) } returns dex
            val model = harness.selecting(libEntry(libManga(1L, source = 66L)), libEntry(libManga(2L)))
            model.syncMangaToDex()
            model.await { it.selection.isEmpty() }
            coVerify(exactly = 1) { dex.updateFollowStatus("1", FollowStatus.READING) }
        } finally {
            mangaDexSourceIds = previous
        }
    }

    @Test
    fun firstUnreadIsTheNextChapter() {
        val chapter = Chapter.create().copy(id = 4L)
        coEvery { harness.getNextChapters.await(1L, any<Boolean>()) } returns listOf(chapter)
        val model = harness.model()
        runBlocking { model.getFirstUnread(libManga(1L)) } shouldBe chapter
        runBlocking { model.getFirstUnread(libManga(2L)) }.shouldBeNull()
    }

    @Test
    fun recommendationSearchRunsOnce() {
        val model = harness.model()
        model.recommendationSearch.status.value = SearchStatus.Initializing
        model.runRecommendationSearch(emptyList())
        model.recommendationSearchJob.shouldBeNull()
        model.cancelRecommendationSearch()
        model.recommendationSearch.status.value = SearchStatus.Idle
        model.runRecommendationSearch(emptyList())
        eventually { model.recommendationSearch.status.value == SearchStatus.Finished.WithoutResults }
        model.cancelRecommendationSearch()
        model.recommendationSearchJob!!.isCancelled shouldBe false
    }

    @Test
    fun favoritesSyncNeedsExhentai() {
        val model = harness.model()
        model.runSync()
        eventually { model.favoritesSync.status.value == FavoritesSyncStatus.SyncError.NotLoggedInSyncError }
        model.onAcceptSyncWarning()
        harness.exhPreferences.exhShowSyncIntro.get() shouldBe false
    }
}
