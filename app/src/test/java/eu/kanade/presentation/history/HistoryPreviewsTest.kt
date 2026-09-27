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

    // The preview examples always pass a builder; the default, identity one is reached reflectively.
    @Test
    fun defaultBuilderKeepsItem() {
        val provider = "eu.kanade.presentation.history.HistoryScreenModelStateProvider"
        val examples = Class.forName("$provider\$HistoryUiModelExamples")
        val instance = examples.getField("INSTANCE").get(null)
        val randItem = examples.getDeclaredMethod(
            "randItem\$default",
            examples,
            Function1::class.java,
            Int::class.java,
            Any::class.java,
        )
        randItem.isAccessible = true
        val item = randItem.invoke(null, instance, null, 1, null) as HistoryUiModel.Item
        item.item.ogTitle shouldBe "Test Title"
    }
}
