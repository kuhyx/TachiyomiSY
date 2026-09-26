package eu.kanade.tachiyomi.ui.reader

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.saver.Image
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Dialog
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.source.local.image.LocalCoverManager

/**
 * A loaded [ReaderViewModel] over [ReaderVmHarness] with two pages of chapter 1 that stream a real
 * PNG; the image saver answers [savedUri] after reading the image it is given, and the save
 * notifier and the cover dependencies are stubbed.
 */
internal class ReaderImageFixture {
    val harness: ReaderVmHarness = ReaderVmHarness(ApplicationProvider.getApplicationContext())
    val savedUri: Uri = Uri.parse("file:///saved.png")
    lateinit var vm: ReaderViewModel
    lateinit var pages: List<ReaderPage>
    val coverManager: LocalCoverManager = mockk(relaxed = true)
    val updateManga: UpdateManga = mockk(relaxed = true)
    val coverCache: CoverCache = mockk(relaxed = true)
    private val customInfo: CustomMangaRepository = mockk()

    fun start() {
        every { customInfo.get(any()) } returns null
        harness.start(
            module {
                single { coverManager }
                single { updateManga }
                single { coverCache }
                single { GetCustomMangaInfo(customInfo) }
            },
        )
        mockkConstructor(SaveImageNotifier::class)
        every { anyConstructed<SaveImageNotifier>().onClear() } just Runs
        every { anyConstructed<SaveImageNotifier>().onComplete(any()) } just Runs
        every { anyConstructed<SaveImageNotifier>().onError(any()) } just Runs
        every { harness.imageSaver.save(any()) } answers {
            (firstArg<Image>() as Image.Page).inputStream().close()
            savedUri
        }
        vm = harness.loadedViewModel()
        pages = loadedPages(readerChapter(), count = 2)
        pages.forEach { it.stream = { pngBytes().inputStream() } }
    }

    fun stop() {
        harness.stop()
    }

    /** Marks every page ready and opens the page-actions dialog on them. */
    fun select(extraPage: Boolean = true, ready: Boolean = true) {
        if (ready) pages.forEach { it.status = Page.State.Ready }
        vm.updateState {
            it.copy(dialog = Dialog.PageActions(pages[0], pages[1].takeIf { extraPage }))
        }
    }

    fun viewer(viewer: Viewer?) {
        vm.updateState { it.copy(viewer = viewer) }
    }

    /** The first event [action] sends. */
    fun event(action: () -> Unit): Event = runBlocking {
        withTimeout(TIMEOUT_MILLIS) {
            val event = async(start = CoroutineStart.UNDISPATCHED) { vm.eventFlow.first() }
            action()
            event.await()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
