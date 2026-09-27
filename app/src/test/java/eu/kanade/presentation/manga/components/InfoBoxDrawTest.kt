package eu.kanade.presentation.manga.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.presentation.util.PresentationKoin
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import tachiyomi.domain.manga.model.Manga

/** Draws the info box for real, so its backdrop's draw block runs. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
internal class InfoBoxDrawTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun backdropDraws() {
        compose.setContent {
            MaterialTheme {
                MangaInfoBox(
                    isTabletUi = true,
                    appBarPadding = 0.dp,
                    manga = Manga.create().copy(id = 1L, ogTitle = "Needle"),
                    sourceName = "Src",
                    isStubSource = true,
                    onCoverClick = {},
                    doSearch = { _, _ -> },
                )
            }
        }
        compose.onRoot().captureToImage()
        compose.onNodeWithText("Needle").assertExists()
    }
}
