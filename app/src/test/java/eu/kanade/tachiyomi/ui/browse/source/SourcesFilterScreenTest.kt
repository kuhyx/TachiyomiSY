package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.domain.source.interactor.GetLanguagesWithSources
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.browse.ScreenHost
import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.source.model.Source

@RunWith(RobolectricTestRunner::class)
internal class SourcesFilterScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = BrowseKoin()
    private val getLanguages = mockk<GetLanguagesWithSources>()
    private val toggleSource = mockk<ToggleSource>(relaxed = true)
    private val toggleLanguage = mockk<ToggleLanguage>(relaxed = true)
    private val host = ScreenHost(SourcesFilterScreen())
    private val extensions = mockk<ExtensionManager>(relaxed = true) {
        every { installedExtensionsFlow } returns MutableStateFlow(emptyList())
    }
    private val source = Source(id = 1L, lang = "en", name = "Site", supportsLatest = false, isStub = false)

    @Before
    fun setUp() = koin.start(
        module {
            single { getLanguages }
            single { toggleSource }
            single { toggleLanguage }
            single { extensions }
        },
    )

    @After
    fun tearDown() = koin.stop()

    @Test
    fun languagesAndSourcesToggle() {
        every { getLanguages.subscribe() } returns MutableStateFlow(sortedMapOf("en" to listOf(source)))
        koin.sourcePreferences.enabledLanguages.set(setOf("en"))
        host.show(compose)
        compose.pollLabel("Site")
        compose.onNodeWithText("Site").performClick()
        verify(timeout = 5_000) { toggleSource.await(source) }
        compose.onNodeWithText("English").performClick()
        verify(timeout = 5_000) { toggleLanguage.await("en") }
    }

    @Test
    fun loadingShowsSpinner() {
        every { getLanguages.subscribe() } returns MutableSharedFlow()
        host.show(compose)
        compose.onNodeWithText("Site").assertDoesNotExist()
    }

    @Test
    fun failureToastsAndLeaves() {
        every { getLanguages.subscribe() } returns flow { error("db") }
        host.show(compose)
        eventually { host.top is BlankScreen }
        host.top.shouldBeInstanceOf<BlankScreen>()
        ShadowToast.getTextOfLatestToast() shouldBe "InternalError: Check crash logs for further information"
    }
}
