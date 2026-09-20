package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val START = "start"
private const val END = "end"

/** Panel widths on the default 320 dp screen: half each, insets added to the outer edges. */
@RunWith(RobolectricTestRunner::class)
internal class TwoPanelBoxTest {
    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)

    private fun setPanels(modifier: Modifier? = null, insets: WindowInsets? = null, rtl: Boolean = false) {
        compose.setContent {
            val direction = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                MaterialTheme {
                    if (modifier == null && insets == null) {
                        TwoPanelBox(startContent = { Panel(START) }, endContent = { Panel(END) })
                    } else {
                        TwoPanelBox(
                            startContent = { Panel(START) },
                            endContent = { Panel(END) },
                            modifier = modifier ?: Modifier,
                            contentWindowInsets = insets,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun splitsWidthInHalf() {
        setPanels()
        compose.onNodeWithTag(START).assertWidthIsEqualTo(160.dp)
        compose.onNodeWithTag(START).assertLeftPositionInRootIsEqualTo(0.dp)
        compose.onNodeWithTag(END).assertWidthIsEqualTo(160.dp)
        compose.onNodeWithTag(END).assertLeftPositionInRootIsEqualTo(160.dp)
    }

    @Test
    fun addsInsetsToOuterEdges() {
        setPanels(insets = WindowInsets(left = 20, right = 10))
        compose.onNodeWithTag(START).assertWidthIsEqualTo(165.dp)
        compose.onNodeWithTag(START).assertLeftPositionInRootIsEqualTo(0.dp)
        compose.onNodeWithTag(END).assertWidthIsEqualTo(155.dp)
        compose.onNodeWithTag(END).assertLeftPositionInRootIsEqualTo(165.dp)
    }

    @Test
    fun mirrorsInsetsInRtl() {
        setPanels(insets = WindowInsets(left = 20, right = 10), rtl = true)
        compose.onNodeWithTag(START).assertWidthIsEqualTo(155.dp)
        compose.onNodeWithTag(START).assertLeftPositionInRootIsEqualTo(165.dp)
        compose.onNodeWithTag(END).assertWidthIsEqualTo(165.dp)
        compose.onNodeWithTag(END).assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun appliesGivenModifier() {
        setPanels(modifier = Modifier.testTag("panels"))
        compose.onNodeWithTag("panels").assertWidthIsEqualTo(320.dp)
        compose.onNodeWithTag(START).assertWidthIsEqualTo(160.dp)
    }

    @Test
    fun unchangedRecomposeIsInert() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    TwoPanelBox(startContent = { Panel(START) }, endContent = { Panel(END) })
                }
            }
        }
        compose.runOnIdle { tick++ }
        compose.waitForIdle()
        compose.onNodeWithText("tick 1").assertIsDisplayed()
        compose.onNodeWithTag(START).assertWidthIsEqualTo(160.dp)
    }
}

/** On a 1000 dp screen the start panel stops at 450 dp and the end panel takes the rest. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1000dp-h800dp-mdpi")
internal class TwoPanelBoxWideTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun capsStartPanelAt450Dp() {
        compose.setContent {
            MaterialTheme {
                TwoPanelBox(startContent = { Panel(START) }, endContent = { Panel(END) })
            }
        }
        compose.onNodeWithTag(START).assertWidthIsEqualTo(450.dp)
        compose.onNodeWithTag(END).assertWidthIsEqualTo(550.dp)
        compose.onNodeWithTag(END).assertLeftPositionInRootIsEqualTo(450.dp)
    }
}

@Composable
private fun Panel(tag: String) {
    Box(modifier = Modifier.fillMaxSize().testTag(tag))
}
