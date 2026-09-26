package eu.kanade.tachiyomi.ui.manga

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MangaDialogsTest {
    private val harness = MangaHarness()

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L), chapter(2L), chapter(3L))
    }

    @After
    fun tearDown() = harness.stop()

    private fun MangaScreenModel.dialog() = awaitSuccess().dialog

    @Test
    fun simpleDialogsOpenAndClose() {
        val model = harness.loaded()
        model.showDeleteChapterDialog(listOf(chapter(1L)))
        model.dialog() shouldBe MangaScreenModel.Dialog.DeleteChapters(listOf(chapter(1L)))
        model.showSettingsDialog()
        model.dialog() shouldBe MangaScreenModel.Dialog.SettingsSheet
        model.showTrackDialog()
        model.dialog() shouldBe MangaScreenModel.Dialog.TrackSheet
        model.showCoverDialog()
        model.dialog() shouldBe MangaScreenModel.Dialog.FullCover
        model.showEditMangaInfoDialog()
        model.dialog().shouldBeInstanceOf<MangaScreenModel.Dialog.EditMangaInfo>().manga.id shouldBe 1L
        model.dismissDialog()
        model.dialog().shouldBeNull()
    }

    @Test
    fun migrateAndMergeNeedData() {
        val model = harness.loaded()
        val other = manga().copy(id = 2L)
        model.showMigrateDialog(other)
        model.dialog() shouldBe MangaScreenModel.Dialog.Migrate(target = model.manga!!, current = other)
        model.dismissDialog()
        model.showEditMergedSettingsDialog()
        model.dialog().shouldBeNull()
        val data = mergedData(other)
        model.updateSuccessState { it.copy(mergedData = data) }
        model.showEditMergedSettingsDialog()
        model.dialog() shouldBe MangaScreenModel.Dialog.EditMergedSettings(data)
        harness.loading().showMigrateDialog(other)
    }

    @Test
    fun selectionFollowsTheList() {
        val model = harness.loaded()
        val rows = model.awaitSuccess().processedChapters
        model.toggleSelection(rows[0], selected = true)
        model.toggleSelection(rows[2], selected = true, fromLongPress = true)
        // Selection updates land on the model's scope; wait for each state rather than read it once.
        model.awaitSuccess { state -> state.chapters.count { it.selected } == 3 }
        model.invertSelection()
        model.awaitSuccess { !it.isAnySelected }
        model.toggleAllSelection(true)
        model.awaitSuccess { state -> state.chapters.all { it.selected } }
    }

    @Test
    fun nextUnreadNeedsState() {
        harness.loaded().getNextUnreadChapter() shouldBe chapter(1L)
        harness.loading().getNextUnreadChapter().shouldBeNull()
    }

    @Test
    fun dialogModelsAreValues() {
        val manga = manga()
        MangaScreenModel.Dialog.SetFetchInterval(manga).copy().manga shouldBe manga
        MangaScreenModel.Dialog.DuplicateManga(manga, emptyList()).copy().duplicates shouldBe emptyList()
        MangaScreenModel.Dialog.ChangeCategory(manga, emptyList()).copy().initialSelection shouldBe emptyList()
        MangaScreenModel.EXHRedirect(3L).copy().mangaId shouldBe 3L
        val combine = MangaScreenModel.CombineState(manga to emptyList(), null)
        combine.pagePreviewsState shouldBe PagePreviewState.Loading
        combine.copy(mergedData = null).mergedData.shouldBeNull()
    }
}
