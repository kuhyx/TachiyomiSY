package eu.kanade.tachiyomi.ui.history

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.domain.chapter.model.Chapter

@RunWith(RobolectricTestRunner::class)
internal class HistoryTabTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = HistoryTabRig(compose)
    private val chapter = Chapter.create().copy(id = 5L, mangaId = 1L)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = stopKoin()

    @Test
    fun resumingWithoutANextChapter() {
        rig.show()
        rig.click("Title 1")
        rig.waitFor("Next chapter not found")
    }

    @Test
    fun resumingOpensTheReader() {
        coEvery { rig.harness.getNextChapters.await(1L, 1L, onlyUnread = false) } returns listOf(chapter)
        rig.show()
        rig.click("Title 1")
        compose.waitUntil(TAB_WAIT) { shadowOf(compose.activity).peekNextStartedActivity() != null }
        shadowOf(compose.activity).nextStartedActivity.component?.className shouldBe ReaderActivity::class.java.name
    }

    @Test
    fun reselectingResumesTheLastRead() {
        coEvery { rig.harness.getNextChapters.await(onlyUnread = false) } returns listOf(chapter)
        rig.show()
        // Sent off the main thread: the tab's collector runs on it.
        CoroutineScope(Dispatchers.IO).launch { HistoryTab.onReselect(mockk<Navigator>()) }
        compose.waitUntil(TAB_WAIT) { shadowOf(compose.activity).peekNextStartedActivity() != null }
    }

    @Test
    fun failingHistoryShowsAnError() {
        every { rig.harness.getHistory.subscribe(any()) } returns flow { error("db") }
        compose.setContent { eu.kanade.tachiyomi.ui.base.TabHost(HistoryTab) }
        rig.waitFor("InternalError: Check crash logs for further information")
    }

    @Test
    fun clearingHistoryConfirms() {
        coEvery { rig.harness.removeHistory.awaitAll() } returns true
        rig.show()
        rig.click("Clear history")
        rig.click("OK")
        rig.waitFor("History deleted")
    }

    @Test
    fun optionsAndEnabled() {
        var enabled: Boolean? = null
        compose.setContent { enabled = HistoryTab.isEnabled() }
        compose.waitForIdle()
        enabled shouldBe true
    }

    @Test
    fun tabTitleIsShown() {
        rig.show()
        rig.node("tab:History").assertExists()
    }
}
