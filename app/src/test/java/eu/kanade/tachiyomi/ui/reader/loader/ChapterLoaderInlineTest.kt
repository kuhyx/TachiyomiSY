package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.readerChapter
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

/** A chapter loaded by a caller already on the IO dispatcher finishes in place, without a thread hop. */
@RunWith(RobolectricTestRunner::class)
internal class ChapterLoaderInlineTest {
    private val harness = ChapterLoaderHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    @Test
    fun loadsInlineOnIo() {
        val chapter = readerChapter()
        runBlocking(Dispatchers.IO) { harness.loader(mockk<HttpSource>()).loadChapter(chapter) }
        chapter.state.shouldBeInstanceOf<ReaderChapter.State.Loaded>()
    }
}
