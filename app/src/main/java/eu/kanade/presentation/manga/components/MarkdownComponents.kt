package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.LocalBulletListHandler
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownBulletList
import com.mikepenz.markdown.compose.elements.MarkdownDivider
import com.mikepenz.markdown.compose.elements.MarkdownOrderedList
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.compose.elements.MarkdownText
import com.mikepenz.markdown.compose.elements.listDepth
import com.mikepenz.markdown.model.MarkdownAlertPadding
import com.mikepenz.markdown.model.MarkdownPadding
import com.mikepenz.markdown.model.markdownAlertPadding
import tachiyomi.presentation.core.components.material.padding

// The padding and block renderers the markdown view is built from.
internal val markdownPadding = object : MarkdownPadding {
    override val alert: MarkdownAlertPadding = markdownAlertPadding()
    override val block: Dp = 2.dp
    override val blockQuote: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    override val blockQuoteBar: PaddingValues.Absolute = PaddingValues.Absolute(
        left = 4.dp,
        top = 2.dp,
        right = 4.dp,
        bottom = 2.dp,
    )
    override val blockQuoteText: PaddingValues = PaddingValues(vertical = 4.dp)
    override val codeBlock: PaddingValues = PaddingValues(8.dp)
    override val list: Dp = 0.dp
    override val listIndent: Dp = 8.dp
    override val listItemBottom: Dp = 0.dp
    override val listItemTop: Dp = 0.dp
}

internal val markdownComponents = markdownComponents(
    horizontalRule = {
        MarkdownDivider(
            modifier = Modifier
                .padding(vertical = MaterialTheme.padding.extraSmall)
                .fillMaxWidth(),
        )
    },
    orderedList = { ol ->
        Column(modifier = Modifier.padding(start = MaterialTheme.padding.small)) {
            MarkdownOrderedList(
                content = ol.content,
                node = ol.node,
                style = ol.typography.ordered,
                depth = ol.listDepth,
                markerModifier = { Modifier.alignBy(FirstBaseline) },
                listModifier = { Modifier.alignBy(FirstBaseline) },
            )
        }
    },
    unorderedList = { ul ->
        val markers = listOf("•", "◦", "▸", "▹")

        CompositionLocalProvider(
            LocalBulletListHandler provides { _, _, _, _, _ -> "${markers[ul.listDepth % markers.size]} " },
        ) {
            Column(modifier = Modifier.padding(start = MaterialTheme.padding.small)) {
                MarkdownBulletList(
                    content = ul.content,
                    node = ul.node,
                    style = ul.typography.bullet,
                    markerModifier = { Modifier.alignBy(FirstBaseline) },
                    listModifier = { Modifier.alignBy(FirstBaseline) },
                )
            }
        }
    },
    table = { t ->
        MarkdownTable(
            content = t.content,
            node = t.node,
            style = t.typography.text,
            headerBlock = { content, header, tableWidth, style ->
                MarkdownTableHeader(
                    content = content,
                    header = header,
                    tableWidth = tableWidth,
                    style = style,
                    maxLines = Int.MAX_VALUE,
                )
            },
            rowBlock = { content, header, tableWidth, style ->
                MarkdownTableRow(
                    content = content,
                    header = header,
                    tableWidth = tableWidth,
                    style = style,
                    maxLines = Int.MAX_VALUE,
                )
            },
        )
    },
    custom = { type, model ->
        if (type in DISALLOWED_MARKDOWN_TYPES) {
            MarkdownText(
                content = model.content.substring(model.node.startOffset, model.node.endOffset),
                node = model.node,
                style = model.typography.text,
            )
        }
    },
)
