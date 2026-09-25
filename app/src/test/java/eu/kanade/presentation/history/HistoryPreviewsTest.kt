package eu.kanade.presentation.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HistoryPreviewsTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun providerOffersSixStates() {
        val states = HistoryScreenModelStateProvider().values.toList()
        states.size shouldBe 6
        states.first().list?.size shouldBe 14
    }

    @Test
    fun everyPreviewStateRenders() {
        val states = HistoryScreenModelStateProvider().values.toList()
        var state: HistoryScreenModel.State by mutableStateOf(states.first())
        compose.setContent { HistoryScreenPreviews(state) }
        states.forEach {
            state = it
            compose.waitForIdle()
        }
        compose.onAllNodesWithText("History").fetchSemanticsNodes().size shouldBe 1
    }
}
