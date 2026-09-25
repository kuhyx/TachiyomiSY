package eu.kanade.presentation.browse

import androidx.compose.ui.test.onAllNodesWithText
import android.graphics.drawable.ShapeDrawable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.getAppIconForSource
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreenModel
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.model.Pins
import tachiyomi.domain.source.model.Source

@RunWith(RobolectricTestRunner::class)
internal class SourcesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val extensionManager = mockk<ExtensionManager>()

    @Before
    fun setUp() {
        mockkStatic("eu.kanade.tachiyomi.extension.ExtensionManagerRegistryKt")
        every { extensionManager.getAppIconForSource(any()) } answers {
            ShapeDrawable().apply {
                intrinsicWidth = 4
                intrinsicHeight = 4
            }.takeIf { secondArg<Long>() == 3L }
        }
        koin.start(module { single { extensionManager } })
    }

    @After
    fun tearDown() {
        koin.stop()
        unmockkAll()
    }

    private fun source(id: Long, latest: Boolean = true, stub: Boolean = false, pins: Pins = Pins.unpinned) =
        Source(id = id, lang = "en", name = "Source $id", supportsLatest = latest, isStub = stub, pin = pins)

    private fun show(state: SourcesScreenModel.State) {
        compose.setContent {
            MaterialTheme {
                SourcesScreen(
                    state = state,
                    contentPadding = PaddingValues(),
                    onClickItem = { source, listing -> events += "open ${source.id} ${listing.javaClass.simpleName}" },
                    onClickPin = { events += "pin ${it.id}" },
                    onLongClickItem = { events += "long ${it.id}" },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun loadingAndEmpty() {
        show(SourcesScreenModel.State(isLoading = false))
        compose.onNodeWithText("No source found").assertExists()
    }

    @Test
    fun stillLoading() {
        show(SourcesScreenModel.State())
        compose.onNodeWithText("No source found").assertDoesNotExist()
    }

    @Test
    fun itemsForwardEveryAction() {
        val items = listOf(
            SourceUiModel.Header("en", isCategory = false),
            SourceUiModel.Header("Favourites", isCategory = true),
            SourceUiModel.Item(source(1L, pins = Pins.pinned)),
            SourceUiModel.Item(source(2L, latest = false, stub = true)),
            SourceUiModel.Item(source(3L)),
            SourceUiModel.Item(source(0L)),
        )
        show(SourcesScreenModel.State(isLoading = false, items = items, showLatest = true))
        compose.onNodeWithText("Favourites").assertExists()
        compose.onNodeWithText("Source 1").performClick()
        compose.onNodeWithText("Source 1").performTouchInput { longClick() }
        compose.onAllNodesWithText("Latest")[0].performClick()
        compose.onNodeWithContentDescription("Unpin").performClick()
        events shouldContainExactly listOf("open 1 Popular", "long 1", "open 1 Latest", "pin 1")
    }

    @Test
    fun hiddenLatestAndPins() {
        val items = listOf(SourceUiModel.Item(source(1L)))
        show(SourcesScreenModel.State(isLoading = false, items = items, showPin = false))
        compose.onNodeWithText("Latest").assertDoesNotExist()
        compose.onNodeWithContentDescription("Pin").assertDoesNotExist()
    }
}
