package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.tachiyomi.data.saver.Image
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Dialog
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SaveImageResult
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.pager.L2RPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerDecodes
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerShadowDecoder
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerShadowDecoderCompanion
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.core.common.util.system.ImageUtil
import java.io.ByteArrayInputStream
import java.io.InputStream

/** Saving, sharing and copying the two pages of a spread as one joined image. */
@RunWith(RobolectricTestRunner::class)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [PagerShadowDecoder::class, PagerShadowDecoderCompanion::class],
)
internal class ReaderSpreadActionsTest {
    private val harness = ReaderVmHarness(ApplicationProvider.getApplicationContext<Application>())
    private val saved: Uri = Uri.parse("content://saved/spread")
    private val config = mockk<PagerConfig>(relaxed = true)

    @Before
    fun setUp() {
        harness.start()
        PagerDecodes.bitmaps.clear()
        mockkObject(ImageUtil)
        every { ImageUtil.findImageType(any<() -> InputStream>()) } returns ImageUtil.ImageType.PNG
        every { ImageUtil.mergeBitmaps(any(), any(), any(), any(), any(), any()) } returns Buffer()
        every { harness.imageSaver.save(any()) } returns saved
    }

    @After
    fun tearDown() = harness.stop()

    private fun ready(index: Int): ReaderPage = loadedPages(readerChapter(), 2)[index].also {
        it.stream = { ByteArrayInputStream(byteArrayOf(1)) }
        it.status = Page.State.Ready
    }

    private fun pager(rightToLeft: Boolean): PagerViewer {
        val viewer: PagerViewer = if (rightToLeft) mockk<R2LPagerViewer>() else mockk<L2RPagerViewer>()
        every { viewer.config } returns config
        return viewer
    }

    private fun model(first: ReaderPage = ready(0), second: ReaderPage? = ready(1), rightToLeft: Boolean = false) =
        harness.loadedViewModel().also { vm ->
            vm.updateState { it.copy(dialog = Dialog.PageActions(first, second), viewer = pager(rightToLeft)) }
            PagerDecodes.bitmaps += listOf(PagerDecodes.portrait(), PagerDecodes.portrait())
        }

    private fun ReaderViewModel.nextEvent(): Event = runBlocking { withTimeout(5_000) { eventFlow.first() } }

    @Test
    fun savedSpreadIsReported() {
        val image = slot<Image>()
        every { harness.imageSaver.save(capture(image)) } returns saved
        val vm = model()
        vm.images.saveImages()
        val result = vm.nextEvent().shouldBeInstanceOf<Event.SavedImage>().result
        result.shouldBeInstanceOf<SaveImageResult.Success>().uri shouldBe saved
        // The saver opens the joined image lazily.
        image.captured.shouldBeInstanceOf<Image.Page>().inputStream().close()
        verify { ImageUtil.mergeBitmaps(any(), any(), true, 0, any(), any()) }
    }

    @Test
    fun rightToLeftJoinsTheOtherWay() {
        every { config.invertDoublePages } returns true
        val vm = model(rightToLeft = true)
        vm.images.saveImages()
        vm.nextEvent().shouldBeInstanceOf<Event.SavedImage>()
    }

    @Test
    fun nonImagesFailTheSave() {
        every { ImageUtil.findImageType(any<() -> InputStream>()) } returns null
        val vm = model()
        vm.images.saveImages()
        val result = vm.nextEvent().shouldBeInstanceOf<Event.SavedImage>().result
        result.shouldBeInstanceOf<SaveImageResult.Error>().error.message shouldBe "Not an image"
    }

    @Test
    fun secondNonImageFailsToo() {
        every { ImageUtil.findImageType(any<() -> InputStream>()) } returnsMany
            listOf(ImageUtil.ImageType.PNG, null)
        val vm = model()
        vm.images.saveImages()
        vm.nextEvent().shouldBeInstanceOf<Event.SavedImage>().result.shouldBeInstanceOf<SaveImageResult.Error>()
    }

    @Test
    fun spreadIsSharedOrCopied() {
        val vm = model()
        vm.images.shareImages(copyToClipboard = false)
        vm.nextEvent().shouldBeInstanceOf<Event.ShareImage>().uri shouldBe saved
        PagerDecodes.bitmaps += listOf(PagerDecodes.portrait(), PagerDecodes.portrait())
        vm.images.shareImages(copyToClipboard = true)
        vm.nextEvent() shouldBe Event.CopyImage(saved)
    }

    @Test
    fun failedSpreadShareIsLogged() {
        val logged = captureLogcat()
        try {
            every { ImageUtil.findImageType(any<() -> InputStream>()) } returns null
            val vm = model()
            vm.images.shareImages(copyToClipboard = false)
            // Logged inside the coroutine with no event sent; the next share still reaches the UI.
            awaitUntil { logged.any { "Not an image" in it } }
            every { ImageUtil.findImageType(any<() -> InputStream>()) } returns ImageUtil.ImageType.PNG
            vm.images.shareImages(copyToClipboard = true)
            vm.nextEvent() shouldBe Event.CopyImage(saved)
        } finally {
            releaseLogcat()
        }
    }

    @Test
    fun rightToLeftShareJoins() {
        every { config.invertDoublePages } returns true
        val vm = model(rightToLeft = true)
        vm.images.shareImages(copyToClipboard = true)
        vm.nextEvent() shouldBe Event.CopyImage(saved)
    }

    @Test
    fun incompleteSpreadsAreIgnored() {
        val vm = harness.loadedViewModel()
        fun tryBoth() {
            vm.images.saveImages()
            vm.images.shareImages(copyToClipboard = false)
        }
        tryBoth()
        vm.updateState { it.copy(dialog = Dialog.PageActions(ready(0), ready(1)), viewer = mockk<WebtoonViewer>()) }
        tryBoth()
        vm.updateState { it.copy(viewer = pager(rightToLeft = false)) }
        vm.updateState { it.copy(dialog = Dialog.PageActions(loadedPages(readerChapter(), 1)[0], ready(1))) }
        tryBoth()
        vm.updateState { it.copy(dialog = Dialog.PageActions(ready(0), null)) }
        tryBoth()
        vm.updateState { it.copy(dialog = Dialog.PageActions(ready(0), loadedPages(readerChapter(), 1)[0])) }
        tryBoth()
        vm.updateState { it.copy(dialog = Dialog.PageActions(ready(0), ready(1)), manga = null) }
        tryBoth()
        verify(exactly = 0) { harness.imageSaver.save(any()) }
    }
}
