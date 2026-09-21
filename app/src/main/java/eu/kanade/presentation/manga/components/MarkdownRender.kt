package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownInlineContent
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownAnnotator
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.NoOpImageTransformerImpl
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.rememberMarkdownState
import org.intellij.markdown.MarkdownTokenTypes.Companion.HTML_TAG
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.flavours.commonmark.CommonMarkMarkerProcessor
import org.intellij.markdown.flavours.gfm.table.GitHubTableMarkerProvider
import org.intellij.markdown.parser.MarkerProcessor
import org.intellij.markdown.parser.MarkerProcessorFactory
import org.intellij.markdown.parser.ProductionHolder
import org.intellij.markdown.parser.constraints.CommonMarkdownConstraints
import org.intellij.markdown.parser.constraints.MarkdownConstraints
import org.intellij.markdown.parser.markerblocks.MarkerBlockProvider
import org.intellij.markdown.parser.markerblocks.providers.AtxHeaderProvider
import org.intellij.markdown.parser.markerblocks.providers.BlockQuoteProvider
import org.intellij.markdown.parser.markerblocks.providers.CodeBlockProvider
import org.intellij.markdown.parser.markerblocks.providers.CodeFenceProvider
import org.intellij.markdown.parser.markerblocks.providers.HorizontalRuleProvider
import org.intellij.markdown.parser.markerblocks.providers.ListMarkerProvider
import org.intellij.markdown.parser.markerblocks.providers.SetextHeaderProvider
import tachiyomi.presentation.core.components.material.padding

internal const val MARKDOWN_INLINE_IMAGE_TAG = "MARKDOWN_INLINE_IMAGE"

@Composable
internal fun MarkdownRender(
    content: String,
    modifier: Modifier = Modifier,
    flavour: MarkdownFlavourDescriptor = SimpleMarkdownFlavourDescriptor,
    annotator: MarkdownAnnotator = remember { markdownAnnotator() },
    loadImages: Boolean = true,
) {
    Markdown(
        markdownState = rememberMarkdownState(
            content = content,
            flavour = flavour,
            immediate = true,
        ),
        annotator = annotator,
        colors = getMarkdownColors(),
        typography = getMarkdownTypography(),
        padding = markdownPadding,
        components = markdownComponents,
        imageTransformer = remember(loadImages) {
            if (loadImages) Coil3ImageTransformerImpl else NoOpImageTransformerImpl()
        },
        inlineContent = getMarkdownInlineContent(),
        modifier = modifier,
    )
}

@Composable
@ReadOnlyComposable
private fun getMarkdownColors(): MarkdownColors {
    val codeBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    return DefaultMarkdownColors(
        text = MaterialTheme.colorScheme.onSurface,
        codeBackground = codeBackground,
        inlineCodeBackground = codeBackground,
        dividerColor = MaterialTheme.colorScheme.outlineVariant,
        tableBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
    )
}

@Composable
@ReadOnlyComposable
internal fun getMarkdownLinkStyle() = MaterialTheme.typography.bodyMedium.copy(
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold,
)

@Composable
@ReadOnlyComposable
private fun getMarkdownTypography(): MarkdownTypography {
    val link = getMarkdownLinkStyle()
    return DefaultMarkdownTypography(
        h1 = MaterialTheme.typography.headlineMedium,
        h2 = MaterialTheme.typography.headlineSmall,
        h3 = MaterialTheme.typography.titleLarge,
        h4 = MaterialTheme.typography.titleMedium,
        h5 = MaterialTheme.typography.titleSmall,
        h6 = MaterialTheme.typography.bodyLarge,
        text = MaterialTheme.typography.bodyMedium,
        code = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        inlineCode = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        quote = MaterialTheme.typography.bodyMedium.plus(SpanStyle(fontStyle = FontStyle.Italic)),
        paragraph = MaterialTheme.typography.bodyMedium,
        ordered = MaterialTheme.typography.bodyMedium,
        bullet = MaterialTheme.typography.bodyMedium,
        list = MaterialTheme.typography.bodyMedium,
        textLink = TextLinkStyles(style = link.toSpanStyle()),
        table = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
@ReadOnlyComposable
private fun getMarkdownInlineContent() = DefaultMarkdownInlineContent(
    inlineContent = mapOf(
        MARKDOWN_INLINE_IMAGE_TAG to InlineTextContent(
            placeholder = Placeholder(
                width = MaterialTheme.typography.bodyMedium.fontSize * 1.25,
                height = MaterialTheme.typography.bodyMedium.fontSize * 1.25,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
            children = {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
        ),
    ),
)

private object SimpleMarkdownFlavourDescriptor : CommonMarkFlavourDescriptor() {
    override val markerProcessorFactory: MarkerProcessorFactory = SimpleMarkdownProcessFactory
}

private object SimpleMarkdownProcessFactory : MarkerProcessorFactory {
    override fun createMarkerProcessor(productionHolder: ProductionHolder): MarkerProcessor<*> =
        SimpleMarkdownMarkerProcessor(productionHolder, CommonMarkdownConstraints.BASE)
}

/**
 * Like `CommonMarkFlavour`, but with html blocks and reference links removed and
 * table support added.
 */
private class SimpleMarkdownMarkerProcessor(
    productionHolder: ProductionHolder,
    constraints: MarkdownConstraints,
) : CommonMarkMarkerProcessor(productionHolder, constraints) {
    private val markerBlockProviders = listOf(
        CodeBlockProvider(),
        HorizontalRuleProvider(),
        CodeFenceProvider(),
        SetextHeaderProvider(),
        BlockQuoteProvider(),
        ListMarkerProvider(),
        AtxHeaderProvider(),
        GitHubTableMarkerProvider(),
    )

    override fun getMarkerBlockProviders(): List<MarkerBlockProvider<StateInfo>> = markerBlockProviders
}

internal val DISALLOWED_MARKDOWN_TYPES = arrayOf(HTML_TAG)
