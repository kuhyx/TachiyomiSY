package mihon.feature.upcoming

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.ui.UiPreferences
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.manga.model.Manga
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
internal class UpcomingScreenContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val month = YearMonth.of(2030, 3)
    private val day = month.atDay(10)
    private val opened = mutableListOf<Long>()
    private val months = mutableListOf<YearMonth>()

    private fun state() = UpcomingScreenModel.State(
        selectedYearMonth = month,
        items = listOf(
            UpcomingUIModel.Header(day, 1),
            UpcomingUIModel.Item(Manga.create().copy(id = 7L, ogTitle = "Soon")),
        ),
        events = mapOf(day to 5),
        headerIndexes = mapOf(day to 0),
    )

    @Before
    fun setUp() {
        stopKoin()
        startKoin { modules(module { single { UiPreferences(InMemoryPreferenceStore()) } }) }
    }

    @After
    fun tearDown() = stopKoin()

    private class Host(private val content: @Composable () -> Unit) : Screen {
        @Composable
        override fun Content() = content()
    }

    private fun show() {
        compose.setContent {
            MaterialTheme {
                Navigator(
                    Host {
                        UpcomingScreenContent(
                            state = state(),
                            setSelectedYearMonth = { months += it },
                            onClickUpcoming = { opened += it.id },
                        )
                    },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun exercise() {
        compose.onNodeWithText("Upcoming").assertExists()
        compose.onNodeWithText("Soon").performClick()
        compose.onNodeWithText("10").performClick()
        compose.onNodeWithText("11").performClick()
        compose.onNodeWithContentDescription("Previous Month").performClick()
        compose.onNodeWithContentDescription("Next Month").performClick()
        compose.onNodeWithContentDescription("Upcoming Guide").performClick()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.waitForIdle()
        opened shouldContainExactly listOf(7L)
        months shouldContainExactly listOf(month.minusMonths(1L), month.plusMonths(1L))
        shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity?.data?.host shouldBe "mihon.app"
    }

    // On a phone the calendar is the list's first item; the default 470dp-high window leaves the
    // entry below the fold, where the lazy list never composes it.
    @Test
    @Config(qualifiers = "h2000dp")
    fun onAPhone() {
        show()
        exercise()
    }

    @Test
    fun onATablet() {
        RuntimeEnvironment.setQualifiers("sw900dp-w1000dp-h1200dp")
        show()
        exercise()
    }

    @Test
    fun onAMediumWindow() {
        RuntimeEnvironment.setQualifiers("sw700dp-w700dp-h1000dp")
        show()
        compose.onNodeWithText("Soon").assertExists()
    }

    @Test
    fun anEmptyDayScrollsNowhere() {
        show()
        // The day heading's count badge also reads "1"; only the calendar day is clickable.
        compose.onNode(hasText(LocalDate.of(2030, 3, 1).dayOfMonth.toString()) and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Previous Month").assertExists()
        opened shouldBe emptyList()
    }
}
