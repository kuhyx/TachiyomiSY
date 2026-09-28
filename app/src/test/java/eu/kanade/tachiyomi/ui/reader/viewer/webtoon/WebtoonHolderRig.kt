package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import androidx.fragment.app.FragmentActivity
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.base.forgetRecordedCalls
import eu.kanade.tachiyomi.ui.base.release
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.hideMenu
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.pager.MENU_KT
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import okio.BufferedSource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import tachiyomi.core.common.util.system.ImageUtil

/**
 * Webtoon page holders built for real in a themed activity, around a mocked viewer whose [config] is a relaxed
 * mock; [ImageUtil] is mocked, tests stub what they need.
 */
internal class WebtoonHolderRig {
    val config: WebtoonConfig = mockk(relaxed = true)
    val activity: ReaderActivity = mockk(relaxed = true)
    val loader: PageLoader = mockk(relaxed = true)
    private val holders = mutableListOf<WebtoonPageHolder>()
    private lateinit var host: ActivityController<FragmentActivity>
    val context: FragmentActivity get() = host.get()

    fun start() {
        host = Robolectric.buildActivity(FragmentActivity::class.java)
        host.get().setTheme(R.style.Theme_Tachiyomi)
        host.setup()
        stopKoin()
        startKoin { modules(module { single { BasePreferences(context.application, MapPreferenceStore()) } }) }
        mockkObject(ImageUtil)
        every { ImageUtil.canUseHardwareBitmap(any<BufferedSource>()) } returns false
        mockkStatic(MENU_KT)
        every { any<ReaderActivity>().hideMenu() } just runs
    }

    fun stop() {
        try {
            holders.forEach { it.recycle() }
            host.release()
        } finally {
            forgetRecordedCalls()
            unmockkAll()
            stopKoin()
        }
    }

    fun viewer(continuous: Boolean = true): WebtoonViewer {
        val viewer = mockk<WebtoonViewer>(relaxed = true)
        every { viewer.config } returns config
        every { viewer.activity } returns activity
        every { viewer.isContinuous } returns continuous
        return viewer
    }

    fun page(imageUrl: String? = null, withLoader: Boolean = false): ReaderPage {
        val chapter = readerChapter()
        if (withLoader) chapter.pageLoader = loader
        return ReaderPage(0, url = "/p/0", imageUrl = imageUrl).also { it.chapter = chapter }
    }

    fun holder(viewer: WebtoonViewer = viewer()): WebtoonPageHolder =
        WebtoonPageHolder(ReaderPageImageView(context, isWebtoon = true), viewer).also { holders += it }
}
