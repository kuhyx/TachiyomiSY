package eu.kanade.presentation.manga.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.tachiyomi.R
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.roundToInt

private const val EMPTY_NOTES_LINES = 2
private const val NOTES_LINES = 5
private val DefaultTagChipModifier = Modifier.padding(vertical = 4.dp)

@Composable
internal fun ExpandableMangaDescription(
    defaultExpandState: Boolean,
    description: String?,
    tagsProvider: () -> List<String>?,
    notes: String,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
    onEditNotes: () -> Unit,
    // SY -->
    searchMetadataChips: SearchMetadataChips?,
    doSearch: (query: String, global: Boolean) -> Unit,
    // SY <--
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val (expanded, onExpanded) = rememberSaveable {
            mutableStateOf(defaultExpandState)
        }
        val desc =
            description.takeIf { !it.isNullOrBlank() } ?: stringResource(MR.strings.description_placeholder)

        MangaSummary(
            description = desc,
            expanded = expanded,
            notes = notes,
            onEditNotesClicked = onEditNotes,
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
                .clickableNoIndication { onExpanded(!expanded) },
        )
        val tags = tagsProvider()
        if (!tags.isNullOrEmpty()) {
            TagsSection(
                tags = tags,
                expanded = expanded,
                searchMetadataChips = searchMetadataChips,
                onTagSearch = onTagSearch,
                onGlobalSearch = { doSearch(it, true) },
                onCopyTagToClipboard = onCopyTagToClipboard,
            )
        }
    }
}

// The tag chips: a scrolling row when collapsed, a wrapping grid (or SY namespace groups) when expanded. Any tap
// opens the search/copy menu for that tag.
@Composable
private fun TagsSection(
    tags: List<String>,
    expanded: Boolean,
    searchMetadataChips: SearchMetadataChips?,
    onTagSearch: (String) -> Unit,
    onGlobalSearch: (String) -> Unit,
    onCopyTagToClipboard: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .padding(vertical = 12.dp)
            .animateContentSize(animationSpec = spring())
            .fillMaxWidth(),
    ) {
        var showMenu by remember { mutableStateOf(false) }
        var tagSelected by remember { mutableStateOf("") }
        val onTagClick: (String) -> Unit = {
            tagSelected = it
            showMenu = true
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            TagMenuItem(MR.strings.action_search) {
                onTagSearch(tagSelected)
                showMenu = false
            }
            // SY -->
            TagMenuItem(MR.strings.action_global_search) {
                onGlobalSearch(tagSelected)
                showMenu = false
            }
            // SY <--
            TagMenuItem(MR.strings.action_copy_to_clipboard) {
                onCopyTagToClipboard(tagSelected)
                showMenu = false
            }
        }
        when {
            // SY -->
            expanded && searchMetadataChips != null -> NamespaceTags(tags = searchMetadataChips, onClick = onTagClick)
            // SY <--
            expanded -> FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                tags.forEach {
                    TagsChip(modifier = DefaultTagChipModifier, text = it, onClick = { onTagClick(it) })
                }
            }
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                items(items = tags) {
                    TagsChip(modifier = DefaultTagChipModifier, text = it, onClick = { onTagClick(it) })
                }
            }
        }
    }
}

@Composable
private fun TagMenuItem(label: StringResource, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(label)) },
        onClick = onClick,
    )
}

@Composable
private fun descriptionAnnotator(loadImages: Boolean, linkStyle: SpanStyle) = remember(loadImages, linkStyle) {
    markdownAnnotator(
        annotate = { content, child ->
            if (!loadImages && child.type == MarkdownElementTypes.IMAGE) {
                annotateImageAsLink(content, child, linkStyle)
            } else {
                if (child.type in DISALLOWED_MARKDOWN_TYPES) {
                    append(content.substring(child.startOffset, child.endOffset))
                    true
                } else {
                    false
                }
            }
        },
        config = markdownAnnotatorConfig(
            eolAsNewLine = true,
        ),
    )
}

