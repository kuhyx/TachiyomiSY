package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import mihon.domain.source.models.RemoteMangaUpdate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class MangaScreenModelTest {
    private val harness = MangaHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun remote(result: Result<RemoteMangaUpdate>) {
        coEvery {
            harness.updateMangaFromRemote(
                source = any(),
                manga = any(),
                fetchDetails = any(),
                fetchChapters = any(),
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } returns result
    }

    @Test
    fun loadsTheEntryAndChapters() {
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L), chapter(2L, read = true))
        val model = harness.loaded()
        val state = model.awaitSuccess { it.chapters.size == 2 }
        model.manga?.id shouldBe 1L
        model.source shouldBe harness.source
        model.isFavorited shouldBe true
        model.allChapters?.map { it.id } shouldContainExactly listOf(1L, 2L)
        model.filteredChapters?.map { it.id } shouldContainExactly listOf(2L, 1L)
        state.isAnySelected shouldBe false
        state.chapterListItems.size shouldBe 2
        state.filterActive shouldBe false
        state.showMergeWithAnother shouldBe false
        state.pagePreviewsState shouldBe PagePreviewState.Unused
        coVerify(exactly = 0) { harness.parts.setMangaDefaultChapterFlags.await(any()) }
    }

    @Test
    fun loadingModelHasNoEntry() {
        harness.mangaFlow.value = manga() to listOf(chapter(1L))
        val model = harness.loading()
        model.manga.shouldBeNull()
        model.source.shouldBeNull()
        model.isFavorited shouldBe false
        model.allChapters.shouldBeNull()
        model.filteredChapters.shouldBeNull()
        model.updateSuccessState { error("not called while loading") }
        model.state.value shouldBe MangaScreenModel.State.Loading
    }

    @Test
    fun scanlatorFilterMarksActive() {
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        val model = harness.loaded()
        harness.scanlators.value = setOf("a", "b")
        harness.excluded.value = setOf("a")
        val state = model.awaitSuccess { it.excludedScanlators == setOf("a") && it.availableScanlators.size == 2 }
        state.scanlatorFilterActive shouldBe true
        state.filterActive shouldBe true
    }

    @Test
    fun chapterFilterMarksActive() {
        harness.mangaFlow.value = manga(flags = Manga.CHAPTER_SHOW_UNREAD, favorite = true) to listOf(chapter(1L))
        val state = harness.loaded().awaitSuccess()
        state.scanlatorFilterActive shouldBe false
        state.filterActive shouldBe true
    }

    @Test
    fun emptyEntryFetchesFromSource() {
        harness.mangaFlow.value = manga().copy(initialized = false) to emptyList()
        remote(Result.success(RemoteMangaUpdate(manga(), listOf(chapter(3L)))))
        harness.loaded()
        coVerify { harness.parts.setMangaDefaultChapterFlags.await(any()) }
        coVerify {
            harness.updateMangaFromRemote(
                source = harness.source,
                manga = any(),
                fetchDetails = true,
                fetchChapters = true,
                manualFetch = false,
                fetchWindow = any(),
                throttleFunc = any(),
            )
        }
    }

    @Test
    fun manualFetchDownloadsNew() {
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        coEvery { harness.parts.filterChaptersForDownload.await(any(), any()) } returns emptyList()
        remote(Result.success(RemoteMangaUpdate(manga(), listOf(chapter(3L)))))
        val model = harness.loaded()
        model.fetchAllFromSource()
        eventually { !model.awaitSuccess().isRefreshingData }
        coVerify { harness.parts.filterChaptersForDownload.await(any(), listOf(chapter(3L))) }
    }

    @Test
    fun fetchFailuresShowASnackbar() {
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        val model = harness.loaded()
        remote(Result.failure(NoChaptersException()))
        model.fetchAllFromSource(manualFetch = false)
        eventually { model.snackbarHostState.currentSnackbarData != null }
        model.snackbarHostState.currentSnackbarData?.dismiss()
        remote(Result.failure(IllegalStateException("boom")))
        model.fetchAllFromSource()
        val message = "IllegalStateException: boom"
        eventually { model.snackbarHostState.currentSnackbarData?.visuals?.message == message }
    }

    @Test
    fun cancelledFetchIsIgnored() {
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        val model = harness.loaded()
        remote(Result.failure(CancellationException("stop")))
        model.fetchAllFromSource()
        eventually { !model.awaitSuccess().isRefreshingData }
        model.snackbarHostState.currentSnackbarData.shouldBeNull()
    }

    @Test
    fun downloadStateReachesTheList() {
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        val model = harness.loaded()
        val download = mockk<Download>(relaxed = true) {
            every { chapter } returns chapter(1L)
            every { status } returns Download.State.DOWNLOADED
            every { progress } returns 100
        }
        model.downloads.updateDownloadState(download)
        model.awaitSuccess().chapters.single().downloadState shouldBe Download.State.DOWNLOADED
        val stranger = mockk<Download>(relaxed = true) { every { chapter } returns chapter(9L) }
        model.downloads.updateDownloadState(stranger)
        model.awaitSuccess().chapters.single().downloadProgress shouldBe 100
    }
}
