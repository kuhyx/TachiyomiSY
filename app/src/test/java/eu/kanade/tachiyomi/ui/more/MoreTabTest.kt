package eu.kanade.tachiyomi.ui.more

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.base.TabHost
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MoreTabTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val store = MapPreferenceStore()
    private val base = BasePreferences(ApplicationProvider.getApplicationContext<Application>(), store)
    private val ui = UiPreferences(store)
    private val running = MutableStateFlow(false)
    private val queue = MutableStateFlow<List<Download>>(emptyList())
    private val downloadManager = mockk<DownloadManager>(relaxed = true) {
        every { isDownloaderRunning } returns running
        every { queueState } returns queue
    }

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { base }
                    single { ui }
                    single { downloadManager }
                },
            )
        }
    }

    @After
    fun tearDown() = stopKoin()

    private fun show() {
        compose.setContent { TabHost(MoreTab) }
        waitFor("Downloaded only")
    }

    private fun waitFor(text: String) {
        compose.waitUntil(WAIT) {
            compose.onAllNodes(hasText(text, substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    /** Clicks the entry [label], scrolled into the lazy list first. */
    private fun click(label: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(label))
        compose.onAllNodes(hasText(label) and hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.OnClick)
    }

    /** Opens [label] and comes back, checking that [screen] was pushed. */
    private fun opens(label: String, screen: String) {
        click(label)
        waitFor("opened:$screen")
        compose.activity.onBackPressedDispatcher.onBackPressed()
        waitFor("Downloaded only")
    }

    @Test
    fun entriesOpenTheirScreens() {
        show()
        opens("Download queue", "DownloadQueueScreen")
        opens("Categories", "CategoryScreen")
        opens("Statistics", "StatsScreen")
        opens("Data and storage", "SettingsScreen")
        opens("Settings", "SettingsScreen")
        opens("About", "SettingsScreen")
        opens("Batch Add", "BatchAddScreen")
    }

    @Test
    fun hiddenTabsBecomeEntries() {
        ui.showNavUpdates.set(false)
        ui.showNavHistory.set(false)
        show()
        opens("Updates", "UpdatesTab")
        opens("History", "HistoryTab")
    }

    @Test
    fun modeSwitches() {
        show()
        click("Downloaded only")
        compose.waitUntil(WAIT) { base.downloadedOnly.get() }
        click("Incognito mode")
        compose.waitUntil(WAIT) { base.incognitoMode.get() }
        base.downloadedOnly.get() shouldBe true
    }

    @Test
    fun queueStateInTheSubtitle() {
        show()
        queue.value = listOf(mockk(), mockk())
        waitFor("Paused • 2 remaining")
        running.value = true
        waitFor("2 remaining")
        queue.value = emptyList()
        compose.waitUntil(WAIT) {
            compose.onAllNodes(hasText("remaining", substring = true)).fetchSemanticsNodes().isEmpty()
        }
    }

    private companion object {
        const val WAIT = 5_000L
    }
}
