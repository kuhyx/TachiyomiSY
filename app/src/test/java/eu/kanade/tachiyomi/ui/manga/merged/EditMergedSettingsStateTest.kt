package eu.kanade.tachiyomi.ui.manga.merged

import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
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
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.manga.model.MergedMangaReference

@RunWith(RobolectricTestRunner::class)
internal class EditMergedSettingsStateTest {
    private val koin = TrackHomeHarness()
    private val context = themedActivity()
    private val deleted = mutableListOf<MergedMangaReference>()
    private val saved = mutableListOf<List<MergedMangaReference>>()
    private var dismissed = 0
    private val state = EditMergedSettingsState(context, { deleted += it }, { dismissed++ }, { saved += it })
    private val binding = EditMergedSettingsDialogBinding.inflate(LayoutInflater.from(context))

    @Before
    fun setUp() {
        koin.start()
        every { koin.sourceManager.getOrStub(any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() = koin.stop()

    private fun open(vararg references: MergedMangaReference) {
        state.onViewCreated(context, binding, listOf(manga().copy(id = 1L)), references.toList())
        layOut(binding)
    }

    private fun confirm() {
        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun tooFewReferencesDismiss() {
        open(reference(1L))
        ShadowToast.shownToastCount() shouldBe 1
        dismissed shouldBe 1
        state.mergeReference shouldBe null
        state.mergedMangaAdapter?.isHandleDragEnabled shouldBe false
    }

    @Test
    fun priorityModeEnablesDragging() {
        open(selfReference(), reference(2L), reference(1L, isInfoManga = true))
        state.mergeReference shouldBe selfReference()
        state.mergedMangaAdapter?.isHandleDragEnabled shouldBe true
        state.mergedMangaAdapter?.currentItems?.map { it.mergedMangaReference.id } shouldBe listOf(1L, 2L)
        state.mergedMangas.first().first?.id shouldBe null
    }

    @Test
    fun releasingRecomputesPriorities() {
        open(selfReference(), reference(1L, priority = 5), reference(2L, priority = 6))
        state.onItemReleased(0)
        state.mergedMangas.map { it.second.chapterPriority } shouldBe listOf(0, 1)
        EditMergedSettingsState(context, {}, {}, {}).onItemReleased(0)
    }

    @Test
    fun deletingAsksFirst() {
        open(selfReference(), reference(1L), reference(2L))
        state.onDeleteClick(5)
        state.onDeleteClick(0)
        confirm()
        deleted.map { it.id } shouldBe listOf(1L)
        dismissed shouldBe 1
        EditMergedSettingsState(context, {}, {}, {}).onDeleteClick(0)
    }

    @Test
    fun chapterUpdatesToggle() {
        open(selfReference(), reference(1L), reference(2L))
        state.onToggleChapterUpdatesClicked(0)
        confirm()
        state.mergedMangas.first { it.second.id == 1L }.second.getChapterUpdates shouldBe false
        state.mergedMangas.first { it.second.id == 2L }.second.getChapterUpdates shouldBe true
        state.onToggleChapterUpdatesClicked(7)
        confirm()
    }

    @Test
    fun downloadsToggle() {
        open(selfReference(), reference(1L), reference(2L))
        state.onToggleDownloadsClicked(1)
        confirm()
        state.mergedMangas.first { it.second.id == 2L }.second.downloadChapters shouldBe false
        state.onToggleDownloadsClicked(7)
        confirm()
    }

    @Test
    fun unboundRowsToast() {
        state.onViewCreated(context, binding, emptyList(), listOf(selfReference(), reference(1L), reference(2L)))
        state.onToggleChapterUpdatesClicked(0)
        confirm()
        state.onToggleDownloadsClicked(0)
        confirm()
        ShadowToast.shownToastCount() shouldBe 2
    }

    @Test
    fun savingReturnsEveryReference() {
        open(selfReference(), reference(1L), reference(2L))
        state.onPositiveButtonClick()
        saved.single().map { it.id } shouldBe listOf(9L, 1L, 2L)
        dismissed shouldBe 1
        val noSelf = EditMergedSettingsState(context, {}, {}, { saved += it })
        noSelf.onPositiveButtonClick()
        saved.last() shouldBe emptyList()
    }
}
