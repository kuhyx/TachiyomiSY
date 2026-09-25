package eu.kanade.tachiyomi.ui.download

import android.app.Application
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import eu.kanade.tachiyomi.source.model.Page
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class DownloadHolderTest {
    private val harness = DownloadHarness()
    private val listener = mockk<DownloadAdapter.DownloadItemListener>(relaxed = true)
    private val first = download(1)
    private val second = download(2)

    private fun list(): DownloadListBinding {
        val model = harness.model()
        val binding = harness.attach(model, headers(first, second))
        model.adapter = DownloadAdapter(listener)
        binding.root.adapter = model.adapter
        model.adapter!!.updateDataSet(headers(first, second))
        harness.layout(binding.root)
        return binding
    }

    private fun DownloadListBinding.holder(position: Int) =
        root.findViewHolderForAdapterPosition(position) as DownloadHolder

    private fun DownloadHolder.progress() = itemView.findViewById<LinearProgressIndicator>(R.id.download_progress)

    private fun DownloadHolder.pagesText() =
        itemView.findViewById<TextView>(R.id.download_progress_text).text.toString()

    @Test
    fun unstartedDownloadHasNoProgress() {
        val holder = list().holder(1)
        holder.progress().max shouldBe 1
        holder.pagesText() shouldBe ""
        holder.notifyProgress()
        holder.notifyDownloadedPages()
        holder.progress().max shouldBe 1
    }

    @Test
    fun pagesDriveTheProgress() {
        val holder = list().holder(1)
        first.pages = listOf(Page(0), Page(1))
        holder.notifyProgress()
        holder.progress().max shouldBe 200
        holder.notifyDownloadedPages()
        holder.pagesText() shouldBe "0/2"
        holder.bind(first)
        holder.progress().max shouldBe 200
        holder.notifyProgress()
    }

    @Test
    fun unboundHolderIgnoresUpdates() {
        val binding = list()
        val holder = DownloadHolder(binding.holder(1).itemView, binding.holder(1).adapter)
        holder.notifyProgress()
        holder.notifyDownloadedPages()
        holder.progress().max shouldBe 1
    }

    @Test
    fun draggingMarksTheCard() {
        val holder = list().holder(1)
        val card = holder.itemView.findViewById<MaterialCardView>(R.id.container)
        holder.onActionStateChanged(1, ItemTouchHelper.ACTION_STATE_IDLE)
        card.isDragged.shouldBeFalse()
        holder.onActionStateChanged(1, ItemTouchHelper.ACTION_STATE_DRAG)
        card.isDragged.shouldBeTrue()
        holder.onItemReleased(1)
        card.isDragged.shouldBeFalse()
        verify { listener.onItemReleased(1) }
    }

    // AppCompat's PopupMenu shows a list in a PopupWindow; its adapter holds the visible items only.
    private fun openMenu(holder: DownloadHolder): ListView {
        holder.itemView.findViewById<View>(R.id.menu).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        val app = ApplicationProvider.getApplicationContext<Application>()
        return shadowOf(app).latestPopupWindow.contentView.findList()!!
    }

    private fun View.findList(): ListView? = when (this) {
        is ListView -> this
        is ViewGroup -> (0..<childCount).firstNotNullOfOrNull { getChildAt(it).findList() }
        else -> null
    }

    private fun ListView.itemIds(): List<Int> = (0..<adapter.count).map { (adapter.getItem(it) as MenuItem).itemId }

    @Test
    fun menuOffersTheMoves() {
        val holder = list().holder(1)
        val menu = openMenu(holder)
        menu.itemIds().contains(R.id.move_to_top).shouldBeFalse()
        menu.itemIds().contains(R.id.move_to_bottom).shouldBeTrue()
        val cancel = menu.itemIds().indexOf(R.id.cancel_download)
        menu.performItemClick(null, cancel, 0)
        verify { listener.onMenuItemClick(1, match { it.itemId == R.id.cancel_download }) }
    }

    @Test
    fun lastRowCannotMoveDown() {
        val menu = openMenu(list().holder(2))
        menu.itemIds().contains(R.id.move_to_top).shouldBeTrue()
        menu.itemIds().contains(R.id.move_to_bottom).shouldBeFalse()
    }

    @Test
    fun headerDragCollapsesTheList() {
        val binding = list()
        val header = binding.root.findViewHolderForAdapterPosition(0) as DownloadHeaderHolder
        val card = header.itemView.findViewById<MaterialCardView>(R.id.container)
        header.onActionStateChanged(0, ItemTouchHelper.ACTION_STATE_IDLE)
        card.isDragged.shouldBeFalse()
        header.onActionStateChanged(0, ItemTouchHelper.ACTION_STATE_DRAG)
        card.isDragged.shouldBeTrue()
        header.onItemReleased(0)
        card.isDragged.shouldBeFalse()
        verify { listener.onItemReleased(0) }
    }
}
