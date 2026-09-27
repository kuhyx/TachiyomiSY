package eu.kanade.presentation.browse

import android.graphics.drawable.ShapeDrawable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.getAppIconForSource
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrateSourceScreenModel
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
import tachiyomi.domain.source.model.Source

@RunWith(RobolectricTestRunner::class)
internal class MigrateSourceScreenTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val extensionManager = mockk<ExtensionManager>()
    private var state by mutableStateOf(MigrateSourceScreenModel.State())

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

    private fun source(id: Long, name: String = "Source $id", lang: String = "en", stub: Boolean = false) =
        Source(id = id, lang = lang, name = name, supportsLatest = false, isStub = stub)

    private fun show() {
        compose.setContent {
            MaterialTheme {
                MigrateSourceScreen(
                    state = state,
                    contentPadding = PaddingValues(),
                    onClickItem = { events += "item ${it.id}" },
                    onToggleSortingDirection = { events += "direction" },
                    onToggleSortingMode = { events += "mode" },
                    onClickAll = { events += "all ${it.id}" },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun loadingThenEmpty() {
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeBy(500L)
        state = MigrateSourceScreenModel.State(isLoading = false)
        compose.mainClock.advanceTimeBy(500L)
        compose.onNodeWithText("Your library is empty", substring = true).assertExists()
    }

    @Test
    fun itemsForwardActions() {
        state = MigrateSourceScreenModel.State(
            isLoading = false,
            items = listOf(
                source(1L) to 3L,
                source(2L, name = " ", lang = "", stub = true) to 1L,
                source(3L, stub = true) to 2L,
                source(0L, name = "Local") to 4L,
                source(4L, stub = true) to 5L,
            ),
            sortingMode = SetMigrateSorting.Mode.TOTAL,
            sortingDirection = SetMigrateSorting.Direction.DESCENDING,
        )
        show()
        compose.onNodeWithText("Source 1").performClick()
        compose.onNodeWithText("Source 1").performTouchInput { longClick() }
        compose.onAllNodesWithText("Not installed")[0].assertExists()
        compose.onAllNodesWithText("All")[0].performClick()
        compose.onNodeWithContentDescription("Total entries").performClick()
        compose.onNodeWithContentDescription("Descending").performClick()
        events shouldContainExactly listOf("item 1", "all 1", "mode", "direction")
    }

    @Test
    fun alphabeticalAscending() {
        state = MigrateSourceScreenModel.State(isLoading = false, items = listOf(source(1L) to 1L))
        show()
        compose.onNodeWithContentDescription("Alphabetically").performClick()
        compose.onNodeWithContentDescription("Ascending").performClick()
        events shouldContainExactly listOf("mode", "direction")
    }
}
