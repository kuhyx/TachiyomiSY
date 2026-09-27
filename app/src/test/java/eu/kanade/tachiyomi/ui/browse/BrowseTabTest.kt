package eu.kanade.tachiyomi.ui.browse

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.extension.interactor.GetExtensionsByType
import eu.kanade.domain.extension.model.Extensions
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.domain.source.interactor.GetShowLatest
import eu.kanade.domain.source.interactor.GetSourceCategories
import eu.kanade.domain.source.interactor.GetSourcesWithFavoriteCount
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.domain.source.interactor.SetSourceCategories
import eu.kanade.domain.source.interactor.ToggleExcludeFromDataSaver
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.base.TabHost
import eu.kanade.tachiyomi.ui.base.disposeScreenModels
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.base.readObjectMember
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import eu.kanade.tachiyomi.ui.browse.feed.FeedHarness
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.browse.source.source
import eu.kanade.tachiyomi.ui.main.MainActivity
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.interactor.GetFavorites

/** The browse tab: its pages in either feed position or without the feed, and the jump to extensions. */
@RunWith(RobolectricTestRunner::class)
internal class BrowseTabTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val harness = FeedHarness()
    private val extensions = mockk<ExtensionManager>(relaxed = true) {
        every { availableExtensionMapFlow } returns MutableStateFlow(emptyMap())
        every { untrustedExtensionMapFlow } returns MutableStateFlow(emptyMap())
        every { installedExtensionsFlow } returns MutableStateFlow(emptyList())
        every { availableExtensionsFlow } returns MutableStateFlow(emptyList())
    }

    @Before
    fun setUp() {
        resetUiDispatcher()
        every { harness.getManga.subscribe(any(), any()) } returns flowOf(null)
        coEvery { harness.getSavedSearches.await() } returns emptyList()
        harness.start()
        loadKoinModules(
            module {
                single<GetEnabledSources> {
                    mockk { every { subscribe() } returns MutableStateFlow(listOf(source(1L, name = "Alpha"))) }
                }
                single<GetSourceCategories> { mockk { every { subscribe() } returns MutableStateFlow(emptyList()) } }
                single<GetShowLatest> { mockk { every { subscribe(any()) } returns MutableStateFlow(true) } }
                single { mockk<ToggleSource>(relaxed = true) }
                single { mockk<ToggleSourcePin>(relaxed = true) }
                single { mockk<ToggleExcludeFromDataSaver>(relaxed = true) }
                single { mockk<SetSourceCategories>(relaxed = true) }
                single { extensions }
                single<GetExtensionsByType> {
                    val none = Extensions(emptyList(), emptyList(), emptyList(), emptyList())
                    mockk { every { subscribe() } returns MutableStateFlow(none) }
                }
                single<GetSourcesWithFavoriteCount> {
                    mockk { every { subscribe() } returns MutableStateFlow(emptyList()) }
                }
                single { mockk<SetMigrateSorting>(relaxed = true) }
                single<GetFavorites> { mockk { coEvery { await() } returns emptyList() } }
            },
        )
    }

    @After
    fun tearDown() {
        disposeScreenModels(BrowseTab)
        harness.stop()
    }

    private fun show() {
        compose.setContent { TabHost(BrowseTab) }
        compose.pollLabel("tab:Browse")
    }

    @Test
    fun sourcesComeFirst() {
        show()
        compose.pollLabel("Alpha")
        compose.pollLabel("Feed")
    }

    @Test
    fun feedCanComeFirst() {
        harness.koin.uiPreferences.feedTabInFront.set(true)
        show()
        compose.pollLabel("Feed")
    }

    @Test
    fun feedCanBeHidden() {
        harness.koin.uiPreferences.hideFeedTab.set(true)
        show()
        compose.pollLabel("Alpha")
    }

    @Test
    fun extensionRequestSwitchesPage() {
        show()
        BrowseTab.showExtension()
        compose.pollLabel("Extensions")
        val channel = readObjectMember(BrowseTab::class, "switchToExtensionTabChannel") as Channel<*>
        channel.tryReceive().isSuccess shouldBe false
    }

    @Test
    fun mainActivityIsReadied() {
        val main = Robolectric.buildActivity(MainActivity::class.java).get()
        compose.setContent {
            CompositionLocalProvider(LocalContext provides main) { TabHost(BrowseTab) }
        }
        compose.pollLabel("tab:Browse")
        compose.waitUntil(timeoutMillis = 10_000) { main.ready }
    }

    @Test
    fun reselectOpensGlobalSearch() {
        val navigator = mockk<Navigator>(relaxed = true)
        runBlocking { BrowseTab.onReselect(navigator) }
        verify { navigator.push(any<GlobalSearchScreen>()) }
    }
}
