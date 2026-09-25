package eu.kanade.tachiyomi.ui.manga.merged

import android.view.LayoutInflater
import android.view.View
import android.widget.Spinner
import com.google.android.material.materialswitch.MaterialSwitch
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.EditMergedSettingsDialogBinding
import eu.kanade.tachiyomi.ui.manga.manga
import eu.kanade.tachiyomi.ui.manga.track.TrackHomeHarness
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import tachiyomi.domain.manga.model.MergedMangaReference

@RunWith(RobolectricTestRunner::class)
internal class EditMergedHeaderTest {
    private val koin = TrackHomeHarness()
    private val context = themedActivity()
    private val state = EditMergedSettingsState(context, {}, {}, {})
    private val binding = EditMergedSettingsDialogBinding.inflate(LayoutInflater.from(context))

    @Before
    fun setUp() {
        koin.start()
        every { koin.sourceManager.getOrStub(any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() = koin.stop()

    private fun open(self: MergedMangaReference?, vararg members: MergedMangaReference) {
        state.onViewCreated(context, binding, listOf(manga().copy(id = 1L)), listOfNotNull(self) + members)
        layOut(binding)
        ShadowLooper.idleMainLooper()
    }

    private fun <T : View> find(id: Int): T = binding.root.findViewById(id)

    @Test
    fun dedupeModeFollowsTheSpinner() {
        open(selfReference(MergedMangaReference.CHAPTER_SORT_NO_DEDUPE), reference(1L), reference(2L))
        val spinner = find<Spinner>(R.id.dedupe_mode_spinner)
        spinner.setSelection(1)
        ShadowLooper.idleMainLooper()
        state.mergeReference?.chapterSortMode shouldBe MergedMangaReference.CHAPTER_SORT_PRIORITY
        state.mergedMangaAdapter?.isPriorityOrder shouldBe true
        spinner.onItemSelectedListener?.onItemSelected(spinner, null, 9, 0L)
        state.mergeReference?.chapterSortMode shouldBe MergedMangaReference.CHAPTER_SORT_NO_DEDUPE
        spinner.onItemSelectedListener?.onNothingSelected(spinner)
        state.mergedMangaAdapter?.isPriorityOrder shouldBe false
    }

    @Test
    fun switchTurnsDedupeOff() {
        open(selfReference(MergedMangaReference.CHAPTER_SORT_MOST_CHAPTERS), reference(1L), reference(2L))
        val switch = find<MaterialSwitch>(R.id.dedupe_switch)
        switch.isChecked shouldBe true
        switch.isChecked = false
        state.mergeReference?.chapterSortMode shouldBe MergedMangaReference.CHAPTER_SORT_NONE
        find<Spinner>(R.id.dedupe_mode_spinner).isEnabled shouldBe false
        switch.isChecked = true
        state.mergeReference?.chapterSortMode shouldBe MergedMangaReference.CHAPTER_SORT_NO_DEDUPE
    }

    @Test
    fun missingSelfReferenceDisablesDedupe() {
        open(null, reference(1L), reference(2L))
        find<MaterialSwitch>(R.id.dedupe_switch).isChecked shouldBe false
        state.mergedMangaHeaderAdapter?.canMove() shouldBe false
        val spinner = find<Spinner>(R.id.dedupe_mode_spinner)
        spinner.onItemSelectedListener?.onNothingSelected(spinner)
        state.mergeReference shouldBe null
    }

    @Test
    fun infoMangaFollowsTheSpinner() {
        open(selfReference(), reference(1L), reference(2L, isInfoManga = true))
        val spinner = find<Spinner>(R.id.manga_info_spinner)
        spinner.selectedItemPosition shouldBe 1
        spinner.onItemSelectedListener?.onItemSelected(spinner, null, 0, 0L)
        state.mergedMangas.map { it.second.isInfoManga } shouldBe listOf(true, false)
        spinner.onItemSelectedListener?.onNothingSelected(spinner)
        state.mergedMangas.map { it.second.isInfoManga } shouldBe listOf(false, true)
    }

    @Test
    fun noInfoMangaSelectsTheFirst() {
        open(selfReference(), reference(1L), reference(2L))
        val spinner = find<Spinner>(R.id.manga_info_spinner)
        spinner.selectedItemPosition shouldBe 0
        spinner.onItemSelectedListener?.onNothingSelected(spinner)
        state.mergedMangas.none { it.second.isInfoManga } shouldBe true
    }

    @Test
    fun holdersBindAndClick() {
        open(selfReference(), reference(1L), reference(2L))
        val holder = state.mergedMangaAdapter!!.allBoundViewHolders.filterIsInstance<EditMergedMangaHolder>().first()
        holder.reference?.id shouldBe 1L
        holder.binding.remove.performClick()
        holder.binding.getChapterUpdates.performClick()
        holder.binding.download.performClick()
        holder.onItemReleased(0)
        holder.setHandelAlpha(false)
        holder.binding.reorder.alpha shouldBe 0.5F
        state.mergedMangaAdapter!!.onSetPrioritySort(false)
        state.mergedMangaAdapter!!.isHandleDragEnabled shouldBe false
    }

    @Test
    fun itemsCompareByReference() {
        val item = EditMergedMangaItem(null, reference(1L))
        (item == EditMergedMangaItem(manga(), reference(1L))) shouldBe true
        (item == item) shouldBe true
        item.equals("x") shouldBe false
        item.hashCode() shouldBe 1L.hashCode()
        item.isDraggable shouldBe true
        item.layoutRes shouldBe R.layout.edit_merged_settings_item
    }
}
