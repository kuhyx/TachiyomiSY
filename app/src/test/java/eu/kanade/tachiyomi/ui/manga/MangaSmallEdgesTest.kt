package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.source.online.all.MergedSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.CustomMangaInfo

/** Edits without tags, a default category that no longer exists, and merged downloads without member data. */
@RunWith(RobolectricTestRunner::class)
internal class MangaSmallEdgesTest {
    private val harness = MangaHarness()

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga() to listOf(chapter(1L))
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun editWithoutTagsKeepsGenre() {
        val model = harness.loaded()
        model.updateMangaInfo("T", null, null, null, null, null, null)
        verify { harness.parts.setCustomMangaInfo.set(CustomMangaInfo(1L, "T", null, null, null, null, null, null)) }
    }

    @Test
    fun missingDefaultCategory() {
        coEvery { harness.parts.getDuplicateLibraryManga(any()) } returns emptyList()
        coEvery { harness.parts.getCategories.await() } returns emptyList()
        coEvery { harness.updateManga.awaitUpdateFavorite(1L, true) } returns true
        harness.libraryPreferences.defaultCategory.set(5)
        harness.loaded().toggleFavorite()
        coVerify(timeout = 5_000) { harness.parts.setMangaCategories.await(1L, emptyList()) }
    }

    @Test
    fun mergedDownloadNeedsMembers() {
        val model = harness.loaded()
        harness.awaitObserver(model)
        model.updateSuccessState { it.copy(source = mockk<MergedSource>(relaxed = true), mergedData = null) }
        model.downloads.downloadChapters(listOf(chapter(1L)))
        verify(exactly = 0) { harness.downloadManager.downloadChapters(any(), any(), any()) }
        model.awaitSuccess().mergedData shouldBe null
    }
}
