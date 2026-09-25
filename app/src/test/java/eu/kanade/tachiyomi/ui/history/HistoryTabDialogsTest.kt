package eu.kanade.tachiyomi.ui.history

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.MangaWithChapterCount

@RunWith(RobolectricTestRunner::class)
internal class HistoryTabDialogsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = HistoryTabRig(compose)
    private val duplicate by lazy { rig.manga.copy(id = 9L, ogTitle = "Dup", favorite = true) }

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = stopKoin()

    @Test
    fun removingOneEntry() {
        rig.show()
        rig.click("Delete")
        rig.click("Remove")
        coVerify(timeout = TAB_WAIT) { rig.harness.removeHistory.await(any<HistoryWithRelations>()) }
    }

    @Test
    fun removingEverythingOfAManga() {
        rig.show()
        rig.click("Delete")
        rig.click("Reset all chapters for this entry")
        rig.click("Remove")
        coVerify(timeout = TAB_WAIT) { rig.harness.removeHistory.await(1L) }
    }

    @Test
    fun duplicatesCanBeAddedAnyway() {
        coEvery { rig.harness.getDuplicate(rig.manga) } returns listOf(MangaWithChapterCount(duplicate, 2))
        rig.show()
        rig.click("Add to library")
        rig.waitFor("Possible duplicates")
        rig.click("Add anyway")
        coVerify(timeout = TAB_WAIT) { rig.harness.updateManga.awaitUpdateFavorite(1L, true) }
    }

    @Test
    fun duplicateOpensOrMigrates() {
        coEvery { rig.harness.getDuplicate(rig.manga) } returns listOf(MangaWithChapterCount(duplicate, 2))
        rig.show()
        rig.click("Add to library")
        rig.waitFor("Dup")
        rig.node("Dup").performTouchInput { longClick() }
        rig.waitFor("opened:MangaScreen")
    }

    @Test
    fun duplicateMigrateDialog() {
        coEvery { rig.harness.getDuplicate(rig.manga) } returns listOf(MangaWithChapterCount(duplicate, 2))
        rig.show()
        rig.click("Add to library")
        rig.waitFor("Dup")
        rig.click("Dup")
        compose.waitForIdle()
    }

    @Test
    fun askingForCategories() {
        coEvery { rig.harness.getCategories.await() } returns listOf(category(1))
        rig.show()
        rig.click("Add to library")
        rig.waitFor("Set categories")
        rig.click("OK")
        coVerify(timeout = TAB_WAIT) { rig.harness.setMangaCategories.await(1L, any()) }
        rig.click("Add to library")
        rig.waitFor("Edit")
        rig.click("Edit")
        rig.waitFor("opened:CategoryScreen")
    }
}
