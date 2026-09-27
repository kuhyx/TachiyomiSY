package eu.kanade.presentation.manga

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.presentation.util.PresentationKoin
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class ChapterSettingsDialogTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val base = BasePreferences(ApplicationProvider.getApplicationContext(), koin.store)
    private val events = mutableListOf<String>()

    @Before
    fun setUp() = koin.start(module { single { base } })

    @After
    fun tearDown() = koin.stop()

    private fun show(manga: Manga?, scanlatorActive: Boolean = false) {
        compose.setContent {
            MaterialTheme {
                ChapterSettingsDialog(
                    onDismissRequest = { events += "dismiss" },
                    manga = manga,
                    onDownloadFilterChanged = { events += "downloaded $it" },
                    onUnreadFilterChanged = { events += "unread $it" },
                    onBookmarkedFilterChanged = { events += "bookmarked $it" },
                    scanlatorFilterActive = scanlatorActive,
                    onScanlatorFilterClicked = { events += "scanlator" },
                    onSortModeChanged = { events += "sort $it" },
                    onDisplayModeChanged = { events += "display $it" },
                    onSetAsDefault = { events += "default $it" },
                    onResetToDefault = { events += "reset" },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun tab(title: String) {
        compose.onNodeWithText(title).performClick()
        compose.waitForIdle()
    }

    @Test
    fun filterPageForwards() {
        show(manga = null)
        listOf("Downloaded", "Unread", "Bookmarked", "Scanlator").forEach { compose.onNodeWithText(it).performClick() }
        events shouldContainExactly listOf(
            "downloaded ENABLED_IS",
            "unread ENABLED_IS",
            "bookmarked ENABLED_IS",
            "scanlator",
        )
    }

    @Test
    fun downloadedOnlyLocksFilter() {
        base.downloadedOnly.set(true)
        val manga = Manga.create().copy(chapterFlags = Manga.CHAPTER_SHOW_UNREAD or Manga.CHAPTER_SORT_DESC)
        show(manga = manga, scanlatorActive = true)
        compose.onNodeWithText("Downloaded").performClick()
        compose.onNodeWithText("Unread").performClick()
        events shouldContainExactly listOf("unread ENABLED_NOT")
    }

    @Test
    fun sortAndDisplayPages() {
        show(manga = Manga.create().copy(chapterFlags = Manga.CHAPTER_SORTING_NUMBER))
        tab("Sort")
        listOf("By source", "By chapter number", "By upload date", "Alphabetically").forEach {
            compose.onNodeWithText(it).performClick()
        }
        tab("Display")
        compose.onNodeWithText("Source title").performClick()
        compose.onNodeWithText("Chapter number").performClick()
        events shouldContainExactly listOf(
            "sort ${Manga.CHAPTER_SORTING_SOURCE}",
            "sort ${Manga.CHAPTER_SORTING_NUMBER}",
            "sort ${Manga.CHAPTER_SORTING_UPLOAD_DATE}",
            "sort ${Manga.CHAPTER_SORTING_ALPHABET}",
            "display ${Manga.CHAPTER_DISPLAY_NAME}",
            "display ${Manga.CHAPTER_DISPLAY_NUMBER}",
        )
    }

    @Test
    fun defaultPagesWithoutManga() {
        show(manga = null)
        tab("Sort")
        compose.onNodeWithText("By source").performClick()
        tab("Display")
        compose.onNodeWithText("Source title").performClick()
        events shouldContainExactly listOf("sort 0", "display 0")
    }

    @Test
    fun setAsDefaultAndReset() {
        show(manga = null)
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Reset").performClick()
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Set as default").performClick()
        compose.onNodeWithText("Also apply to all entries in my library").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Set as default").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Chapter settings").assertDoesNotExist()
        events shouldContainExactly listOf("reset", "default true")
    }

    @Test
    fun mangaDefaultsToNone() {
        compose.setContent {
            MaterialTheme {
                ChapterSettingsDialog(
                    onDismissRequest = {},
                    onDownloadFilterChanged = { events += "downloaded $it" },
                    onUnreadFilterChanged = {},
                    onBookmarkedFilterChanged = {},
                    scanlatorFilterActive = false,
                    onScanlatorFilterClicked = {},
                    onSortModeChanged = {},
                    onDisplayModeChanged = {},
                    onSetAsDefault = {},
                    onResetToDefault = {},
                )
            }
        }
        compose.onNodeWithText("Downloaded").performClick()
        events shouldContainExactly listOf("downloaded ENABLED_IS")
    }
}
