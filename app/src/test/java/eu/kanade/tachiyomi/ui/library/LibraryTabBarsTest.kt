package eu.kanade.tachiyomi.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.tachiyomi.ui.base.libraryManga
import eu.kanade.tachiyomi.ui.home.HomeScreen
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class LibraryTabBarsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = LibraryTabRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun selectAlpha() {
        rig.show()
        rig.select("Alpha")
        rig.waitFor("Mark as read")
    }

    @Test
    fun selectionFromTheToolbar() {
        selectAlpha()
        // Selecting hides the home screen's bottom bar; nothing else receives that here.
        compose.waitUntil(LIBRARY_WAIT) { HomeScreen.showBottomNavEvent.tryReceive().getOrNull() == false }
        rig.click("Select inverse")
        rig.waitUntilGone("Mark as read")
        rig.select("Alpha")
        rig.click("Select all")
        compose.activity.onBackPressedDispatcher.onBackPressed()
        rig.waitUntilGone("Mark as read")
    }

    @Test
    fun markingRead() {
        selectAlpha()
        rig.click("Mark as read")
        coVerify(timeout = LIBRARY_WAIT) { rig.harness.setReadStatus.await(manga = any(), read = true) }
        rig.select("Alpha")
        rig.click("More")
        rig.click("Mark as unread")
        coVerify(timeout = LIBRARY_WAIT) { rig.harness.setReadStatus.await(manga = any(), read = false) }
    }

    @Test
    fun downloadingNextChapters() {
        coEvery { rig.harness.getNextChapters.await(1L) } returns emptyList()
        selectAlpha()
        rig.click("Download")
        rig.click("Next chapter")
        coVerify(timeout = LIBRARY_WAIT) { rig.harness.getNextChapters.await(1L) }
    }

    @Test
    fun localEntriesCannotDownload() {
        rig.harness.library.value = listOf(libraryManga(1, manga(1, "Alpha").copy(source = 0), listOf(1L)))
        selectAlpha()
        rig.node("Download").assertDoesNotExist()
    }

    @Test
    fun migratingOpensTheConfig() {
        selectAlpha()
        rig.click("More")
        rig.click("Migrate")
        rig.waitFor("opened:MigrationConfigScreen")
    }

    @Test
    fun mergedEntriesCannotMigrate() {
        val merged = manga(1, "Alpha").copy(source = MERGED_SOURCE_ID)
        rig.harness.library.value = listOf(libraryManga(1, merged, listOf(1L)))
        coEvery { rig.harness.getMergedMangaById.await(1L) } returns emptyList()
        selectAlpha()
        rig.click("More")
        rig.click("Migrate")
        ShadowToast.getTextOfLatestToast() shouldBe "No valid entry selected"
    }

    @Test
    fun deleteAndCategoryDialogs() {
        coEvery { rig.harness.getCategories.await(1L) } returns listOf(rig.reading)
        selectAlpha()
        rig.click("Delete")
        rig.waitFor("From library")
        rig.click("Cancel")
        rig.click("Set categories")
        rig.waitFor("Edit")
    }
}
