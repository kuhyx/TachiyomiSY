package eu.kanade.tachiyomi.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import exh.md.utils.MdUtil
import exh.md.utils.getEnabledMangaDex
import exh.source.EH_SOURCE_ID
import exh.source.MERGED_SOURCE_ID
import exh.source.mangaDexSourceIds
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.LocalSource

@RunWith(RobolectricTestRunner::class)
internal class LibraryTabSelectionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = LibraryTabRig(compose)
    private val harness = rig.harness

    @Before
    fun setUp() {
        rig.start()
        harness.categories.value = listOf(libCategory(1L))
    }

    @After
    fun tearDown() = rig.stop()

    // Shows the tab over the given manga and selects all of them.
    private fun select(vararg manga: Manga) {
        harness.libraryManga.value = manga.map { libEntry(it) }
        rig.show()
        compose.waitForLabel(manga.first().title)
        rig.node(manga.first().title).performTouchInput { longClick() }
        compose.waitForLabel("Select all")
        rig.click("Select all")
    }

    private fun overflow(label: String) {
        rig.click("More")
        rig.click(label)
    }

    @Test
    fun markAsReadAndUnread() {
        select(libManga(1L))
        rig.click("Mark as read")
        coVerify(timeout = WAIT) { harness.setReadStatus.await(manga = libManga(1L), read = true) }
        rig.node("Manga 1").performTouchInput { longClick() }
        compose.waitForLabel("More")
        overflow("Mark as unread")
        coVerify(timeout = WAIT) { harness.setReadStatus.await(manga = libManga(1L), read = false) }
    }

    @Test
    fun categoriesAndDeleteOpenDialogs() {
        select(libManga(1L))
        rig.click("Set categories")
        compose.waitForLabel("Edit")
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        rig.click("Delete")
        compose.waitForLabel("From library")
    }

    @Test
    fun downloadQueuesUnreadChapters() {
        select(libManga(1L))
        rig.click("Download")
        rig.click("Unread")
        coVerify(timeout = WAIT) { harness.getNextChapters.await(1L, any<Boolean>()) }
    }

    @Test
    fun localEntriesCannotDownload() {
        select(libManga(1L, source = LocalSource.ID))
        compose.hasLabel("Download") shouldBe false
    }

    @Test
    fun migrateOpensTheConfig() {
        select(libManga(1L), libManga(2L, source = MERGED_SOURCE_ID))
        overflow("Migrate")
        compose.waitForLabel("opened:MigrationConfigScreen")
    }

    @Test
    fun mergedEntriesCannotMigrate() {
        select(libManga(1L, source = MERGED_SOURCE_ID))
        overflow("Migrate")
        ShadowToast.getTextOfLatestToast() shouldBe "No valid entry selected"
    }

    @Test
    fun cleanTitlesForGalleries() {
        select(libManga(1L, "[G] Name", source = EH_SOURCE_ID))
        overflow("Clean titles")
        verify(timeout = WAIT) { harness.setCustomMangaInfo.set(match { it.title == "Name" }) }
    }

    @Test
    fun recommendationsForSeveral() {
        select(libManga(1L), libManga(2L))
        overflow("Find recommendations")
        compose.waitUntil(WAIT) { ShadowDialog.getLatestDialog() != null }
    }

    @Test
    fun mangaDexAndResetInfo() {
        mockkStatic("exh.md.utils.MdSourcesKt")
        every { MdUtil.getEnabledMangaDex(any(), any()) } returns null
        val previous = mangaDexSourceIds
        try {
            mangaDexSourceIds = listOf(66L)
            val edited = spyk(libManga(2L, source = 66L)) { every { author } returns "x" }
            select(libManga(1L), edited)
            overflow("Add to MangaDex follows")
            compose.waitUntil(WAIT) { !compose.hasLabel("Select all") }
            rig.node("Manga 1").performTouchInput { longClick() }
            rig.click("Select all")
            overflow("Reset Info")
            verify(timeout = WAIT) { harness.setCustomMangaInfo.set(match { it.id == 2L && it.author == null }) }
        } finally {
            mangaDexSourceIds = previous
        }
    }
}
