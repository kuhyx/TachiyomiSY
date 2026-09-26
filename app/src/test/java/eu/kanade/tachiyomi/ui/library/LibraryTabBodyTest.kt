package eu.kanade.tachiyomi.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.model.LibraryGroup

@RunWith(RobolectricTestRunner::class)
internal class LibraryTabBodyTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = LibraryTabRig(compose)
    private val harness = rig.harness

    @Before
    fun setUp() {
        rig.start()
        harness.categories.value = listOf(libCategory(1L), libCategory(2L))
        harness.libraryManga.value = listOf(
            libEntry(libManga(1L), total = 2L),
            libEntry(libManga(2L), total = 2L),
            libEntry(libManga(3L), listOf(2L)),
        )
    }

    @After
    fun tearDown() = rig.stop()

    private fun showEntries() {
        rig.show()
        compose.waitForLabel("Manga 1")
    }

    @Test
    fun continueWithoutNextChapter() {
        harness.libraryPreferences.showContinueReadingButton.set(true)
        showEntries()
        compose.onAllNodesWithContentDescription("Resume")[0].performClick()
        compose.waitForLabel("Next chapter not found")
    }

    @Test
    fun continueOpensTheReader() {
        val chapter = Chapter.create().copy(id = 9L, mangaId = 1L, name = "C9", url = "/c/9")
        coEvery { harness.getChapters.await(any(), any()) } returns listOf(chapter)
        harness.libraryPreferences.showContinueReadingButton.set(true)
        showEntries()
        compose.onAllNodesWithContentDescription("Resume")[0].performClick()
        compose.waitUntil(WAIT) { shadowOf(compose.activity).peekNextStartedActivity() != null }
        shadowOf(compose.activity).nextStartedActivity.component?.className shouldBe ReaderActivity::class.java.name
    }

    @Test
    fun emptySearchOffersGlobalSearch() {
        showEntries()
        val sender = CoroutineScope(Dispatchers.Default).launch { LibraryTab.search("zzz") }
        try {
            compose.waitForLabel("Search for \"zzz\" globally")
        } finally {
            sender.cancel()
        }
        rig.click("Search for \"zzz\" globally")
        compose.waitForLabel("opened:GlobalSearchScreen")
    }

    @Test
    fun emptySearchKeepsTheList() {
        showEntries()
        val sender = CoroutineScope(Dispatchers.Default).launch { LibraryTab.search("") }
        try {
            // An empty query opens the search bar, which shows its hint.
            compose.waitForLabel("Search…")
        } finally {
            sender.cancel()
        }
        compose.hasLabel("Manga 1") shouldBe true
    }

    @Test
    fun noExhSyncWithoutHentai() {
        harness.exhPreferences.isHentaiEnabled.set(false)
        showEntries()
        rig.click("More options")
        compose.hasLabel("Sync EH favorites") shouldBe false
    }

    @Test
    fun filteredLibraryIsNotEmpty() {
        harness.libraryManga.value = emptyList()
        harness.libraryPreferences.filterUnread.set(TriState.ENABLED_IS)
        rig.show()
        compose.waitForLabel("No match found")
        compose.hasLabel("Your library is empty") shouldBe false
    }

    @Test
    fun tabsChangeTheCategory() {
        harness.libraryPreferences.categoryTabs.set(true)
        showEntries()
        rig.click("Cat 2")
        compose.waitUntil(WAIT) { harness.libraryPreferences.lastUsedCategory.get() == 1 }
    }

    @Test
    fun pullToRefreshUpdatesCategory() {
        showEntries()
        compose.onNodeWithText("Manga 1").performTouchInput { swipeDown() }
        compose.mainClock.advanceTimeBy(2_000L)
        compose.waitUntil(WAIT) { rig.updates.isNotEmpty() }
        rig.updates.first().category?.id shouldBe 1L
    }

    @Test
    fun longPressSelectsARange() {
        showEntries()
        rig.node("Manga 1").performTouchInput { longClick() }
        compose.waitForLabel("Select all")
        rig.node("Manga 2").performTouchInput { longClick() }
        compose.waitForLabel("2")
        rig.click("Manga 2")
        compose.waitForLabel("1")
    }

    @Test
    fun refreshNamesTheGroup() {
        rig.updateStarts = true
        showEntries()
        // Each grouping titles the bar with its first group's name.
        val titles = mapOf(
            LibraryGroup.BY_SOURCE to "0",
            LibraryGroup.BY_TRACK_STATUS to "Not tracked",
            LibraryGroup.BY_STATUS to "Unknown",
            // Observed: with the library ungrouped the bar reads "Library".
            LibraryGroup.UNGROUPED to "Library",
        )
        val groups = titles.keys.toList()
        titles.forEach { (group, title) ->
            harness.libraryPreferences.groupLibraryBy.set(group)
            compose.waitForLabel(title)
            rig.action("Update category")
            rig.action("Update library")
        }
        rig.updates.map { it.group } shouldBe groups.flatMap { listOf(it, it) }
        rig.updates.map { it.category } shouldBe List(groups.size * 2) { null }
        rig.updates.map { it.groupExtra } shouldBe listOf("1", null, "7", null, "0", null, null, null)
    }
}
