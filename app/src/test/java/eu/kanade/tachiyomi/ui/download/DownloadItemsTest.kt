package eu.kanade.tachiyomi.ui.download

import android.widget.TextView
import eu.kanade.tachiyomi.R
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DownloadItemsTest {
    private val harness = DownloadHarness()
    private val first = download(1)
    private val header = headers(first, download(2)).single()

    @Test
    fun headerEqualityChecksEveryField() {
        (header == header).shouldBeTrue()
        header.equals("other").shouldBeFalse()
        val nothing: Any? = null
        header.equals(nothing).shouldBeFalse()
        (header == header.copy(id = 9)).shouldBeFalse()
        (header == header.copy(name = "x")).shouldBeFalse()
        (header == header.copy(size = 9)).shouldBeFalse()
        (header == header.copy()).shouldBeFalse()
        val sameItems = header.copy().also { it.subItems = header.subItems }
        (header == sameItems).shouldBeTrue()
        sameItems.hashCode() shouldBe header.hashCode()
        val copiedItems = header.copy().also { it.subItems = header.subItems.toMutableList() }
        (header == copiedItems).shouldBeFalse()
    }

    @Test
    fun headerIsAValueHolder() {
        val (id, name, size) = header
        Triple(id, name, size) shouldBe Triple(1L, "S1", 2)
        header.toString() shouldBe "DownloadHeaderItem(id=1, name=S1, size=2)"
        header.layoutRes shouldBe R.layout.download_header
        header.isExpanded.shouldBeTrue()
        header.isSelectable.shouldBeFalse()
    }

    @Test
    fun itemsCompareByChapter() {
        val item = header.subItems.first()
        (item == item).shouldBeTrue()
        (item == DownloadItem(download(1), header)).shouldBeTrue()
        (item == header.subItems.last()).shouldBeFalse()
        item.equals("other").shouldBeFalse()
        item.hashCode() shouldBe 1
        item.isDraggable.shouldBeTrue()
        item.layoutRes shouldBe R.layout.download_item
    }

    @Test
    fun itemsBindTheirHolders() {
        val binding = harness.attach(harness.model(), listOf(header))
        val headerView = binding.root.findViewHolderForAdapterPosition(0)!!.itemView
        headerView.findViewById<TextView>(R.id.title).text.toString() shouldBe "S1 (2)"
        val itemView = binding.root.findViewHolderForAdapterPosition(1)!!.itemView
        itemView.findViewById<TextView>(R.id.chapter_title).text.toString() shouldBe "Chapter 1"
        itemView.findViewById<TextView>(R.id.manga_full_title).text.toString() shouldBe "Manga 1"
    }

    @Test
    fun rowsOnlyMoveWithinTheirSeries() {
        val other = download(3, source = httpSource(2))
        val model = harness.model()
        harness.attach(model, headers(first, download(2), other))
        val adapter = model.adapter!!
        adapter.shouldMove(1, 2).shouldBeTrue()
        adapter.shouldMove(1, 4).shouldBeFalse()
    }
}
