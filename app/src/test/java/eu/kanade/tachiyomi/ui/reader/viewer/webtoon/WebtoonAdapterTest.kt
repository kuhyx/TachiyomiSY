package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.loadedPages
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Which transitions the webtoon adapter lays out, and how it treats holders it did not create. */
@RunWith(RobolectricTestRunner::class)
internal class WebtoonAdapterTest {
    private val harness = ReaderActivityHarness(pageCount = 3, viewerFlags = ReadingMode.WEBTOON.flagValue.toLong())
    private lateinit var adapter: WebtoonAdapter

    // A holder of some other adapter.
    private class Foreign(view: View) : RecyclerView.ViewHolder(view)

    @Before
    fun setUp() {
        harness.start()
        val activity: ReaderActivity = harness.launch().get()
        adapter = (activity.viewModel.state.value.viewer as WebtoonViewer).adapter
    }

    @After
    fun tearDown() = harness.stop()

    private fun numbered(id: Long, pages: Int = 2): ReaderChapter = readerChapter(id = 100L + id).also {
        it.chapter.chapter_number = id.toFloat()
        if (pages > 0) loadedPages(it, pages)
    }

    private fun transitions() = adapter.items.count { it is ChapterTransition }

    @Test
    fun gapsForceTransitions() {
        adapter.setChapters(ViewerChapters(numbered(5), numbered(2), numbered(9)), forceTransition = false)
        transitions() shouldBe 2
    }

    @Test
    fun loadedNeighboursJoinUp() {
        adapter.setChapters(ViewerChapters(numbered(5), numbered(4), numbered(6)), forceTransition = false)
        transitions() shouldBe 0
        adapter.setChapters(ViewerChapters(numbered(5), numbered(4), numbered(6)), forceTransition = true)
        transitions() shouldBe 2
    }

    @Test
    fun unloadedKeepTransitions() {
        adapter.setChapters(ViewerChapters(numbered(5, 0), numbered(4, 0), numbered(6, 0)), forceTransition = false)
        adapter.items.size shouldBe 2
    }

    @Test
    fun foreignHoldersAreLeftAlone() {
        val parent = FrameLayout(harness.app)
        val holder = Foreign(View(harness.app))
        adapter.onBindViewHolder(holder, 0)
        adapter.onViewRecycled(holder)
        shouldThrow<IllegalStateException> { adapter.onCreateViewHolder(parent, 99) }
    }
}
