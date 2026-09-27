package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.releaseLogcat
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.base.customInfoModule
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Dialog
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.image.LocalCoverManager
import java.io.ByteArrayInputStream

/** Saving, sharing and copying one page image, and setting it as the cover. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderImageActionsTest {
    private val harness = ReaderVmHarness(ApplicationProvider.getApplicationContext<Application>())
    private val coverCache = mockk<CoverCache>(relaxed = true)
    private val saved: Uri = Uri.parse("content://saved/1")

    @Before
    fun setUp() {
        harness.start(
            // A favourite Manga fixture resolves its custom info through Koin.
            customInfoModule(),
            module {
                single { coverCache }
                single { mockk<LocalCoverManager>(relaxed = true) }
                single { mockk<UpdateManga>(relaxed = true) }
            },
        )
        every { harness.imageSaver.save(any()) } returns saved
    }

    @After
    fun tearDown() = harness.stop()

    private fun ready(index: Int = 0): ReaderPage {
        val page = loadedPages(readerChapter(), 2)[index]
        page.stream = { ByteArrayInputStream(byteArrayOf(1, 2)) }
        page.status = Page.State.Ready
        return page
    }

    private fun ReaderViewModel.withPages(page: ReaderPage, extra: ReaderPage? = null, manga: Manga? = harness.manga) =
        also { vm -> vm.updateState { it.copy(manga = manga, dialog = Dialog.PageActions(page, extra)) } }

    private fun ReaderViewModel.nextEvent(): Event = runBlocking { withTimeout(5_000) { eventFlow.first() } }

    @Test
    fun savedPageIsReported() {
        harness.readerPreferences.folderPerManga.set(true)
        val vm = harness.viewModel().withPages(ready())
        vm.images.saveImage(useExtraPage = false)
        val result = vm.nextEvent().shouldBeInstanceOf<Event.SavedImage>().result
        result.shouldBeInstanceOf<ReaderViewModel.SaveImageResult.Success>().uri shouldBe saved
    }

    @Test
    fun failedSaveIsReported() {
        every { harness.imageSaver.save(any()) } throws IllegalStateException("full")
        val vm = harness.viewModel().withPages(ready(), extra = ready(1))
        vm.images.saveImage(useExtraPage = true)
        val result = vm.nextEvent().shouldBeInstanceOf<Event.SavedImage>().result
        result.shouldBeInstanceOf<ReaderViewModel.SaveImageResult.Error>().error.message shouldBe "full"
    }

    @Test
    fun unreadyPagesAreIgnored() {
        val vm = harness.viewModel()
        vm.images.saveImage(useExtraPage = false)
        vm.images.shareImage(copyToClipboard = false, useExtraPage = true)
        vm.images.setAsCover(useExtraPage = false)
        vm.withPages(loadedPages(readerChapter(), 1)[0])
        vm.images.saveImage(useExtraPage = false)
        vm.images.shareImage(copyToClipboard = false, useExtraPage = false)
        vm.images.setAsCover(useExtraPage = false)
        vm.withPages(ready(), manga = null)
        vm.images.saveImage(useExtraPage = false)
        vm.images.shareImage(copyToClipboard = true, useExtraPage = false)
        vm.images.setAsCover(useExtraPage = false)
        verify(exactly = 0) { harness.imageSaver.save(any()) }
    }

    @Test
    fun sharedOrCopiedPage() {
        val page = ready()
        val vm = harness.viewModel().withPages(page)
        vm.images.shareImage(copyToClipboard = false, useExtraPage = false)
        vm.nextEvent() shouldBe Event.ShareImage(saved, page)
        vm.images.shareImage(copyToClipboard = true, useExtraPage = false)
        vm.nextEvent() shouldBe Event.CopyImage(saved)
    }

    @Test
    fun failedShareIsOnlyLogged() {
        val logged = captureLogcat()
        try {
            every { harness.imageSaver.save(any()) } throws IllegalStateException("cache full")
            val vm = harness.viewModel().withPages(ready())
            vm.images.shareImage(copyToClipboard = false, useExtraPage = false)
            // The failure is caught inside the coroutine: logged, no event, and the scope still works.
            awaitUntil { logged.any { "cache full" in it } }
            every { harness.imageSaver.save(any()) } returns saved
            vm.images.shareImage(copyToClipboard = true, useExtraPage = false)
            vm.nextEvent() shouldBe Event.CopyImage(saved)
        } finally {
            releaseLogcat()
        }
    }

    @Test
    fun coverNeedsTheLibrary() {
        val vm = harness.viewModel().withPages(ready())
        vm.images.setAsCover(useExtraPage = false)
        vm.nextEvent() shouldBe Event.SetCoverResult(SetAsCoverResult.AddToLibraryFirst)
        vm.withPages(ready(), manga = harness.manga.copy(favorite = true))
        vm.images.setAsCover(useExtraPage = false)
        vm.nextEvent() shouldBe Event.SetCoverResult(SetAsCoverResult.Success)
        vm.withPages(ready(), manga = harness.manga.copy(source = 0L))
        vm.images.setAsCover(useExtraPage = false)
        vm.nextEvent() shouldBe Event.SetCoverResult(SetAsCoverResult.Success)
    }

    @Test
    fun coverFailureIsReported() {
        every { coverCache.setCustomCoverToCache(any(), any()) } throws IllegalStateException("disk")
        val vm = harness.viewModel().withPages(ready(), manga = harness.manga.copy(favorite = true))
        vm.images.setAsCover(useExtraPage = false)
        vm.nextEvent() shouldBe Event.SetCoverResult(SetAsCoverResult.Error)
    }

    @Test
    fun streamlessPageIsNoCover() {
        val page = ready().also { it.stream = null }
        val vm = harness.viewModel().withPages(page)
        vm.images.setAsCover(useExtraPage = false)
        vm.images.generateFilename(harness.manga, ready()) shouldBe "Manga - Chapter 1 - 1"
    }
}
