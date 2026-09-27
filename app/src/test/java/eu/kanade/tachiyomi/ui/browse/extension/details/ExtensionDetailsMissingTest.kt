package eu.kanade.tachiyomi.ui.browse.extension.details

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.source.interactor.ToggleIncognito
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.base.poll
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.browse.ScreenHost
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

/** An extension that is not installed when the screen opens: the screen leaves instead of spinning. */
@RunWith(RobolectricTestRunner::class)
internal class ExtensionDetailsMissingTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = BrowseKoin()

    @Before
    fun setUp() = koin.start(
        module {
            single { mockk<NetworkHelper>(relaxed = true) }
            single {
                mockk<ExtensionManager>(relaxed = true) {
                    every { installedExtensionsFlow } returns MutableStateFlow(emptyList())
                }
            }
            single { mockk<GetExtensionSources>() }
            single { mockk<ToggleSource>(relaxed = true) }
            single { mockk<ToggleIncognito>(relaxed = true) }
        },
    )

    @After
    fun tearDown() = koin.stop()

    @Test
    fun missingExtensionLeaves() {
        val host = ScreenHost(ExtensionDetailsScreen("eu.kanade.tachiyomi.extension.gone"))
        host.show(compose)
        compose.poll({ "still spinning" }) { host.top is BlankScreen }
    }
}
