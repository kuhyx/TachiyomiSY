package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowLooper

/**
 * Launches a real [ReaderActivity] in the Robolectric sandbox over [ReaderVmHarness]'s graph:
 * manga 10 with chapters 1..3 from an HTTP source, chapter loading replaced by [pageCount]
 * queued pages per chapter so no image is ever decoded.
 */
internal class ReaderActivityHarness(private val pageCount: Int = 4) {
    val app: Application = ApplicationProvider.getApplicationContext()
    val vm: ReaderVmHarness = ReaderVmHarness(app)
    val source: HttpSource = mockk(relaxed = true)

    fun start() {
        vm.start(module { single { SecurityPreferences(vm.store) } }, testMain = false)
        every { vm.sourceManager.getOrStub(1L) } returns source
        every { vm.sourceManager.get(1L) } returns source
        coEvery { vm.getManga.await(10L) } returns vm.manga
        vm.chapters(domainChapter(1L), domainChapter(2L), domainChapter(3L))
        mockkConstructor(ChapterLoader::class)
        coEvery { anyConstructed<ChapterLoader>().loadChapter(any(), any()) } answers {
            val chapter = firstArg<ReaderChapter>()
            if (chapter.state !is ReaderChapter.State.Loaded) {
                val pages = List(pageCount) { ReaderPage(it, url = "/p/$it") }
                pages.forEach { it.chapter = chapter }
                chapter.state = ReaderChapter.State.Loaded(pages)
            }
        }
    }

    fun stop() {
        vm.stop()
    }

    /** Builds and resumes the activity for chapter [chapterId], then lets the main looper settle. */
    fun launch(chapterId: Long = 2L, page: Int? = null): ActivityController<ReaderActivity> {
        val intent = ReaderActivity.newIntent(app, mangaId = 10L, chapterId = chapterId, page = page)
        val controller = Robolectric.buildActivity(ReaderActivity::class.java, intent).setup()
        settle()
        return controller
    }

    fun settle() {
        repeat(3) {
            Thread.sleep(50)
            ShadowLooper.idleMainLooper()
        }
    }

    /** Idles the main looper (letting IO work land) until [condition] holds, for at most two seconds. */
    fun settleUntil(condition: () -> Boolean) {
        repeat(100) {
            ShadowLooper.idleMainLooper()
            if (condition()) return
            Thread.sleep(20)
        }
        error("condition not reached")
    }

    fun pagesReady(chapter: ReaderChapter) {
        chapter.pages?.forEach { it.status = Page.State.Ready }
    }
}
