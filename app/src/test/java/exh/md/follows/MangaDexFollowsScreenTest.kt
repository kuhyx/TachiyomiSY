package exh.md.follows

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.library.hasLabel
import eu.kanade.tachiyomi.ui.library.waitForLabel
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category

@RunWith(RobolectricTestRunner::class)
internal class MangaDexFollowsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val rig = FollowsRig()

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun show() {
        compose.setContent { ScreenHost(MangaDexFollowsScreen(1L)) }
        compose.waitForLabel("MangaDex follows")
        compose.waitForLabel("Plain")
    }

    private fun click(label: String) {
        compose.waitForLabel(label)
        compose.onNodeWithText(label).performClick()
        compose.waitForIdle()
    }

    private fun hold(label: String) {
        compose.onNodeWithText(label).performTouchInput { longClick() }
        compose.waitForIdle()
    }

    // The favourite toggles run on the main dispatcher, so the main looper must idle while waiting.
    private fun updated() = eventually { runCatching { coVerify { rig.browse.updateManga.await(any()) } }.isSuccess }

    @Test
    fun aSpinnerShowsUntilSourcesLoad() {
        rig.loaded.value = false
        compose.setContent { ScreenHost(MangaDexFollowsScreen(1L)) }
        compose.waitForIdle()
        compose.hasLabel("MangaDex follows") shouldBe false
        rig.loaded.value = true
        compose.waitForLabel("Plain")
    }

    @Test
    fun clickOpensTheEntry() {
        show()
        click("Plain")
        compose.waitForLabel("opened:MangaScreen")
    }

    @Test
    fun holdingAFavoriteAsksToRemove() {
        show()
        hold("Fav")
        click("Cancel")
        compose.hasLabel("Remove") shouldBe false
        hold("Fav")
        click("Remove")
        updated()
    }

    @Test
    fun holdingAddsWithoutCategories() {
        show()
        hold("Plain")
        updated()
    }

    @Test
    fun holdingAsksForCategories() {
        rig.browse.categories.value = listOf(Category(id = 5L, name = "Five", order = 1L, flags = 0L))
        show()
        hold("Plain")
        click("OK")
        coVerify(timeout = 5_000) { rig.browse.setMangaCategories.await(13L, any()) }
        hold("Plain")
        click("Edit")
        compose.waitForLabel("opened:CategoryScreen")
    }

    @Test
    fun duplicatesCanBeAddedAnyway() {
        show()
        hold("Dup")
        compose.waitForLabel("Add anyway")
        // The footer sits below the small test window, so a touch would land on the scrim.
        compose.onNodeWithText("Add anyway").performSemanticsAction(SemanticsActions.OnClick)
        updated()
    }

    @Test
    fun duplicatesCanBeOpened() {
        show()
        hold("Dup")
        compose.waitForLabel("Older copy")
        hold("Older copy")
        compose.waitForLabel("opened:MangaScreen")
    }

    @Test
    fun duplicatesCanBeMigrated() {
        show()
        hold("Dup")
        click("Older copy")
        click("Copy")
        coVerify(timeout = 5_000) { rig.migrate(any(), any(), false, any()) }
        eventually { !compose.hasLabel("Copy") }
        hold("Dup")
        click("Older copy")
        click("Show entry")
        compose.waitForLabel("opened:MangaScreen")
    }

    @Test
    fun displayModeAndBack() {
        show()
        compose.onNodeWithContentDescription("Display mode").performClick()
        click("List")
        rig.browse.koin.sourcePreferences.sourceDisplayMode.get().toString() shouldBe "List"
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.waitForIdle()
    }
}
