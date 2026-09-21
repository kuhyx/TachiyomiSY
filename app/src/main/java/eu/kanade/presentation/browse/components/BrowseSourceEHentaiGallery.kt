package eu.kanade.presentation.browse.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowtham.ratingbar.ComposeStars
import com.gowtham.ratingbar.RatingBarStyle
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.manga.components.MangaCover
import exh.metadata.MetadataUtil
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.util.SourceTagsUtil
import exh.util.SourceTagsUtil.GenreColor
import exh.util.floor
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import java.time.ZoneId

private const val HALF_STAR = 0.5F

// The texts derived from the metadata off the main thread: language + page count, post date, genre, rating.
internal data class GalleryDetails(
    val languageText: String,
    val datePosted: String,
    val genre: Pair<GenreColor, StringResource>?,
    val rating: Float,
)

private val GENRES: Map<String, Pair<GenreColor, StringResource>> = mapOf(
    "doujinshi" to (GenreColor.DOUJINSHI_COLOR to SYMR.strings.doujinshi),
    "manga" to (GenreColor.MANGA_COLOR to SYMR.strings.entry_type_manga),
    "artistcg" to (GenreColor.ARTIST_CG_COLOR to SYMR.strings.artist_cg),
    "gamecg" to (GenreColor.GAME_CG_COLOR to SYMR.strings.game_cg),
    "western" to (GenreColor.WESTERN_COLOR to SYMR.strings.western),
    "non-h" to (GenreColor.NON_H_COLOR to SYMR.strings.non_h),
    "imageset" to (GenreColor.IMAGE_SET_COLOR to SYMR.strings.image_set),
    "cosplay" to (GenreColor.COSPLAY_COLOR to SYMR.strings.cosplay),
    "asianporn" to (GenreColor.ASIAN_PORN_COLOR to SYMR.strings.asian_porn),
    "misc" to (GenreColor.MISC_COLOR to SYMR.strings.misc),
)

@Composable
internal fun rememberGalleryDetails(metadata: EHentaiSearchMetadata): GalleryDetails {
    val context = LocalContext.current
    val languageText by produceState("", metadata) {
        value = withIOContext { context.languageAndPages(metadata) }
    }
    val datePosted by produceState("", metadata) {
        value = withIOContext {
            runCatching {
                metadata.datePosted?.let {
                    MetadataUtil.EX_DATE_FORMAT.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
                }
            }.getOrNull().orEmpty()
        }
    }
    val genre by produceState<Pair<GenreColor, StringResource>?>(null, metadata) {
        value = withIOContext { GENRES[metadata.genre] }
    }
    val rating by produceState(0f, metadata) {
        value = withIOContext {
            val rating = metadata.averageRating?.toFloat()
            rating?.div(HALF_STAR)?.floor()?.let { HALF_STAR.times(it) } ?: 0f
        }
    }
    return GalleryDetails(languageText, datePosted, genre, rating)
}

internal fun Context.languageAndPages(metadata: EHentaiSearchMetadata): String {
    val locale = SourceTagsUtil.getLocaleSourceUtil(
        metadata.tags
            .firstOrNull { it.namespace == EHentaiSearchMetadata.EH_LANGUAGE_NAMESPACE }
            ?.name,
    )
    val pageCount = metadata.length
    return when {
        locale != null && pageCount != null -> pluralStringResource(
            SYMR.plurals.browse_language_and_pages,
            pageCount,
            pageCount,
            locale.toLanguageTag().uppercase(),
        )
        pageCount != null -> pluralStringResource(SYMR.plurals.num_pages, pageCount, pageCount)
        else -> locale?.toLanguageTag()?.uppercase().orEmpty()
    }
}

@Composable
internal fun GalleryCover(manga: Manga, overlayColor: Color) {
    Box {
        MangaCover.Book(
            modifier = Modifier
                .fillMaxHeight()
                .drawWithContent {
                    drawContent()
                    if (manga.favorite) {
                        drawRect(overlayColor)
                    }
                },
            data = manga,
        )
        if (manga.favorite) {
            BadgeGroup(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopStart),
            ) {
                Badge(stringResource(MR.strings.in_library))
            }
        }
    }
}

// Stars and genre card on the left, language/pages and date on the right.
@Composable
internal fun GalleryFooter(metadata: EHentaiSearchMetadata, details: GalleryDetails) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            horizontalAlignment = Alignment.Start,
        ) {
            ComposeStars(
                value = details.rating,
                numOfStars = 5,
                size = 18.dp,
                spaceBetween = 2.dp,
                hideInactiveStars = false,
                style = RatingBarStyle.Fill(
                    activeColor = Color(color = 0xFF005ED7),
                    inActiveColor = Color(color = 0xE1E2ECFF),
                ),
                painterEmpty = null,
                painterFilled = null,
            )
            val color = details.genre?.first?.color
            val res = details.genre?.second
            Card(
                colors = if (color != null) CardDefaults.cardColors(Color(color)) else CardDefaults.cardColors(),
            ) {
                Text(
                    text = if (res != null) stringResource(res) else metadata.genre.orEmpty(),
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            horizontalAlignment = Alignment.End,
        ) {
            Text(details.languageText, maxLines = 1, fontSize = 14.sp)
            Text(details.datePosted, maxLines = 1, fontSize = 14.sp)
        }
    }
}
