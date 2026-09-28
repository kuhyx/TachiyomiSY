package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.Bitmap
import androidx.fragment.app.FragmentActivity
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.base.forgetRecordedCalls
import eu.kanade.tachiyomi.ui.base.release
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.hideMenu
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.readerChapter
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.cancel
import okio.Buffer
import okio.BufferedSource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import tachiyomi.core.common.util.system.ImageUtil
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor

/** The file of the viewer's double-page extensions a page holder calls. */
internal const val DOUBLE_PAGES_KT: String = "eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewerDoublePagesKt"

/** The file of the reader activity's menu extensions a page holder calls. */
internal const val MENU_KT: String = "eu.kanade.tachiyomi.ui.reader.ReaderActivityMenuKt"

/**
 * Page holders built for real in a themed activity, around a mocked viewer whose [config] is a relaxed mock;
 * [ImageUtil] is mocked (tests stub what they need) and the viewer's page-split extensions do nothing.
 */
internal class PagerHolderRig {
    val config: PagerConfig = mockk(relaxed = true)
    val activity: ReaderActivity = mockk(relaxed = true)
    val loader: PageLoader = mockk(relaxed = true)
    private val holders = mutableListOf<PagerPageHolder>()
    private lateinit var host: ActivityController<FragmentActivity>
    val context: FragmentActivity get() = host.get()

    fun start() {
        host = Robolectric.buildActivity(FragmentActivity::class.java)
        host.get().setTheme(R.style.Theme_Tachiyomi)
        host.setup()
        stopKoin()
        startKoin { modules(module { single { BasePreferences(context.application, MapPreferenceStore()) } }) }
        PagerDecodes.bitmaps.clear()
        PagerDecodes.throwNext = false
        PagerDecodes.refuseAfter = null
        // The still image view decodes on AsyncTask threads that report back whenever they finish, possibly after
        // the assertion or into the next test (and with a profile the decoder shadow refuses): never start them.
        overrideAsyncExecutor(Executor { _ -> })
        mockkObject(ImageUtil)
        every { ImageUtil.canUseHardwareBitmap(any<BufferedSource>()) } returns false
        every { config.imageScaleType } returns SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
        mockkStatic(DOUBLE_PAGES_KT)
        every { any<PagerViewer>().splitDoublePages(any()) } just runs
        every { any<PagerViewer>().onPageSplit(any(), any()) } just runs
        mockkStatic(MENU_KT)
        every { any<ReaderActivity>().hideMenu() } just runs
    }

    fun stop() {
        try {
            holders.forEach { it.scope.cancel() }
            host.release()
        } finally {
            overrideAsyncExecutor(null)
            forgetRecordedCalls()
            unmockkAll()
            stopKoin()
        }
    }

    fun viewer(rightToLeft: Boolean = false): PagerViewer {
        val viewer: PagerViewer = if (rightToLeft) {
            mockk<R2LPagerViewer>(relaxed = true)
        } else {
            mockk<L2RPagerViewer>(relaxed = true)
        }
        every { viewer.config } returns config
        every { viewer.activity } returns activity
        return viewer
    }

    fun page(index: Int = 0, imageUrl: String? = null, withLoader: Boolean = false): ReaderPage {
        val chapter = readerChapter()
        if (withLoader) chapter.pageLoader = loader
        return ReaderPage(index, url = "/p/$index", imageUrl = imageUrl).also { it.chapter = chapter }
    }

    fun insert(): InsertPage = InsertPage(page())

    fun holder(page: ReaderPage = page(), extra: ReaderPage? = null, viewer: PagerViewer = viewer()): PagerPageHolder =
        PagerPageHolder(context, viewer, page, extra).also { holders += it }

    fun png(width: Int, height: Int): Buffer {
        val out = ByteArrayOutputStream()
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, out)
        return Buffer().write(out.toByteArray())
    }
}

// Robolectric's paused-looper AsyncTask shadow is deprecated as a type, so its static hook is reached reflectively;
// a null executor restores the task's own.
private fun overrideAsyncExecutor(executor: Executor?) {
    Class.forName("org.robolectric.shadows.ShadowPausedAsyncTask")
        .getMethod("overrideExecutor", Executor::class.java)
        .invoke(null, executor)
}
