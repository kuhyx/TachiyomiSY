package eu.kanade.presentation.manga.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.MarkdownSuccess
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.State
import eu.kanade.presentation.browse.UiDispatcherReset
import io.kotest.matchers.shouldBe
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val Render = Class.forName("eu.kanade.presentation.manga.components.MarkdownRenderKt")

// The file's private color and typography builders, as the renderer itself uses them.
@Composable
private fun renderPart(name: String): Any? =
    Render.getDeclaredMethod(name, Composer::class.java, Int::class.javaPrimitiveType)
        .apply { isAccessible = true }
        .invoke(null, currentComposer, 0)

private const val LIST = "- a\n    - b\n"

private fun listNode(): ASTNode =
    MarkdownParser(CommonMarkFlavourDescriptor(), true, CancellationToken.NonCancellable)
        .buildMarkdownTreeFromString(StringBuilder(LIST))
        .children
        .first { it.type == MarkdownElementTypes.UNORDERED_LIST }

/** Renders nodes no parse of ours produces: a top-level HTML tag, and a bullet list recomposed as a restart. */
@RunWith(RobolectricTestRunner::class)
internal class MarkdownComponentsTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private var tick by mutableIntStateOf(0)

    @Test
    fun topLevelHtmlAndRestartedList() {
        val content = "<b>tag</b>"
        val root = CompositeASTNode(
            MarkdownElementTypes.MARKDOWN_FILE,
            listOf(
                LeafASTNode(MarkdownTokenTypes.HTML_TAG, 0, content.length),
                LeafASTNode(MarkdownTokenTypes.TEXT, 0, content.length),
            ),
        )
        compose.setContent {
            MaterialTheme {
                Markdown(
                    state = State.Success(root, content, true),
                    colors = renderPart("getMarkdownColors") as MarkdownColors,
                    typography = renderPart("getMarkdownTypography") as MarkdownTypography,
                    components = markdownComponents,
                    success = { state, components, modifier ->
                        Text("tick $tick")
                        MarkdownSuccess(state, components, modifier)
                        val model = MarkdownComponentModel(LIST, listNode(), LocalMarkdownTypography.current)
                        val list = components.unorderedList
                        // Same argument, restart bit set: the list runs with its marker lambda already remembered.
                        list.javaClass.getMethod("invoke", Any::class.java, Any::class.java, Any::class.java)
                            .invoke(list, model, currentComposer, 0b011)
                    },
                )
            }
        }
        compose.runOnIdle { tick++ }
        compose.waitForIdle()
        compose.onAllNodesWithText("tag", substring = true).fetchSemanticsNodes().size shouldBe 2
    }
}
