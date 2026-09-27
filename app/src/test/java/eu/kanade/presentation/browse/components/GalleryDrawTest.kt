package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.presentation.browse.browseManga
import eu.kanade.presentation.util.PresentationKoin
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Draws gallery covers for real, so the favourite overlay's draw block runs both ways. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
internal class GalleryDrawTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun coversDraw() {
        compose.setContent {
            MaterialTheme {
                Row(Modifier.height(100.dp)) {
                    GalleryCover(browseManga(1L, favorite = true), Color.Black)
                    GalleryCover(browseManga(2L), Color.Black)
                }
            }
        }
        compose.onRoot().captureToImage()
        compose.onNodeWithText("In library").assertExists()
    }
}
