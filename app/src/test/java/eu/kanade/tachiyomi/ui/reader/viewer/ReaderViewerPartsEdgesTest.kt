package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.util.view.isVisibleOnScreen
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.LocalSource
import java.util.concurrent.TimeUnit

private const val VIEW_EXTENSIONS = "eu.kanade.tachiyomi.util.view.ViewExtensionsKt"

/** Transition labels for every source kind, and the page image view's rarer inputs. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderViewerPartsEdgesTest {
    private val rig = PageImageRig()
    private val downloads = mockk<DownloadManager>(relaxed = true)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() {
        unmockkAll()
        rig.stop()
    }

    @Test
    fun transitionLabelsEveryKind() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = ReaderTransitionView(host)
        host.setContentView(view)
        val from = readerChapter()
        view.bind(ChapterTransition.Next(from, null), downloads, manga = null)
        ShadowLooper.idleMainLooper()
        val local = Manga.create().copy(source = LocalSource.ID)
        view.bind(ChapterTransition.Next(from, readerChapter(id = 2L)), downloads, local)
        from.pageLoader = mockk<PageLoader> { every { isLocal } returns true }
        view.bind(ChapterTransition.Prev(from, null), downloads, Manga.create())
        from.pageLoader = mockk<PageLoader> { every { isLocal } returns false }
        view.bind(ChapterTransition.Prev(from, readerChapter(id = 3L)), downloads, Manga.create())
        verify(exactly = 1) {
            downloads.isChapterDownloaded(
                chapterName = any(),
                chapterScanlator = any(),
                chapterUrl = any(),
                mangaTitle = any(),
                sourceId = any(),
                skipCache = true,
            )
        }
        // The next chapter already downloaded.
        every {
            downloads.isChapterDownloaded(
                chapterName = any(),
                chapterScanlator = any(),
                chapterUrl = any(),
                mangaTitle = any(),
                sourceId = any(),
                skipCache = true,
            )
        } returns true
        view.bind(ChapterTransition.Next(from, readerChapter(id = 4L)), downloads, Manga.create())
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun verticalMoveCancelsLongTap() {
        val taps = mutableListOf<MotionEvent>()
        val detector = GestureDetectorWithLongTap(
            ApplicationProvider.getApplicationContext(),
            object : GestureDetectorWithLongTap.Listener() {
                override fun onLongTapConfirmed(ev: MotionEvent) {
                    taps += ev
                }
            },
        )
        detector.onTouchEvent(MotionEvent.obtain(10_000L, 10_000L, MotionEvent.ACTION_DOWN, 1f, 1f, 0))
        detector.onTouchEvent(MotionEvent.obtain(10_000L, 10_010L, MotionEvent.ACTION_MOVE, 1f, 500f, 0))
        ShadowLooper.idleMainLooper(2, TimeUnit.SECONDS)
        taps.size shouldBe 0
    }

    @Test
    fun otherInnerViewIsHidden() {
        val view = rig.view()
        view.pageView = View(rig.context)
        view.recycle()
        view.pageView!!.visibility shouldBe View.GONE
    }

    @Test
    fun detachedViewDoesNotZoom() {
        val view = rig.view()
        view.config = ReaderPageImageView.Config(zoomDuration = 1, landscapeZoom = true)
        val inner = mockk<SubsamplingScaleImageView>(relaxed = true) {
            every { sWidth } returns 20
            every { sHeight } returns 10
            every { scale } returns 1f
            every { minScale } returns 1f
            every { handler } returns null
        }
        with(view) { inner.landscapeZoom(true) }
        verify(exactly = 0) { inner.animateScaleAndCenter(any(), any()) }
    }

    @Test
    fun visibleImageZoomsWhenReady() {
        mockkStatic(VIEW_EXTENSIONS)
        every { any<View>().isVisibleOnScreen() } returns true
        val view = rig.view()
        view.prepareNonAnimatedImageView()
        val bitmap = Bitmap.createBitmap(4, 2, Bitmap.Config.ARGB_8888)
        val config = ReaderPageImageView.Config(zoomDuration = 1, landscapeZoom = true)
        val inner = view.setNonAnimatedImage(BitmapDrawable(rig.context.resources, bitmap), config)!!
        inner.imageEvents().onReady()
        rig.events shouldBe listOf("loaded")
    }
}
