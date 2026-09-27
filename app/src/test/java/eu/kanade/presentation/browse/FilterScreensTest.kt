package eu.kanade.presentation.browse

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.getAppIconForSource
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionFilterState
import eu.kanade.tachiyomi.ui.browse.source.SourcesFilterScreenModel
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.source.model.Source
import java.util.TreeMap

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h2000dp")
internal class FilterScreensTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val extensionManager = mockk<ExtensionManager>()

    private var sourcesState by mutableStateOf(
        SourcesFilterScreenModel.State.Success(TreeMap(), enabledLanguages = emptySet(), disabledSources = emptySet()),
    )

    @Before
    fun setUp() {
        mockkStatic("eu.kanade.tachiyomi.extension.ExtensionManagerRegistryKt")
        every { extensionManager.getAppIconForSource(any()) } returns null
        koin.start(module { single { extensionManager } })
    }

    @After
    fun tearDown() {
        koin.stop()
        unmockkAll()
    }

    private fun source(id: Long, lang: String) =
        Source(id = id, lang = lang, name = "Source $id", supportsLatest = false, isStub = false)

    private fun showSources() {
        compose.setContent {
            MaterialTheme {
                SourcesFilterScreen(
                    navigateUp = { events += "up" },
                    state = sourcesState,
                    onClickLanguage = { events += "lang $it" },
                    onClickSource = { events += "source ${it.id}" },
                    onClickSources = { enable, sources -> events += "all $enable ${sources.map { it.id }}" },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun sourcesEmptyThenListed() {
        showSources()
        compose.onNodeWithText("No installed source found", substring = true).assertExists()
        val items = TreeMap(mapOf("en" to listOf(source(1L, "en"), source(2L, "en")), "ja" to listOf(source(3L, "ja"))))
        sourcesState = SourcesFilterScreenModel.State.Success(
            items = items,
            enabledLanguages = setOf("en"),
            disabledSources = setOf("2"),
        )
        compose.waitForIdle()
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("All Sources").performClick()
        compose.onNodeWithText("Source 1").performClick()
        compose.onNodeWithText("日本語").performClick()
        compose.onNodeWithText("Source 3").assertDoesNotExist()
        sourcesState = sourcesState.copy(disabledSources = emptySet())
        compose.waitForIdle()
        compose.onNodeWithText("All Sources").performClick()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly
            listOf("lang en", "all true [1, 2]", "source 1", "lang ja", "all false [1, 2]", "up")
    }

    @Test
    fun extensionLanguages() {
        var state by mutableStateOf(ExtensionFilterState.Success(languages = emptyList()))
        compose.setContent {
            MaterialTheme {
                ExtensionFilterScreen(
                    navigateUp = { events += "up" },
                    state = state,
                    onClickToggle = { events += "toggle $it" },
                )
            }
        }
        compose.onNodeWithText("Well, this is awkward", substring = true).assertExists()
        state = ExtensionFilterState.Success(languages = listOf("en", "ja"), enabledLanguages = setOf("ja"))
        compose.waitForIdle()
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("日本語").performClick()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly listOf("toggle en", "toggle ja", "up")
    }
}
