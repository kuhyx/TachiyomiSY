package eu.kanade.tachiyomi.ui.browse.extension.details

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.domain.extension.interactor.ExtensionSourceItem
import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.extension.interactor.installed
import eu.kanade.domain.source.interactor.ToggleIncognito
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.ui.base.clickLabel
import eu.kanade.tachiyomi.ui.base.poll
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.browse.ScreenHost
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
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
internal class ExtensionDetailsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = BrowseKoin()
    private val installedFlow = MutableStateFlow<List<Extension.Installed>>(emptyList())
    private val manager = mockk<ExtensionManager>(relaxed = true) {
        every { installedExtensionsFlow } returns installedFlow
    }
    private val getSources = mockk<GetExtensionSources>()
    private val toggleSource = mockk<ToggleSource>(relaxed = true)
    private val source = mockk<CatalogueSource>(relaxed = true, moreInterfaces = arrayOf(ConfigurableSource::class)) {
        every { id } returns 2L
        every { name } returns "Site"
        every { lang } returns "en"
    }
    private val ext = installed("Ext").copy(sources = listOf(source))
    private val host = ScreenHost(ExtensionDetailsScreen(ext.pkgName))

    @Before
    fun setUp() {
        every { getSources.subscribe(any()) } returns MutableStateFlow(
            listOf(ExtensionSourceItem(source, enabled = true, labelAsName = false)),
        )
        installedFlow.value = listOf(ext)
        koin.start(
            module {
                single { mockk<NetworkHelper>(relaxed = true) }
                single { manager }
                single { getSources }
                single { toggleSource }
                single { mockk<ToggleIncognito>(relaxed = true) }
            },
        )
        host.show(compose)
        compose.pollLabel("English")
    }

    @After
    fun tearDown() = koin.stop()

    @Test
    fun sourceSettingsOpen() {
        compose.clickLabel("Settings")
        host.top.shouldBeInstanceOf<SourcePreferencesScreen>()
    }

    @Test
    fun overflowTogglesAll() {
        compose.clickLabel("More options")
        compose.clickLabel("Enable all")
        verify(timeout = 5_000) { toggleSource.await(listOf(2L), true) }
        compose.clickLabel("More options")
        compose.clickLabel("Disable all")
        verify(timeout = 5_000) { toggleSource.await(listOf(2L), false) }
    }

    @Test
    fun uninstalledExtensionLeaves() {
        installedFlow.value = emptyList()
        compose.poll({ "still open" }) { host.top is BlankScreen }
    }
}
