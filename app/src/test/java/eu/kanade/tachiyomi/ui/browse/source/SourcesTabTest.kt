package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.domain.source.interactor.GetShowLatest
import eu.kanade.domain.source.interactor.GetSourceCategories
import eu.kanade.domain.source.interactor.SetSourceCategories
import eu.kanade.domain.source.interactor.ToggleExcludeFromDataSaver
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.getAppIconForSource
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.browse.TabHost
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.feed.SourceFeedScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import exh.ui.smartsearch.SmartSearchScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR

private const val REGISTRY = "eu.kanade.tachiyomi.extension.ExtensionManagerRegistryKt"

@RunWith(RobolectricTestRunner::class)
internal class SourcesTabTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = BrowseKoin()
    private val sources = MutableStateFlow(listOf(source(1L, name = "Alpha"), source(2L, name = "Beta")))
    private val categories = MutableStateFlow(emptyList<String>())
    private val toggleSource = mockk<ToggleSource>(relaxed = true)
    private val togglePin = mockk<ToggleSourcePin>(relaxed = true)
    private val toggleDataSaver = mockk<ToggleExcludeFromDataSaver>(relaxed = true)
    private val setCategories = mockk<SetSourceCategories>(relaxed = true)
    private val extensions = mockk<ExtensionManager>()
    private val getEnabled = mockk<GetEnabledSources> { every { subscribe() } returns sources }

    @Before
    fun setUp() {
        mockkStatic(REGISTRY)
        every { extensions.getAppIconForSource(any()) } returns null
        koin.start(
            module {
                single { getEnabled }
                single<GetSourceCategories> { mockk { every { subscribe() } returns categories } }
                single<GetShowLatest> { mockk { every { subscribe(any()) } returns MutableStateFlow(true) } }
                single { toggleSource }
                single { togglePin }
                single { toggleDataSaver }
                single { setCategories }
                single { extensions }
            },
        )
    }

    @After
    fun tearDown() {
        koin.stop()
        unmockkStatic(REGISTRY)
    }

    private fun host(config: SourcesScreen.SmartSearchConfig? = null) =
        TabHost { sourcesTab(config) }.apply { show(compose) }

    @Test
    fun clicksOpenTheSource() {
        koin.uiPreferences.useNewSourceNavigation.set(false)
        val host = host()
        compose.onNodeWithText("Alpha").performClick()
        compose.waitForIdle()
        host.navigator.lastItem.shouldBeInstanceOf<BrowseSourceScreen>()
        compose.onNodeWithText("Latest", useUnmergedTree = true).assertExists()
    }

    @Test
    fun newNavigationOpensTheFeed() {
        koin.uiPreferences.useNewSourceNavigation.set(true)
        val host = host()
        compose.onNodeWithText("Alpha").performClick()
        compose.waitForIdle()
        host.navigator.lastItem.shouldBeInstanceOf<SourceFeedScreen>()
    }

    @Test
    fun smartSearchOpensSmartSearch() {
        val host = host(SourcesScreen.SmartSearchConfig("Needle", 3L))
        host.content.actions.size shouldBe 1
        compose.onNodeWithText("Beta").performClick()
        compose.waitForIdle()
        host.navigator.lastItem.shouldBeInstanceOf<SmartSearchScreen>()
        host.action("Global search")
        compose.waitForIdle()
        host.navigator.lastItem.shouldBeInstanceOf<GlobalSearchScreen>()
    }

    @Test
    fun actionsNavigate() {
        val host = host()
        host.action("Global search")
        compose.waitForIdle()
        host.navigator.lastItem.shouldBeInstanceOf<GlobalSearchScreen>()
        host.action("Filter")
        compose.waitForIdle()
        host.navigator.lastItem.shouldBeInstanceOf<SourcesFilterScreen>()
    }

    @Test
    fun pinButtonTogglesThePin() {
        host()
        compose.onAllNodesWithContentDescription("Pin")[0].performClick()
        verify { togglePin.await(source(1L, name = "Alpha")) }
    }

    @Test
    fun longClickOffersOptions() {
        host()
        compose.onNodeWithText("Alpha").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Pin").performClick()
        verify { togglePin.await(any<Source>()) }
        compose.onNodeWithText("Alpha").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Disable").performClick()
        verify { toggleSource.await(any<Source>(), any()) }
    }

    @Test
    fun categoriesAndDataSaverOptions() {
        categories.value = listOf("Fav")
        koin.sourcePreferences.dataSaver.set(SourcePreferences.DataSaver.BANDWIDTH_HERO)
        host()
        compose.onNodeWithText("Alpha").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Exclude from data saver").performClick()
        verify { toggleDataSaver.await(any()) }
        compose.onNodeWithText("Alpha").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Categories").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Fav").performClick()
        compose.onNodeWithText("OK").performClick()
        verify { setCategories.await(any(), listOf("Fav")) }
    }

    @Test
    fun failuresShowASnackbar() {
        every { getEnabled.subscribe() } returns flow { error("db") }
        val host = host()
        compose.waitUntil(timeoutMillis = 10_000) { host.snackbar.currentSnackbarData != null }
        host.snackbar.currentSnackbarData?.visuals?.message shouldBe
            "InternalError: Check crash logs for further information"
        host.content.titleRes shouldBe MR.strings.label_sources
    }
}
