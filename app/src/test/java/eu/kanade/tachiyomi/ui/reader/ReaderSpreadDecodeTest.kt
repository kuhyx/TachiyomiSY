package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.data.saver.Location
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerDecodes
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerShadowDecoder
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerShadowDecoderCompanion
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.core.common.util.system.ImageUtil
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** A spread whose either page the decoder cannot open is not saved. */
@RunWith(RobolectricTestRunner::class)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [PagerShadowDecoder::class, PagerShadowDecoderCompanion::class],
)
internal class ReaderSpreadDecodeTest {
    private val harness = ReaderVmHarness(ApplicationProvider.getApplicationContext<Application>())
    private val png: ByteArray = ByteArrayOutputStream().also {
        Bitmap.createBitmap(2, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
    }.toByteArray()

    @Before
    fun setUp() {
        harness.start()
        mockkObject(ImageUtil)
        every { ImageUtil.findImageType(any<() -> InputStream>()) } returns ImageUtil.ImageType.PNG
    }

    @After
    fun tearDown() {
        PagerDecodes.refuseAfter = null
        PagerDecodes.bitmaps.clear()
        harness.stop()
    }

    private fun page(index: Int, bytes: ByteArray): ReaderPage = loadedPages(readerChapter(), 2)[index].also {
        it.stream = { ByteArrayInputStream(bytes) }
        it.status = Page.State.Ready
    }

    private fun save(first: ReaderPage, second: ReaderPage) = harness.loadedViewModel().images.saveImages(
        page1 = first,
        page2 = second,
        isLTR = true,
        bg = 0,
        location = mockk<Location>(relaxed = true),
        manga = harness.manga,
    )

    @Test
    fun undecodableFirstPage() {
        PagerDecodes.refuseAfter = 0
        shouldThrow<NullPointerException> { save(page(0, byteArrayOf(1, 2)), page(1, png)) }
        verify(exactly = 0) { harness.imageSaver.save(any()) }
    }

    @Test
    fun undecodableSecondPage() {
        PagerDecodes.refuseAfter = 1
        PagerDecodes.bitmaps += PagerDecodes.portrait()
        shouldThrow<NullPointerException> { save(page(0, png), page(1, byteArrayOf(1, 2))) }
        verify(exactly = 0) { harness.imageSaver.save(any()) }
    }
}