@Composable
private fun MangaSummary(
    description: String,
    notes: String,
    expanded: Boolean,
    onEditNotesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        label = "summary",
    )
    var infoHeight by remember { mutableIntStateOf(0) }
    Layout(
        modifier = modifier.clipToBounds(),
        contents = listOf(
            {
                Text(
                    // Shows at least 3 lines if no notes
                    // when there are notes show 6
                    text = "\n".repeat(if (notes.isBlank()) EMPTY_NOTES_LINES else NOTES_LINES),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            {
                SummaryBody(
                    description = description,
                    notes = notes,
                    expanded = expanded,
                    onEditNotesClicked = onEditNotesClicked,
                    modifier = Modifier.onSizeChanged { size -> infoHeight = size.height },
                )
            },
            { ExpandScrim(expanded) },
        ),
    ) { (shrunk, actual, scrim), constraints ->
        val shrunkHeight = shrunk.single()
            .measure(constraints)
            .height
        val heightDelta = infoHeight - shrunkHeight
        val scrimHeight = 24.dp.roundToPx()

        val actualPlaceable = actual.single()
            .measure(constraints)
        val scrimPlaceable = scrim.single()
            .measure(Constraints.fixed(width = constraints.maxWidth, height = scrimHeight))

        val currentHeight = shrunkHeight + ((heightDelta + scrimHeight) * animProgress).roundToInt()
        layout(constraints.maxWidth, currentHeight) {
            actualPlaceable.place(0, 0)

            val scrimY = currentHeight - scrimHeight
            scrimPlaceable.place(0, scrimY)
        }
    }
}

@Composable
private fun SummaryBody(
    description: String,
    notes: String,
    expanded: Boolean,
    onEditNotesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = remember { Injekt.get<UiPreferences>() }
    val loadImages = remember { preferences.imagesInDescription.get() }
    Column(modifier = modifier) {
        MangaNotesSection(
            content = notes,
            expanded = expanded,
            onEditNotes = onEditNotesClicked,
        )
        SelectionContainer {
            MarkdownRender(
                content = description,
                modifier = Modifier.secondaryItemAlpha(),
                annotator = descriptionAnnotator(
                    loadImages = loadImages,
                    linkStyle = getMarkdownLinkStyle().toSpanStyle(),
                ),
                loadImages = loadImages,
            )
        }
    }
}

// The fade over the collapsed summary, with the animated caret.
@Composable
private fun ExpandScrim(expanded: Boolean) {
    val colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
    Box(
        modifier = Modifier.background(Brush.verticalGradient(colors = colors)),
        contentAlignment = Alignment.Center,
    ) {
        val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_caret_down)
        Icon(
            painter = rememberAnimatedVectorPainter(image, !expanded),
            contentDescription = stringResource(
                if (expanded) MR.strings.manga_info_collapse else MR.strings.manga_info_expand,
            ),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.background(Brush.radialGradient(colors = colors.asReversed())),
        )
    }
}

// Renders an image node as a link to it (images are off); false when the node has no destination.
private fun AnnotatedString.Builder.annotateImageAsLink(
    content: String,
    child: ASTNode,
    linkStyle: SpanStyle,
): Boolean {
    val inlineLink = child.findChildOfType(MarkdownElementTypes.INLINE_LINK)

    val url = inlineLink?.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)
        ?.getUnescapedTextInNode(content)
        ?: inlineLink?.findChildOfType(MarkdownElementTypes.AUTOLINK)
            ?.findChildOfType(MarkdownTokenTypes.AUTOLINK)
            ?.getUnescapedTextInNode(content)
        ?: return false

    val textNode = inlineLink?.findChildOfType(MarkdownElementTypes.LINK_TITLE)
        ?: inlineLink?.findChildOfType(MarkdownElementTypes.LINK_TEXT)
    val altText = textNode?.findChildOfType(MarkdownTokenTypes.TEXT)
        ?.getUnescapedTextInNode(content)
        .orEmpty()

    withLink(LinkAnnotation.Url(url = url)) {
        pushStyle(linkStyle)
        appendInlineContent(MARKDOWN_INLINE_IMAGE_TAG)
        append(altText)
        pop()
    }

    return true
}
