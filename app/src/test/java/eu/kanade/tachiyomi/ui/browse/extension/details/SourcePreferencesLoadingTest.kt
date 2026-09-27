package eu.kanade.tachiyomi.ui.browse.extension.details

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
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
import tachiyomi.domain.source.service.SourceManager

/** Until the sources are loaded the preference screen only shows a spinner. */
@RunWith(RobolectricTestRunner::class)
internal class SourcePreferencesLoadingTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val sources = mockk<SourceManager> { every { isInitialized } returns MutableStateFlow(false) }

    @Before
    fun setUp() {
        stopKoin()
        startKoin { modules(module { single { sources } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun waitsForSources() {
        // The spinner animates forever: a manual clock lets the test settle without running it.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                val screen = SourcePreferencesScreen(1L)
                Navigator(listOf(BlankScreen(), screen)) { screen.Content() }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        val spinners = compose.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        spinners.fetchSemanticsNodes().isNotEmpty() shouldBe true
    }
}
