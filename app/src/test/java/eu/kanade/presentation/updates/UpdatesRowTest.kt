package eu.kanade.presentation.updates

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UpdatesRowTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val harness = UpdatesScreenHarness(compose)

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun showOne(item: UpdatesItem, preserve: Boolean = false) {
        harness.show(UpdatesScreenModel.State(isLoading = false, items = listOf(item)), preserve)
    }

    @Test
    fun unreadProgressShowsThePage() {
        showOne(updatesItem(lastPageRead = 5L, bookmark = true))
        compose.onNodeWithText("Page: 6").assertExists()
        compose.onNodeWithContentDescription("Unread").assertExists()
        compose.onNodeWithContentDescription("Bookmarked").assertExists()
    }

    @Test
    fun readChaptersHideProgress() {
        showOne(updatesItem(read = true, lastPageRead = 5L), preserve = true)
        compose.onNodeWithText("Page: 6").assertDoesNotExist()
        compose.onNodeWithContentDescription("Unread").assertDoesNotExist()
    }

    @Test
    fun ehKeepsProgressWhenPreserving() {
        showOne(updatesItem(read = true, lastPageRead = 5L, sourceId = EH_SOURCE_ID), preserve = true)
        compose.onNodeWithText("Page: 6").assertExists()
    }

    @Test
    fun exhKeepsProgressWhenPreserving() {
        showOne(updatesItem(read = true, lastPageRead = 5L, sourceId = EXH_SOURCE_ID), preserve = true)
        compose.onNodeWithText("Page: 6").assertExists()
    }

    @Test
    fun ehWithoutPreservingHidesIt() {
        showOne(updatesItem(read = true, lastPageRead = 5L, sourceId = EH_SOURCE_ID))
        compose.onNodeWithText("Page: 6").assertDoesNotExist()
    }

    @Test
    fun unstartedHasNoProgress() {
        showOne(updatesItem())
        compose.onNodeWithText("Page: 1").assertDoesNotExist()
    }

    @Test
    fun coverAndDownloadForward() {
        showOne(updatesItem())
        compose.onNodeWithContentDescription("", substring = false).performClick()
        compose.onNodeWithContentDescription("Download").performClick()
        compose.onNodeWithText("Chapter 1").performTouchInput { longClick() }
        harness.events shouldContainExactly listOf("cover 1", "download [1] START", "select 1 true true")
    }

    @Test
    fun selectionDisablesCoverAndDownload() {
        showOne(updatesItem(selected = true, state = eu.kanade.tachiyomi.data.download.model.Download.State.QUEUE))
        compose.onNodeWithText("Chapter 1").performClick()
        harness.events shouldContainExactly listOf("select 1 false false")
    }
}
