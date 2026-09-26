package eu.kanade.tachiyomi.ui.library

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.startNow
import eu.kanade.tachiyomi.ui.base.libraryManga
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.verify
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

@RunWith(RobolectricTestRunner::class)
internal class LibraryTabContentTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = LibraryTabRig(compose)
    private val chapter = Chapter.create().copy(id = 5, mangaId = 1, name = "c5")

    @Before
    fun setUp() {
        rig.start()
        rig.harness.libraryPreferences.showContinueReadingButton.set(true)
        rig.harness.library.value = listOf(libraryManga(1, manga(1, "Alpha"), listOf(1L), totalChapters = 3))
        coEvery { rig.harness.getMergedMangaById.await(1L) } returns emptyList()
    }

    @After
    fun tearDown() = rig.stop()

    @Test
    fun continueReadingOpensTheReader() {
        coEvery { rig.harness.getChaptersByMangaId.await(1L, applyScanlatorFilter = true) } returns listOf(chapter)
        rig.show()
        rig.click("Resume")
        compose.waitUntil(LIBRARY_WAIT) { shadowOf(compose.activity).peekNextStartedActivity() != null }
        shadowOf(compose.activity).nextStartedActivity.component?.className shouldBe ReaderActivity::class.java.name
    }

    @Test
    fun continueReadingWithoutAChapter() {
        coEvery { rig.harness.getChaptersByMangaId.await(1L, applyScanlatorFilter = true) } returns emptyList()
        rig.show()
        rig.click("Resume")
        rig.waitFor("Next chapter not found")
    }

    @Test
    fun searchingGlobally() {
        rig.show()
        CoroutineScope(Dispatchers.IO).launch { LibraryTab.search("alp") }
        rig.waitFor("Search for \"alp\" globally")
        rig.click("Search for \"alp\" globally")
        rig.waitFor("opened:GlobalSearchScreen")
    }

    @Test
    fun pullingRefreshesTheCategory() {
        rig.show()
        compose.onAllNodes(hasText("Alpha"), useUnmergedTree = true).onFirst().performTouchInput {
            swipeDown(startY = 0f, endY = PULL_DISTANCE, durationMillis = PULL_MILLIS)
        }
        verify(timeout = LIBRARY_WAIT) {
            LibraryUpdateJob.startNow(any<Context>(), rig.reading, any(), any(), any())
        }
    }

    @Test
    fun filtersAndTabsOnAnEmptyLibrary() {
        rig.harness.library.value = emptyList()
        rig.harness.libraryPreferences.categoryTabs.set(false)
        rig.harness.libraryPreferences.filterUnread.set(TriState.ENABLED_IS)
        rig.show("No match found")
        // Page tabs show for a real query or when asked for; an empty query is no search.
        CoroutineScope(Dispatchers.IO).launch { LibraryTab.search("") }
        compose.waitUntil(LIBRARY_WAIT) { rig.model().state.value.searchQuery == "" }
        compose.waitForIdle()
        CoroutineScope(Dispatchers.IO).launch { LibraryTab.search("x") }
        compose.waitUntil(LIBRARY_WAIT) { rig.model().state.value.searchQuery == "x" }
        compose.waitForIdle()
        rig.harness.libraryPreferences.categoryTabs.set(true)
        compose.waitUntil(LIBRARY_WAIT) { rig.model().state.value.showCategoryTabs }
        compose.waitForIdle()
    }

    @Test
    fun clickingDuringSelectionToggles() {
        rig.show()
        rig.select("Alpha")
        rig.waitFor("Mark as read")
        rig.click("Alpha")
        rig.waitUntilGone("Mark as read")
    }

    private companion object {
        const val PULL_DISTANCE = 1_500f
        const val PULL_MILLIS = 400L
    }
}
