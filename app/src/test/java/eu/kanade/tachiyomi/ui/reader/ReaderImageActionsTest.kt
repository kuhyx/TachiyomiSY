package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.data.saver.Location
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SaveImageResult
import eu.kanade.tachiyomi.ui.reader.viewer.pager.L2RPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ReaderShadowDecoder::class, ReaderShadowDecoderCompanion::class],
)
internal class ReaderImageActionsTest {

    private val fixture = ReaderImageFixture()
    private val actions get() = fixture.vm.images

    @Before
    fun setUp() {
        fixture.start()
    }

    @After
    fun tearDown() {
        fixture.stop()
    }

    private fun nothingSaved() {
        verify(exactly = 0) { anyConstructed<SaveImageNotifier>().onClear() }
    }

    @Test
    fun saveNeedsReadyPageAndManga() {
        actions.saveImage(useExtraPage = false)
        fixture.select(ready = false)
        actions.saveImage(useExtraPage = false)
        fixture.select(extraPage = false)
        actions.saveImage(useExtraPage = true)
        fixture.vm.updateState { it.copy(manga = null) }
        actions.saveImage(useExtraPage = false)
        nothingSaved()
    }

    @Test
    fun saveReportsTheResult() {
        fixture.select()
        fixture.harness.readerPreferences.folderPerManga.set(true)
        val saved = fixture.event { actions.saveImage(useExtraPage = true) } as Event.SavedImage
        (saved.result as SaveImageResult.Success).uri shouldBe fixture.savedUri
        verify { anyConstructed<SaveImageNotifier>().onComplete(fixture.savedUri) }
        fixture.harness.readerPreferences.folderPerManga.set(false)
        every { fixture.harness.imageSaver.save(any()) } throws IOException("full")
        val failed = fixture.event { actions.saveImage(useExtraPage = false) } as Event.SavedImage
        failed.result.shouldBeInstanceOf<SaveImageResult.Error>()
        verify { anyConstructed<SaveImageNotifier>().onError("full") }
    }

    @Test
    fun spreadNeedsBothPages() {
        actions.saveImages()
        fixture.select()
        actions.saveImages()
        fixture.viewer(mockk<L2RPagerViewer>(relaxed = true))
        fixture.pages[0].status = Page.State.Queue
        actions.saveImages()
        fixture.select(extraPage = false)
        actions.saveImages()
        fixture.select()
        fixture.pages[1].status = Page.State.Queue
        actions.saveImages()
        fixture.select()
        fixture.vm.updateState { it.copy(manga = null) }
        actions.saveImages()
        nothingSaved()
    }

    @Test
    fun spreadIsMergedAndSaved() {
        fixture.select()
        fixture.viewer(mockk<R2LPagerViewer>(relaxed = true))
        val saved = fixture.event { actions.saveImages() } as Event.SavedImage
        (saved.result as SaveImageResult.Success).uri shouldBe fixture.savedUri
        fixture.viewer(mockk<L2RPagerViewer>(relaxed = true))
        every { fixture.harness.imageSaver.save(any()) } throws IOException("full")
        val failed = fixture.event { actions.saveImages() } as Event.SavedImage
        failed.result.shouldBeInstanceOf<SaveImageResult.Error>()
    }

    @Test
    fun mergeRejectsNonImages() {
        val (first, second) = fixture.pages
        fun merge() = actions.saveImages(
            page1 = first,
            page2 = second,
            isLTR = true,
            bg = 0,
            location = Location.Cache,
            manga = fixture.harness.manga,
        )
        second.stream = { "text".byteInputStream() }
        shouldThrow<IllegalArgumentException> { merge() }
        first.stream = { "text".byteInputStream() }
        shouldThrow<IllegalArgumentException> { merge() }
        // A PNG signature over nothing decodable: the sniffing passes, decoding does not.
        first.stream = { pngBytes().copyOf(8).inputStream() }
        second.stream = { pngBytes().inputStream() }
        shouldThrow<NullPointerException> { merge() }
        first.stream = second.stream
        second.stream = { pngBytes().copyOf(8).inputStream() }
        shouldThrow<NullPointerException> { merge() }
    }

    @Test
    fun filenameKeepsThePageNumber() {
        val page = fixture.pages[1]
        actions.generateFilename(fixture.harness.manga, page).endsWith(" - 2") shouldBe true
    }
}
