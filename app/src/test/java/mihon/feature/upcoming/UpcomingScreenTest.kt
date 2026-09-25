package mihon.feature.upcoming

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import mihon.domain.upcoming.interactor.GetUpcomingManga
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
internal class UpcomingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        val manga = Manga.create().copy(
            id = 3L,
            ogTitle = "Soon",
            nextUpdate = LocalDate.now().plusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        val getUpcomingManga = mockk<GetUpcomingManga>()
        coEvery { getUpcomingManga.subscribe() } returns flowOf(listOf(manga))
        stopKoin()
        startKoin { modules(module { single { getUpcomingManga } }) }
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun anEntryOpensItsManga() {
        compose.setContent {
            MaterialTheme {
                // Only the screen under test is composed; where it navigates to is shown by name.
                Navigator(UpcomingScreen()) { navigator ->
                    if (navigator.lastItem is UpcomingScreen) {
                        CurrentScreen()
                    } else {
                        Text("at ${navigator.lastItem::class.simpleName}")
                    }
                }
            }
        }
        compose.waitUntil(10_000L) {
            compose.onAllNodes(hasText("Soon")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Soon").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("at MangaScreen").assertExists()
    }
}
