package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import cafe.adriel.voyager.core.screen.Screen

/** A Voyager screen that shows [body]; lets a test host a bare composable under a Navigator. */
internal class ComposableScreen(private val body: @Composable () -> Unit) : Screen {
    @Composable
    override fun Content() {
        body()
    }
}

/** Waits until a node with [label] as text or content description exists. */
internal fun ComposeContentTestRule.waitForLabel(label: String) {
    try {
        waitUntil(WAIT) { hasLabel(label) }
    } catch (expected: ComposeTimeoutException) {
        val texts = onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() }
        throw AssertionError("no \"$label\" among $texts", expected)
    }
}

/** Whether a node with [label] as text or content description exists right now. */
internal fun ComposeContentTestRule.hasLabel(label: String): Boolean =
    onAllNodes(hasText(label) or hasContentDescription(label), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
