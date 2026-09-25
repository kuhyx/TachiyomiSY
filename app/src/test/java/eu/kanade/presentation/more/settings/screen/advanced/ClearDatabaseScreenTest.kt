package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.extension.ExtensionManager
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import tachiyomi.data.Database
import tachiyomi.domain.source.interactor.GetSourcesWithNonLibraryManga
import tachiyomi.domain.source.model.SourceWithCount

@RunWith(RobolectricTestRunner::class)
internal class ClearDatabaseScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val sources = MutableStateFlow<List<SourceWithCount>>(emptyList())
    private val database = mockk<Database>(relaxed = true)

    @Before
    fun setUp() {
        val getSources = mockk<GetSourcesWithNonLibraryManga> { every { subscribe() } returns sources }
        val extensions = mockk<ExtensionManager> { every { getAppIconForSource(any()) } returns null }
        stopKoin()
        startKoin {
            modules(
                module {
                    single { getSources }
                    single { database }
                    single { extensions }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun show() {
        compose.setContent { MaterialTheme { Navigator(ClearDatabaseScreen()) } }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Clear database").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    @Test
    fun emptyDatabaseMessage() {
        show()
        compose.onNodeWithText("Nothing to clear").assertExists()
    }

    @Test
    fun selectAndDelete() {
        sources.value = listOf(sourceWithCount(id = 1, name = "Alpha"), sourceWithCount(id = 2, name = "Beta"))
        show()
        tap("Alpha (EN)")
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        compose.onNodeWithContentDescription("Select all").performClick()
        compose.onNodeWithContentDescription("Select inverse").performClick()
        compose.onNodeWithContentDescription("Select all").performClick()
        tap("Delete")
        compose.onNode(isToggleable() and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("will be lost", substring = true).assertExists()
        tap("OK")
        compose.waitUntil(timeoutMillis = 10_000) { ShadowToast.shownToastCount() == 1 }
        verify { database.mangasQueries.deleteNonLibraryManga(listOf(1L, 2L), 0L) }
    }

    @Test
    fun cancelConfirmation() {
        sources.value = listOf(sourceWithCount(id = 1, name = "Alpha"))
        show()
        tap("Alpha (EN)")
        tap("Delete")
        tap("Cancel")
        compose.onAllNodesWithText("Are you sure?").fetchSemanticsNodes().size shouldBe 0
    }
}
