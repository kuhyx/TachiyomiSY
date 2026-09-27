package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Favouriting through the library helper's defaults, and a stale default category with no categories left. */
@RunWith(RobolectricTestRunner::class)
internal class MangaLibraryDefaultsTest {
    private val harness = MangaHarness()
    private val parts get() = harness.parts

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga() to listOf(chapter(1L))
        coEvery { parts.getDuplicateLibraryManga(any()) } returns emptyList()
        coEvery { harness.updateManga.awaitUpdateFavorite(any(), any()) } returns true
        coEvery { parts.getCategories.await() } returns emptyList()
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun duplicatesCheckedByDefault() {
        val model = harness.loaded()
        model.library.toggleFavorite(onRemoved = {})
        coVerify(timeout = 5_000) { parts.getDuplicateLibraryManga(any()) }
        coVerify(timeout = 5_000) { parts.setMangaCategories.await(1L, emptyList()) }
    }

    @Test
    fun staleDefaultWithoutCategories() {
        harness.libraryPreferences.defaultCategory.set(9)
        harness.loaded().toggleFavorite()
        coVerify(timeout = 5_000) { parts.setMangaCategories.await(1L, emptyList()) }
    }

    @Test
    fun itemSelectionDefaultsOff() {
        val item = ChapterList.Item(
            chapter = chapter(1L),
            downloadState = Download.State.QUEUE,
            downloadProgress = 3,
            sourceName = null,
            showScanlator = true,
        )
        item.selected shouldBe false
        item.isDownloaded shouldBe false
    }
}
