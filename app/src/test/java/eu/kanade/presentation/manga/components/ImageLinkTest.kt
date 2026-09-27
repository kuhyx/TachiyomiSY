package eu.kanade.presentation.manga.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import io.kotest.matchers.shouldBe
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.junit.jupiter.api.Test

private const val CONTENT = "alt http://x title"

private fun leaf(type: IElementType, start: Int, end: Int): ASTNode = LeafASTNode(type, start, end)

private fun node(type: IElementType, vararg children: ASTNode): ASTNode = CompositeASTNode(type, children.toList())

private val altText = node(MarkdownElementTypes.LINK_TEXT, leaf(MarkdownTokenTypes.TEXT, 0, 3))
private val destination = node(MarkdownElementTypes.LINK_DESTINATION, leaf(MarkdownTokenTypes.TEXT, 4, 12))

private fun image(vararg linkChildren: ASTNode): ASTNode =
    node(MarkdownElementTypes.IMAGE, node(MarkdownElementTypes.INLINE_LINK, *linkChildren))

private fun annotate(image: ASTNode): Triple<Boolean, String, String?> {
    val builder = AnnotatedString.Builder()
    val result = builder.annotateImageAsLink(CONTENT, image, SpanStyle())
    val built = builder.toAnnotatedString()
    val url = built.getLinkAnnotations(0, built.length).firstOrNull()?.item as? LinkAnnotation.Url
    return Triple(result, built.text, url?.url)
}

internal class ImageLinkTest {
    @Test
    fun destinationAndText() {
        annotate(image(altText, destination)) shouldBe Triple(true, "�alt", "http://x")
    }

    @Test
    fun titleWinsOverText() {
        val title = node(MarkdownElementTypes.LINK_TITLE, leaf(MarkdownTokenTypes.TEXT, 13, 18))
        annotate(image(altText, destination, title)).second shouldBe "�title"
    }

    @Test
    fun missingTextIsEmpty() {
        val bare = node(MarkdownElementTypes.LINK_TEXT, leaf(MarkdownTokenTypes.LPAREN, 0, 1))
        annotate(image(bare, destination)).second shouldBe "�"
        annotate(image(destination)).second shouldBe "�"
    }

    @Test
    fun autolinkDestination() {
        val autolink = node(MarkdownElementTypes.AUTOLINK, leaf(MarkdownTokenTypes.AUTOLINK, 4, 12))
        annotate(image(altText, autolink)).third shouldBe "http://x"
    }

    @Test
    fun unusableImagesAreSkipped() {
        annotate(node(MarkdownElementTypes.IMAGE, altText)).first shouldBe false
        annotate(image(altText)).first shouldBe false
        annotate(image(altText, node(MarkdownElementTypes.AUTOLINK, altText))).first shouldBe false
    }
}
