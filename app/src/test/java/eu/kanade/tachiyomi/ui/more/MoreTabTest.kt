package eu.kanade.tachiyomi.ui.more

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.TabHost
import eu.kanade.tachiyomi.ui.base.disposeScreenModels
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.browse.ScreenHost
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.hasLabel
import eu.kanade.tachiyomi.ui.library.waitForLabel
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import exh.ui.batchadd.BatchAddScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class MoreTabTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = BrowseKoin()
    private val running = MutableStateFlow(false)
    private val queue = MutableStateFlow<List<Download>>(emptyList())
    private val downloads: DownloadManager = mockk {
        every { isDownloaderRunning } returns running
        every { queueState } returns queue
    }
    private val host = ScreenHost(MoreTab)

    @Before
    fun setUp() = koin.start(module { single { downloads } })

    @After
    fun tearDown() {
        disposeScreenModels(MoreTab)
        koin.stop()
    }

    private fun show() {
        host.show(compose)
        compose.waitForLabel("Download queue")
    }

    private fun opens(label: String): Any {
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.waitForIdle()
        return host.top.also { host.navigator.pop() }
    }

    @Test
    fun linksOpenTheirScreens() {
        koin.uiPreferences.showNavUpdates.set(false)
        koin.uiPreferences.showNavHistory.set(false)
        show()
        opens("Download queue").shouldBeInstanceOf<DownloadQueueScreen>()
        opens("Categories").shouldBeInstanceOf<CategoryScreen>()
        opens("Statistics").shouldBeInstanceOf<StatsScreen>()
        opens("Data and storage").shouldBeInstanceOf<SettingsScreen>()
        opens("Batch Add").shouldBeInstanceOf<BatchAddScreen>()
        opens("Updates") shouldBe UpdatesTab
        opens("History") shouldBe HistoryTab
        opens("Settings").shouldBeInstanceOf<SettingsScreen>()
        opens("About").shouldBeInstanceOf<SettingsScreen>()
    }

    @Test
    fun switchesWritePreferences() {
        show()
        compose.onNodeWithText("Downloaded only").performClick()
        compose.onNodeWithText("Incognito mode").performClick()
        compose.waitUntil { koin.basePreferences.incognitoMode.get() }
        koin.basePreferences.downloadedOnly.get() shouldBe true
    }

    @Test
    fun queueStateFollowsDownloader() {
        show()
        queue.value = listOf(mockk(), mockk())
        compose.waitForLabel("Paused • 2 remaining")
        running.value = true
        compose.waitForLabel("2 remaining")
        queue.value = emptyList()
        compose.waitUntil { !compose.hasLabel("2 remaining") }
    }

    @Test
    fun optionsAndReselect() {
        compose.setContent { TabHost(MoreTab) }
        compose.waitForLabel("tab:More")
        val navigator = mockk<Navigator>(relaxed = true)
        runBlocking { MoreTab.onReselect(navigator) }
        coVerify { navigator.push(any<SettingsScreen>()) }
    }
}
