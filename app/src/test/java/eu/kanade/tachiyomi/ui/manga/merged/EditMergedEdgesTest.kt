package eu.kanade.tachiyomi.ui.manga.merged

import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
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
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import tachiyomi.domain.manga.model.MergedMangaReference

/** Toggling a merged member twice, spinner positions past the list, and members whose entry is missing. */
@RunWith(RobolectricTestRunner::class)
internal class EditMergedEdgesTest {
    private val koin = TrackHomeHarness()
    private val context = themedActivity()
    private val state = EditMergedSettingsState(context, {}, {}, {})
    private val binding = EditMergedSettingsDialogBinding.inflate(LayoutInflater.from(context))

    @Before
    fun setUp() {
        XLog.init(LogConfiguration.Builder().build(), Printer { _, _, _ -> })
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

    private fun confirm() {
        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        ShadowLooper.idleMainLooper()
    }

    private fun member(id: Long) = state.mergedMangas.first { it.second.id == id }.second

    @Test
    fun chapterUpdatesToggleBack() {
        open(selfReference(), reference(1L), reference(2L))
        state.onToggleChapterUpdatesClicked(0)
        confirm()
        state.onToggleChapterUpdatesClicked(0)
        confirm()
        member(1L).getChapterUpdates shouldBe true
    }

    @Test
    fun downloadsToggleBack() {
        open(selfReference(), reference(1L), reference(2L))
        state.onToggleDownloadsClicked(0)
        confirm()
        state.onToggleDownloadsClicked(0)
        confirm()
        member(1L).downloadChapters shouldBe true
    }

    @Test
    fun spinnersPastTheList() {
        open(null, reference(1L), reference(2L))
        val dedupe = binding.root.findViewById<Spinner>(R.id.dedupe_mode_spinner)
        dedupe.onItemSelectedListener?.onItemSelected(dedupe, null, 9, 0L)
        state.mergeReference shouldBe null
        val info = binding.root.findViewById<Spinner>(R.id.manga_info_spinner)
        info.onItemSelectedListener?.onItemSelected(info, null, 9, 0L)
        state.mergedMangas.none { it.second.isInfoManga } shouldBe true
    }

    @Test
    fun switchWithoutSelfReference() {
        open(null, reference(1L), reference(2L))
        val switch = binding.root.findViewById<MaterialSwitch>(R.id.dedupe_switch)
        switch.isChecked = true
        state.mergeReference shouldBe null
    }

    @Test
    fun itemsWithOtherReferencesDiffer() {
        (EditMergedMangaItem(null, reference(1L)) == EditMergedMangaItem(null, reference(2L))) shouldBe false
    }

    @Test
    fun noDedupeLeavesSwitchOff() {
        open(selfReference(MergedMangaReference.CHAPTER_SORT_NONE), reference(1L), reference(2L))
        val switch = binding.root.findViewById<MaterialSwitch>(R.id.dedupe_switch)
        switch.isChecked shouldBe false
    }

    // A member whose entry was deleted keeps its reference with no manga id (ON DELETE SET NULL).
    @Test
    fun memberWithoutMangaId() {
        val orphan = reference(3L).copy(mangaId = null)
        open(selfReference(), reference(1L), orphan)
        state.mergedMangas.first { it.second.id == 3L }.first shouldBe null
    }

    // Like releasing or deleting, toggling before the list is built changes nothing.
    @Test
    fun togglesBeforeTheListIsBuilt() {
        val fresh = EditMergedSettingsState(context, {}, {}, {})
        fresh.onToggleChapterUpdatesClicked(0)
        confirm()
        fresh.onToggleDownloadsClicked(0)
        confirm()
        fresh.mergedMangas shouldBe emptyList()
    }

    // Bound holders sit in a hash set, so toggling both rows makes one search pass the other row's holder first.
    @Test
    fun bothRowsToggle() {
        open(selfReference(), reference(1L), reference(2L))
        for (position in 0..1) {
            state.onToggleChapterUpdatesClicked(position)
            confirm()
            state.onToggleDownloadsClicked(position)
            confirm()
        }
        member(1L).getChapterUpdates shouldBe false
        member(2L).downloadChapters shouldBe false
    }
}
