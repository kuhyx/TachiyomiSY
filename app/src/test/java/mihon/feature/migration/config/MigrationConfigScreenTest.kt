package mihon.feature.migration.config

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import eu.kanade.presentation.util.ProvideBack
import eu.kanade.presentation.util.TestBackOwner
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.library.hasLabel
import eu.kanade.tachiyomi.ui.library.waitForLabel
import eu.kanade.tachiyomi.ui.manga.clearVoyagerScopes
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch

@RunWith(RobolectricTestRunner::class)
internal class MigrationConfigScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = MigrationConfigHarness()
    private val back = TestBackOwner()

    @Before
    fun setUp() {
        harness.start()
        harness.sources = {
            listOf(httpSource(1L, name = "Alpha"), httpSource(2L, name = "Beta"), httpSource(3L, name = "Gamma"))
        }
    }

    @After
    fun tearDown() {
        clearVoyagerScopes()
        harness.stop()
    }

    private fun show(screen: MigrationConfigScreen) {
        compose.setContent { ProvideBack(back) { ScreenHost(screen) } }
        compose.waitForLabel("Continue")
    }

    private fun click(label: String) {
        compose.onNodeWithText(label, useUnmergedTree = true).performClick()
        compose.waitForIdle()
    }

    private fun selected(): List<Long> = harness.preferences.migrationSources.get()

    @Test
    fun aSpinnerShowsWhileLoading() {
        val latch = CountDownLatch(1)
        val all = harness.sources
        harness.sources = {
            latch.await()
            all()
        }
        compose.setContent { ScreenHost(MigrationConfigScreen(listOf(1L, 2L))) }
        compose.waitForIdle()
        compose.hasLabel("Continue") shouldBe false
        latch.countDown()
        compose.waitForLabel("Selected")
        compose.hasLabel("Available") shouldBe false
    }

    @Test
    fun oneEntryGoesStraightToSearch() {
        show(MigrationConfigScreen(7L))
        click("Continue")
        compose.waitForLabel("opened:MigrateSearchScreen")
        selected() shouldContainExactly listOf(1L, 2L, 3L)
    }

    @Test
    fun manyEntriesOpenTheSheetFirst() {
        show(MigrationConfigScreen(listOf(1L, 2L)))
        click("Continue")
        compose.waitForLabel("Data to migrate")
        compose.onNodeWithText("Additional keywords (optional)").performTextInput("  extra  ")
        compose.waitForIdle()
        compose.onNode(hasText("Continue") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForLabel("opened:MigrationListScreen")
    }

    @Test
    fun backClosesTheSheet() {
        show(MigrationConfigScreen(listOf(1L, 2L)))
        click("Continue")
        compose.waitForLabel("Data to migrate")
        compose.runOnIdle { back.pressBack() }
        compose.waitForIdle()
        compose.hasLabel("Data to migrate") shouldBe false
        compose.hasLabel("Alpha") shouldBe true
    }

    @Test
    fun rowsToggleTheirSource() {
        show(MigrationConfigScreen(listOf(1L, 2L)))
        click("Beta")
        compose.waitForLabel("Available")
        selected() shouldContainExactly listOf(1L, 3L)
        click("Beta")
        selected() shouldContainExactly listOf(1L, 3L, 2L)
    }

    @Test
    fun barActionsApplyPresets() {
        harness.preferences.pinnedSources.set(setOf("2"))
        harness.preferences.migrationSources.set(listOf(1L))
        show(MigrationConfigScreen(listOf(1L, 2L)))
        compose.onNodeWithContentDescription("Select none").performClick()
        compose.waitForIdle()
        selected() shouldBe emptyList()
        compose.hasLabel("Selected") shouldBe false
        compose.onNodeWithContentDescription("Select all").performClick()
        compose.waitForIdle()
        selected().size shouldBe 3
        compose.onNodeWithContentDescription("More options").performClick()
        click("Select pinned sources")
        selected() shouldContainExactly listOf(2L)
        compose.onNodeWithContentDescription("More options").performClick()
        click("Select enabled sources")
        selected().size shouldBe 3
    }

    @Test
    fun languagesShowWhenTheyDiffer() {
        harness.sources = { listOf(httpSource(1L, name = "Alpha"), httpSource(2L, name = "Beta", lang = "ja")) }
        show(MigrationConfigScreen(listOf(1L, 2L)))
        compose.waitForLabel("JA")
        compose.hasLabel("EN") shouldBe true
    }

    @Test
    fun oneLanguageHidesThePill() {
        show(MigrationConfigScreen(listOf(1L, 2L)))
        compose.hasLabel("EN") shouldBe false
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.waitForIdle()
        compose.hasLabel("Alpha") shouldBe true
    }
}
