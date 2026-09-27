package eu.kanade.tachiyomi.ui.browse.extension.details

import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.source.interactor.ToggleIncognito
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

/** Every action is a no-op while the model has no extension, and the state reads as loading. */
@RunWith(RobolectricTestRunner::class)
internal class ExtensionDetailsNoExtensionTest {
    private val koin = BrowseKoin()
    private val network = mockk<NetworkHelper>(relaxed = true)
    private val manager = mockk<ExtensionManager>(relaxed = true) {
        every { installedExtensionsFlow } returns MutableStateFlow(emptyList())
    }
    private val toggleSource = mockk<ToggleSource>(relaxed = true)
    private val toggleIncognito = mockk<ToggleIncognito>(relaxed = true)

    @Before
    fun setUp() = koin.start(
        module {
            single { network }
            single { manager }
            single { mockk<GetExtensionSources>() }
            single { toggleSource }
            single { toggleIncognito }
        },
    )

    @After
    fun tearDown() = koin.stop()

    @Test
    fun actionsNeedAnExtension() {
        val model = ExtensionDetailsScreenModel("missing", koin.app)
        model.clearCookies()
        model.uninstallExtension()
        model.toggleSources(true)
        model.toggleIncognito(true)
        verify(exactly = 0) { network.cookieJar }
        verify(exactly = 0) { toggleSource.await(any<List<Long>>(), any()) }
        verify(exactly = 0) { toggleIncognito.await(any(), any()) }
    }

    @Test
    fun emptyStateIsLoading() {
        val state = ExtensionDetailsScreenModel.State()
        state.sources shouldBe emptyList()
        state.isLoading shouldBe true
    }
}
