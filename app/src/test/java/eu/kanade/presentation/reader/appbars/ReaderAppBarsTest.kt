package eu.kanade.presentation.reader.appbars

import eu.kanade.presentation.util.invokeClick
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.reader.components.ChapterNavigation
import eu.kanade.presentation.reader.components.ChapterNavigatorType
import io.kotest.matchers.collections.shouldContain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class ReaderAppBarsTest {
    @get:Rule
    val compose = createComposeRule()

    private val callbacks = ReaderBarCallbacks()
    private var visible by mutableStateOf(true)

    private fun show(type: ChapterNavigatorType) {
        compose.setContent {
            MaterialTheme {
                ReaderAppBars(
                    visible = visible,
                    mangaTitle = "Manga",
                    chapterTitle = "Chapter",
                    navigateUp = { callbacks.events += "up" },
                    onClickTopAppBar = { callbacks.events += "top" },
                    chapterNavigatorType = type,
                    navigation = ChapterNavigation(
                        onNextChapter = {},
                        enabledNext = true,
                        onPreviousChapter = {},
                        enabledPrevious = true,
                        onPageIndexChange = {},
                        onPageIndexChangeFinished = {},
                    ),
                    currentPage = 1,
                    totalPages = 5,
                    settings = callbacks.settings(),
                    onClickSettings = { callbacks.events += "settings" },
                    isExhToolsVisible = false,
                    onSetExhUtilsVisibility = {},
                    autoScroll = callbacks.autoScroll(),
                    exhPageActions = callbacks.pageActions(),
                    currentPageText = "1",
                    syBottomBar = syState(),
                    syBottomBarActions = callbacks.actions(),
                )
            }
        }
        compose.waitForIdle()
    }

    private fun hideAndShow() {
        visible = false
        compose.mainClock.advanceTimeBy(1_000L)
        compose.waitForIdle()
        visible = true
        compose.mainClock.advanceTimeBy(1_000L)
        compose.waitForIdle()
    }

    @Test
    fun horizontalBarsForward() {
        show(ChapterNavigatorType.HORIZONTAL_LTR)
        compose.onNodeWithText("Manga").performClick()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.onNodeWithContentDescription("Settings").invokeClick()
        hideAndShow()
        callbacks.events shouldContain "top"
        callbacks.events shouldContain "settings"
    }

    @Test
    fun verticalRailOnTheLeft() {
        show(ChapterNavigatorType.VERTICAL_LEFT)
        hideAndShow()
        compose.onNodeWithText("5").assertExists()
    }

    @Test
    @Config(qualifiers = "night")
    fun verticalRailOnTheRight() {
        show(ChapterNavigatorType.VERTICAL_RIGHT)
        hideAndShow()
        compose.onNodeWithText("5").assertExists()
    }

    @Test
    fun topBarDefaults() {
        compose.setContent { MaterialTheme { ReaderTopBar(mangaTitle = null, chapterTitle = null, navigateUp = {}) } }
        compose.onNodeWithContentDescription("Navigate up").assertExists()
    }
}
