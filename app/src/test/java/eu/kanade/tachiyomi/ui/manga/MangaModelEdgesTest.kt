package eu.kanade.tachiyomi.ui.manga

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.tachiyomi.data.track.domainTrack
import io.kotest.matchers.nulls.shouldBeNull
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** The manga screen model's less travelled paths: swipes, tracking order, the automatic category, unfavouring. */
@RunWith(RobolectricTestRunner::class)
internal class MangaModelEdgesTest {
    private val harness = MangaHarness()
    private val parts get() = harness.parts
    private val done = AtomicBoolean()

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L), chapter(2L), chapter(3L))
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun swipeReadsUnreadChapter() {
        coEvery { parts.setReadStatus.await(read = true, chapters = anyVararg()) } answers {
            done.set(true)
            SetReadStatus.Result.Success
        }
        harness.loaded().chapterActions.chapterSwipe(
            item(chapter(2L)),
            LibraryPreferences.ChapterSwipeAction.ToggleRead,
        )
        eventually { done.get() }
    }

    @Test
    fun highestReadChapterIsTracked() {
        val model = harness.loaded()
        model.updateSuccessState { it.copy(hasLoggedInTrackers = true) }
        model.autoTrackState = AutoTrackState.ALWAYS
        coEvery { parts.getTracks.await(1L) } returns listOf(domainTrack(id = 1L).copy(lastChapterRead = 1.0))
        coEvery { parts.refreshTracks.await(1L) } returns emptyList()
        coEvery { parts.trackChapter.await(any(), 1L, 3.0) } answers { done.set(true) }
        // Out of order, so the highest number is not the last one read.
        model.chapterActions.markChaptersRead(listOf(chapter(3L), chapter(2L)), read = true)
        eventually { done.get() }
    }

    @Test
    fun automaticCategoryFilesNowhere() {
        val user = mockk<Category> {
            every { id } returns 5L
            every { isSystemCategory } returns false
        }
        harness.mangaFlow.value = manga() to listOf(chapter(1L))
        coEvery { parts.getDuplicateLibraryManga(any()) } returns emptyList()
        coEvery { parts.getCategories.await() } returns listOf(user)
        coEvery { harness.updateManga.awaitUpdateFavorite(1L, true) } returns true
        coEvery { parts.setMangaCategories.await(1L, emptyList()) } answers { done.set(true) }
        harness.libraryPreferences.defaultCategory.set(0)
        harness.loaded().toggleFavorite()
        eventually { done.get() }
    }

    @Test
    fun unfavouriteWithoutDownloads() {
        coEvery { harness.updateManga.awaitUpdateFavorite(1L, false) } returns true
        val model = harness.loaded()
        every { harness.downloadManager.getDownloadCount(any<Manga>()) } answers {
            done.set(true)
            0
        }
        model.toggleFavorite()
        eventually { done.get() }
        // Nothing to delete, so no offer to.
        model.snackbarHostState.currentSnackbarData.shouldBeNull()
    }

    @Test
    fun cancelledLoadSkipsTheFetch() {
        harness.mangaFlow.value = manga(favorite = true) to emptyList()
        val built = CountDownLatch(1)
        val model = AtomicReference<MangaScreenModel>()
        // The screen closes while the first state is being built: no refresh follows.
        coEvery { harness.getExcludedScanlators.await(1L) } answers {
            built.await()
            model.get().screenModelScope.cancel()
            emptySet()
        }
        model.set(harness.model())
        built.countDown()
        model.get().awaitSuccess { !it.isRefreshingData }
        coVerify(exactly = 0) {
            harness.updateMangaFromRemote(
                source = any(),
                manga = any(),
                fetchDetails = any(),
                fetchChapters = any(),
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        }
    }
}
