package eu.kanade.presentation.manga

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.MergedMangaData
import eu.kanade.tachiyomi.ui.manga.PagePreviewState
import exh.source.EH_SOURCE_ID
import io.kotest.matchers.collections.shouldContain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h3000dp")
internal class MangaScreenRowsTest {
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val host = MangaScreenHost(compose)

    @Before
    fun setUp() = host.start()

    @After
    fun tearDown() = host.stop()

    private fun rows() = listOf(
        chapterItem(mangaChapter(1L, read = true, lastPageRead = 4L, dateUpload = 86_400_000L)),
        chapterItem(mangaChapter(2L, scanlator = " "), showScanlator = true),
        chapterItem(mangaChapter(3L, scanlator = "Group")),
        chapterItem(mangaChapter(4L, scanlator = null), Download.State.DOWNLOADED, sourceName = "Other"),
    )

    @Test
    fun numberedGalleryRows() {
        val manga = screenManga(source = EH_SOURCE_ID, flags = Manga.CHAPTER_DISPLAY_NUMBER)
        host.show(screenState(chapters = rows(), manga = manga, alwaysProgress = true))
        compose.onNodeWithText("Chapter 1").assertExists()
        compose.onNodeWithText("Page: 5").assertExists()
    }

    @Test
    fun mergedLocalRows() {
        val local = screenManga(source = 0L)
        val merged =
            MergedMangaData(references = emptyList(), manga = mapOf(1L to local), sources = listOf(plainSource()))
        host.show(screenState(chapters = rows(), mergedData = merged))
        compose.onNodeWithText("Chapter 3").performClick()
        host.state = screenState(chapters = rows(), mergedData = merged.copy(manga = emptyMap()))
        compose.waitForIdle()
        compose.onNodeWithText("Chapter 2").performClick()
        host.events shouldContain "open 2"
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp")
    fun scrollingFadesTheToolbar() {
        val many = (1L..40L).map { chapterItem(mangaChapter(it)) }
        host.show(screenState(chapters = many, previews = PagePreviewState.Loading))
        host.actions = host.recorder.build(rows = 0)
        compose.waitForIdle()
        compose.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(30)
        compose.waitForIdle()
        compose.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(0)
        compose.waitForIdle()
        compose.onAllNodes(hasScrollToIndexAction())[0].performTouchInput {
            swipeUp(startY = centerY, endY = centerY - 40f)
        }
        compose.waitForIdle()
    }

    @Test
    fun tabletOverflowWithoutButtons() {
        host.show(screenState(overflow = true), isTablet = true)
        compose.onNodeWithText("See Recommendations").assertDoesNotExist()
        compose.onNodeWithText("Action").performClick()
        compose.onNodeWithText("Copy to clipboard").performClick()
    }

    @Test
    fun selectedRowLongClick() {
        host.show(screenState(chapters = listOf(chapterItem(mangaChapter(1L), selected = true))))
        compose.onNodeWithText("Chapter 1").performTouchInput { longClick() }
        host.events shouldContain "select 1 false true"
    }

    @Test
    fun emptyTagIsNotCopied() {
        host.show(screenState(manga = screenManga(genre = listOf("", "Action"))), isTablet = true)
        compose.onAllNodes(hasText(""))[0].performClick()
        compose.onNodeWithText("Copy to clipboard").performClick()
        compose.onNodeWithText("Action").assertExists()
    }
}
