package eu.kanade.tachiyomi.ui.browse.extension.details

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.extension.interactor.installed
import eu.kanade.domain.source.interactor.ToggleIncognito
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.browse.ScreenHost
import eu.kanade.tachiyomi.ui.library.hasLabel
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

/** The details screen keeps its spinner until the extension's sources arrive. */
@RunWith(RobolectricTestRunner::class)
internal class ExtensionDetailsLoadingTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = BrowseKoin()
    private val ext = installed("Ext")
    private val getSources = mockk<GetExtensionSources> {
        every { subscribe(any()) } returns MutableSharedFlow()
    }

    @Before
    fun setUp() = koin.start(
        module {
            single { mockk<NetworkHelper>(relaxed = true) }
            single {
                mockk<ExtensionManager>(relaxed = true) {
                    every { installedExtensionsFlow } returns MutableStateFlow(listOf(ext))
                }
            }
            single { getSources }
            single { mockk<ToggleSource>(relaxed = true) }
            single { mockk<ToggleIncognito>(relaxed = true) }
        },
    )

    @After
    fun tearDown() = koin.stop()

    @Test
    fun spinnerUntilSourcesLoad() {
        ScreenHost(ExtensionDetailsScreen(ext.pkgName)).show(compose)
        verify(timeout = 5_000) { getSources.subscribe(any()) }
        compose.waitForIdle()
        compose.hasLabel("English") shouldBe false
        compose.hasLabel("Uninstall") shouldBe false
    }
}
