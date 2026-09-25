package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Dialog
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ReaderStateTest {

    private val harness = ReaderVmHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun dialogsOpenAndClose() {
        val vm = harness.viewModel()
        val opened = listOf(
            vm::openChapterListDialog to Dialog.ChapterList,
            vm::openAutoScrollHelpDialog to Dialog.AutoScrollHelp,
            vm::openBoostPageHelp to Dialog.BoostPageHelp,
            vm::openRetryAllHelp to Dialog.RetryAllHelp,
            vm::showLoadingDialog to Dialog.Loading,
            vm::openReadingModeSelectDialog to Dialog.ReadingModeSelect,
            vm::openOrientationSelectDialog to Dialog.OrientationModeSelect,
            vm::openSettingsDialog to Dialog.Settings,
        )
        for ((open, dialog) in opened) {
            open()
            vm.state.value.dialog shouldBe dialog
        }
        vm.closeDialog()
        vm.state.value.dialog.shouldBeNull()
    }

    @Test
    fun pageDialogCarriesPages() {
        val vm = harness.viewModel()
        val page = ReaderPage(0)
        val extra = ReaderPage(1)
        vm.openPageDialog(page)
        vm.state.value.dialog shouldBe Dialog.PageActions(page, null)
        vm.openPageDialog(page, extra)
        vm.state.value.dialog shouldBe Dialog.PageActions(page, extra)
    }

    @Test
    fun togglesUpdateState() {
        val vm = harness.viewModel()
        vm.showMenus(true)
        vm.showEhUtils(true)
        vm.setIndexChapterToShift(4L)
        vm.setIndexPageToShift(2)
        vm.setDoublePages(true)
        vm.toggleAutoScroll(true)
        vm.setBrightnessOverlayValue(-40)
        val state = vm.state.value
        state.menuVisible shouldBe true
        state.ehUtilsVisible shouldBe true
        state.indexChapterToShift shouldBe 4L
        state.indexPageToShift shouldBe 2
        state.doublePages shouldBe true
        state.autoScroll shouldBe true
        state.brightnessOverlayValue shouldBe -40
    }
}
