package eu.kanade.presentation.manga.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.ChipBorder
import eu.kanade.presentation.components.SuggestionChip
import eu.kanade.presentation.components.SuggestionChipDefaults
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.util.SourceTagsUtil
import androidx.compose.material3.SuggestionChipDefaults as SuggestionChipDefaultsM3

private const val FEMALE = "Female"

@Immutable
internal data class DisplayTag(
    val namespace: String?,
    val text: String,
    val search: String,
    val border: Int?,
)

@Immutable
@JvmInline
internal value class SearchMetadataChips(
    val tags: Map<String, List<DisplayTag>>,
) {
    companion object {
        operator fun invoke(meta: RaisedSearchMetadata?, sourceId: Long, tags: List<String>?): SearchMetadataChips? {
            return when {
                meta != null -> SearchMetadataChips(
                    meta.tags
                        .filterNot { it.type == RaisedSearchMetadata.TAG_TYPE_VIRTUAL }
                        .map { it.toDisplayTag(sourceId) }
                        .groupBy { it.namespace.orEmpty() },
                )
                tags != null && tags.all { it.contains(':') } -> SearchMetadataChips(
                    tags.map(::parseNamespacedTag).groupBy { it.namespace.orEmpty() },
                )
                else -> null
            }
        }
    }
}

private fun RaisedTag.toDisplayTag(sourceId: Long): DisplayTag = DisplayTag(
    namespace = namespace,
    text = name,
    search = if (namespace.isNullOrEmpty()) {
        SourceTagsUtil.getWrappedTag(sourceId, fullTag = name)
    } else {
        SourceTagsUtil.getWrappedTag(sourceId, namespace = namespace, tag = name)
    } ?: name,
    border = ehBorder(sourceId, type),
)

// Only E-Hentai grades its tags; the border width shows the grade.
private fun ehBorder(sourceId: Long, tagType: Int): Int? {
    if (sourceId != EXH_SOURCE_ID && sourceId != EH_SOURCE_ID) return null
    return when (tagType) {
        EHentaiSearchMetadata.TAG_TYPE_NORMAL -> 2
        EHentaiSearchMetadata.TAG_TYPE_LIGHT -> 1
        else -> null
    }
}

// A plain "namespace:name" genre string from a source without metadata.
private fun parseNamespacedTag(tag: String): DisplayTag {
    val index = tag.indexOf(':')
    return DisplayTag(tag.substring(0, index).trim(), tag.substring(index + 1).trim(), tag, null)
}

@Composable
internal fun NamespaceTags(
    tags: SearchMetadataChips,
    onClick: (item: String) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        tags.tags.forEach { (namespace, tags) ->
            Row(Modifier.padding(start = 16.dp)) {
                if (namespace.isNotEmpty()) {
                    TagsChip(
                        modifier = Modifier.padding(top = 4.dp),
                        text = namespace,
                        onClick = null,
                    )
                }
                FlowRow(
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tags.forEach { tag ->
                        val borderDp = tag.border?.dp
                        TagsChip(
                            modifier = Modifier.padding(vertical = 4.dp),
                            text = tag.text,
                            onClick = { onClick(tag.search) },
                            border = borderDp?.let {
                                SuggestionChipDefaults.suggestionChipBorder(borderWidth = it)
                            } ?: SuggestionChipDefaults.suggestionChipBorder(),
                            borderM3 = borderDp?.let {
                                SuggestionChipDefaultsM3.suggestionChipBorder(enabled = true, borderWidth = it)
                            } ?: SuggestionChipDefaultsM3.suggestionChipBorder(enabled = true),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TagsChip(
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    border: ChipBorder? = SuggestionChipDefaults.suggestionChipBorder(),
    borderM3: BorderStroke? = SuggestionChipDefaultsM3.suggestionChipBorder(enabled = true),
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        if (onClick != null) {
            SuggestionChip(
                modifier = modifier,
                onClick = onClick,
                label = {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                border = borderM3,
            )
        } else {
            SuggestionChip(
                modifier = modifier,
                label = {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                border = border,
            )
        }
    }
}

@PreviewLightDark
@Composable
internal fun NamespaceTagsPreview() {
    TachiyomiPreviewTheme {
        Surface {
            NamespaceTags(
                tags = remember {
                    EHentaiSearchMetadata().apply {
                        this.tags.addAll(
                            arrayOf(
                                RaisedTag(
                                    "Male",
                                    "Test",
                                    EHentaiSearchMetadata.TAG_TYPE_NORMAL,
                                ),
                                RaisedTag(
                                    "Male",
                                    "Test2",
                                    EHentaiSearchMetadata.TAG_TYPE_WEAK,
                                ),
                                RaisedTag(
                                    "Male",
                                    "Test3",
                                    EHentaiSearchMetadata.TAG_TYPE_LIGHT,
                                ),
                                RaisedTag(
                                    FEMALE,
                                    "Test",
                                    EHentaiSearchMetadata.TAG_TYPE_NORMAL,
                                ),
                                RaisedTag(
                                    FEMALE,
                                    "Test2",
                                    EHentaiSearchMetadata.TAG_TYPE_WEAK,
                                ),
                                RaisedTag(
                                    FEMALE,
                                    "Test3",
                                    EHentaiSearchMetadata.TAG_TYPE_LIGHT,
                                ),
                            ),
                        )
                    }.let { SearchMetadataChips(it, EXH_SOURCE_ID, emptyList()) }!!
                },
                onClick = {},
            )
        }
    }
}
