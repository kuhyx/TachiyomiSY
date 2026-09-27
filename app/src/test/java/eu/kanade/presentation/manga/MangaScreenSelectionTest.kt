package eu.kanade.presentation.manga

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h3000dp")
internal class MangaScreenSelectionTest {
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val host = MangaScreenHost(compose)

    @Before
    fun setUp() = host.start()

    @After
    fun tearDown() = host.stop()

    private fun mixed() = listOf(
        chapterItem(mangaChapter(1L), selected = true),
        chapterItem(mangaChapter(2L, read = true, bookmark = true), Download.State.DOWNLOADED, selected = true),
        chapterItem(mangaChapter(3L)),
    )

    private fun clickAll(vararg labels: String) {
        labels.forEach { host.click(it, lowest = true) }
    }

    @Test
    fun mixedSelectionMenu() {
        host.show(screenState(chapters = mixed()))
        compose.onNodeWithText("2").assertExists()
        clickAll("Bookmark chapter", "Mark as read", "Mark as unread", "Download", "Delete")
        compose.onNodeWithContentDescription("Unbookmark chapter").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mark previous as read").assertDoesNotExist()
        host.events shouldContainExactly listOf(
            "bookmark [2, 1] true",
            "read [2, 1] true",
            "read [2, 1] false",
            "chapter download [2, 1] START",
            "delete [2, 1]",
        )
    }

    @Test
    fun singleSelectionMenu() {
        val single = chapterItem(mangaChapter(2L, read = true, bookmark = true), selected = true)
        host.show(screenState(chapters = listOf(single, chapterItem(mangaChapter(1L, lastPageRead = 2L)))))
        clickAll("Unbookmark chapter", "Mark previous as read", "Mark as unread", "Download")
        compose.onNodeWithContentDescription("Bookmark chapter").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mark as read").assertDoesNotExist()
        compose.onNodeWithContentDescription("Delete").assertDoesNotExist()
        host.events shouldContainExactly listOf(
            "bookmark [2] false",
            "previous 2",
            "read [2] false",
            "chapter download [2] START",
        )
    }

    @Test
    fun noDownloadWithoutCallback() {
        host.actions = host.recorder.build(optional = false)
        host.show(screenState(chapters = listOf(chapterItem(mangaChapter(1L, lastPageRead = 1L), selected = true))))
        host.count("Download") shouldBe 1
        compose.onNodeWithContentDescription("Mark as unread").assertExists()
    }

    @Test
    fun actionModeToolbar() {
        host.show(screenState(chapters = mixed()))
        clickAll("Select all", "Select inverse", "Cancel")
        compose.onNodeWithText("Chapter 1").performClick()
        compose.onNodeWithText("Chapter 3").performClick()
        compose.onNodeWithText("Start").assertDoesNotExist()
        host.events shouldContainExactly listOf(
            "all true",
            "invert",
            "all false",
            "select 1 false false",
            "select 3 true false",
        )
    }

    @Test
    fun backClearsSelection() {
        host.show(screenState(chapters = mixed()))
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        host.tablet = true
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        host.events shouldContainExactly listOf("all false", "all false")
    }

    @Test
    fun recomposesWithNewState() {
        host.show(screenState(chapters = mixed()))
        host.churn(screenState(chapters = mixed().map { it.copy(selected = false) }, overflow = true))
        host.churn(screenState(chapters = mixed(), mergeWithAnother = true))
        host.tablet = true
        host.churn(screenState(chapters = mixed().map { it.copy(selected = false) }))
        host.churn(screenState(manga = screenManga(genre = null, description = null)))
        compose.onNodeWithText("No description").assertExists()
    }
}
