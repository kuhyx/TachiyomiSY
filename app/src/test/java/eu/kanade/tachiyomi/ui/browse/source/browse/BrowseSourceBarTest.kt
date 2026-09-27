package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.poll
import eu.kanade.tachiyomi.ui.library.hasLabel
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.source.local.LocalSource

/** The toolbar: web view, help, source settings, display mode, and leaving the search or the screen. */
@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceBarTest {
    @get:Rule
    val compose = createComposeRule()

    private val rig = BrowseScreenRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun serve(source: Source) {
        every { source.id } returns 1L
        every { source.name } returns "Other"
        (source as? CatalogueSource)?.let { every { it.getFilterList() } returns FilterList() }
        every { rig.harness.sourceManager.getOrStub(1L) } returns source
    }

    @Test
    fun webViewOpensHome() {
        val web = mockk<HttpSource>(relaxed = true) { every { getHomeUrl() } returns "https://home" }
        serve(web)
        val screen = BrowseSourceScreen(1L, "")
        rig.show(screen)
        rig.await("WebView")
        compose.poll({ "no assist url" }) { screen.onProvideAssistUrl() == "https://home" }
        rig.click("WebView")
        rig.await("opened:WebViewScreen")
    }

    @Test
    fun plainSourceHasNoWebView() {
        rig.show()
        rig.await("WebView")
        rig.click("WebView")
        compose.hasLabel("opened:WebViewScreen") shouldBe false
    }

    @Test
    fun localSourceOffersHelp() {
        serve(mockk<LocalSource>(relaxed = true))
        rig.show()
        rig.await("Help")
        rig.click("Help")
    }

    @Test
    fun configurableSourceSettings() {
        serve(mockk<CatalogueSource>(relaxed = true, moreInterfaces = arrayOf(ConfigurableSource::class)))
        rig.show()
        rig.await("More options")
        rig.click("More options")
        rig.click("Settings")
        rig.await("opened:SourcePreferencesScreen")
    }

    @Test
    fun displayModeIsPicked() {
        rig.show()
        rig.click("Display mode")
        rig.click("List")
        compose.poll({ "mode kept" }) {
            rig.harness.koin.sourcePreferences.sourceDisplayMode.get() == LibraryDisplayMode.List
        }
    }

    @Test
    fun upClosesSearchFirst() {
        rig.show()
        rig.click("Search")
        rig.await("Navigate up")
        rig.click("Navigate up")
        rig.click("Navigate up")
    }
}
