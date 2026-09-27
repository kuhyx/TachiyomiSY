package eu.kanade.presentation.manga

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.ui.manga.notes.MangaNotesScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

private val Buttons = listOf(
    "Outlined.FormatBold",
    "Outlined.FormatItalic",
    "Outlined.FormatUnderlined",
    "AutoMirrored.Outlined.FormatListBulleted",
    "Outlined.FormatListNumbered",
)

@RunWith(RobolectricTestRunner::class)
internal class MangaNotesScreenTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val updates = mutableListOf<String>()
    private var navigatedUp = 0
    private var visible by mutableStateOf(true)

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun show(notes: String) {
        val state = MangaNotesScreen.State(manga = Manga.create().copy(ogTitle = "Needle"), notes = notes)
        compose.setContent {
            MaterialTheme {
                if (visible) {
                    MangaNotesScreen(state = state, navigateUp = { navigatedUp++ }, onUpdate = { updates += it })
                }
            }
        }
        compose.waitForIdle()
    }

    private fun settle() {
        compose.mainClock.advanceTimeBy(1_000L)
        compose.waitForIdle()
    }

    @Test
    fun formattingButtonsToggle() {
        show("")
        compose.onNodeWithText("Enjoyed the part where…").assertExists()
        Buttons.forEach { compose.onNodeWithContentDescription(it).performClick() }
        settle()
        Buttons.forEach { compose.onNodeWithContentDescription(it).performClick() }
        settle()
        compose.onNodeWithText("Needle").assertExists()
    }

    @Test
    fun typingUpdatesAfterDebounce() {
        show("**old**")
        compose.onNodeWithText("Needle").assertExists()
        settle()
        compose.onNodeWithText("old").performTextInput(" new")
        settle()
        updates.last() shouldContain "new"
        compose.onNodeWithContentDescription("Navigate up").performClick()
        navigatedUp shouldBe 1
    }

    @Test
    fun disposingSavesNotes() {
        show("~~gone~~")
        settle()
        val before = updates.size
        visible = false
        compose.waitForIdle()
        updates.size shouldBe before + 1
    }
}
