package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult
import eu.kanade.tachiyomi.ui.reader.viewer.pager.L2RPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tachiyomi.source.local.LocalSource
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ReaderShadowDecoder::class, ReaderShadowDecoderCompanion::class],
)
internal class ReaderImageSharingTest {

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

    @Test
    fun shareNeedsReadyStreamedPage() {
        actions.shareImage(copyToClipboard = false, useExtraPage = true)
        fixture.select(ready = false)
        actions.shareImage(copyToClipboard = false, useExtraPage = false)
        fixture.select()
        fixture.pages[0].stream = null
        actions.shareImage(copyToClipboard = false, useExtraPage = false)
        fixture.vm.updateState { it.copy(manga = null) }
        actions.shareImage(copyToClipboard = false, useExtraPage = true)
        verify(exactly = 0) { fixture.harness.imageSaver.save(any()) }
    }

    @Test
    fun shareOrCopyThePage() {
        fixture.select()
        val (first, second) = fixture.pages
        fixture.event { actions.shareImage(copyToClipboard = false, useExtraPage = true) } shouldBe
            Event.ShareImage(fixture.savedUri, second)
        fixture.event { actions.shareImage(copyToClipboard = true, useExtraPage = false) } shouldBe
            Event.CopyImage(fixture.savedUri)
        first.chapter shouldBe second.chapter
    }

    @Test
    fun spreadSharingNeedsBothPages() {
        actions.shareImages(copyToClipboard = false)
        fixture.select()
        actions.shareImages(copyToClipboard = false)
        fixture.viewer(mockk<L2RPagerViewer>(relaxed = true))
        fixture.pages[0].status = Page.State.Queue
        actions.shareImages(copyToClipboard = false)
        fixture.select(extraPage = false)
        actions.shareImages(copyToClipboard = false)
        fixture.select()
        fixture.pages[1].status = Page.State.Queue
        actions.shareImages(copyToClipboard = false)
        fixture.select()
        fixture.vm.updateState { it.copy(manga = null) }
        actions.shareImages(copyToClipboard = false)
        verify(exactly = 0) { fixture.harness.imageSaver.save(any()) }
    }

    @Test
    fun shareOrCopyTheSpread() {
        fixture.select()
        fixture.viewer(mockk<L2RPagerViewer>(relaxed = true))
        val (first, second) = fixture.pages
        fixture.event { actions.shareImages(copyToClipboard = false) } shouldBe
            Event.ShareImage(fixture.savedUri, first, second)
        fixture.viewer(mockk<R2LPagerViewer>(relaxed = true))
        fixture.event { actions.shareImages(copyToClipboard = true) } shouldBe Event.CopyImage(fixture.savedUri)
    }

    @Test
    fun coverNeedsReadyStreamedPage() {
        actions.setAsCover(useExtraPage = false)
        fixture.select(ready = false)
        actions.setAsCover(useExtraPage = false)
        fixture.select()
        fixture.pages[1].stream = null
        actions.setAsCover(useExtraPage = true)
        fixture.vm.updateState { it.copy(manga = null) }
        actions.setAsCover(useExtraPage = false)
    }

    @Test
    fun coverResultFollowsTheManga() {
        fixture.select()
        fun cover() = (fixture.event { actions.setAsCover(useExtraPage = false) } as Event.SetCoverResult).result
        cover() shouldBe SetAsCoverResult.AddToLibraryFirst
        fixture.vm.updateState { it.copy(manga = fixture.harness.manga.copy(favorite = true)) }
        cover() shouldBe SetAsCoverResult.Success
        fixture.vm.updateState { it.copy(manga = fixture.harness.manga.copy(source = LocalSource.ID)) }
        cover() shouldBe SetAsCoverResult.Success
        coEvery { fixture.updateManga.awaitUpdateCoverLastModified(any()) } throws IOException("disk")
        cover() shouldBe SetAsCoverResult.Error
    }
}
