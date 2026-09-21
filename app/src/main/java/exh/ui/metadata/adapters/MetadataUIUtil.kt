package exh.ui.metadata.adapters

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.core.content.ContextCompat
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.dpToPx
import exh.util.SourceTagsUtil.GenreColor
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.sy.SYMR
import kotlin.math.roundToInt

// Every spelling a source uses for a genre, to its badge colour and label.
private val GENRE_PRESENTATION: Map<String, Pair<GenreColor, StringResource>> = buildMap {
    fun genre(vararg names: String, color: GenreColor, label: StringResource) {
        names.forEach { put(it, color to label) }
    }
    genre("doujinshi", "Doujinshi", color = GenreColor.DOUJINSHI_COLOR, label = SYMR.strings.doujinshi)
    genre(
        "manga", "Japanese Manga", "Manga",
        color = GenreColor.MANGA_COLOR,
        label = SYMR.strings.entry_type_manga,
    )
    genre(
        "artistcg", "artist CG", "artist-cg", "Artist CG",
        color = GenreColor.ARTIST_CG_COLOR,
        label = SYMR.strings.artist_cg,
    )
    genre(
        "gamecg", "game CG", "game-cg", "Game CG",
        color = GenreColor.GAME_CG_COLOR,
        label = SYMR.strings.game_cg,
    )
    genre("western", color = GenreColor.WESTERN_COLOR, label = SYMR.strings.western)
    genre("non-h", "non-H", color = GenreColor.NON_H_COLOR, label = SYMR.strings.non_h)
    genre("imageset", "image Set", color = GenreColor.IMAGE_SET_COLOR, label = SYMR.strings.image_set)
    genre("cosplay", color = GenreColor.COSPLAY_COLOR, label = SYMR.strings.cosplay)
    genre("asianporn", "asian Porn", color = GenreColor.ASIAN_PORN_COLOR, label = SYMR.strings.asian_porn)
    genre("misc", color = GenreColor.MISC_COLOR, label = SYMR.strings.misc)
    genre("Korean Manhwa", color = GenreColor.ARTIST_CG_COLOR, label = SYMR.strings.entry_type_manhwa)
    genre("Chinese Manhua", color = GenreColor.GAME_CG_COLOR, label = SYMR.strings.entry_type_manhua)
    genre("Comic", color = GenreColor.WESTERN_COLOR, label = SYMR.strings.entry_type_comic)
    genre("artbook", color = GenreColor.IMAGE_SET_COLOR, label = SYMR.strings.artbook)
    genre("webtoon", color = GenreColor.NON_H_COLOR, label = SYMR.strings.entry_type_webtoon)
    genre("Video", color = GenreColor.WESTERN_COLOR, label = SYMR.strings.video)
}

internal object MetadataUIUtil {
    // One label per whole star, 0 to 10.
    private val RATING_LABELS = listOf(
        SYMR.strings.rating0,
        SYMR.strings.rating1,
        SYMR.strings.rating2,
        SYMR.strings.rating3,
        SYMR.strings.rating4,
        SYMR.strings.rating5,
        SYMR.strings.rating6,
        SYMR.strings.rating7,
        SYMR.strings.rating8,
        SYMR.strings.rating9,
        SYMR.strings.rating10,
    )

    fun getRatingString(
        context: Context,
        @FloatRange(from = 0.0, to = 10.0) rating: Float? = null,
    ) = context.stringResource(RATING_LABELS.getOrNull(rating?.roundToInt() ?: -1) ?: SYMR.strings.no_rating)

    fun getGenreAndColour(context: Context, genre: String): Pair<Int, String>? =
        GENRE_PRESENTATION[genre]?.let { (genreColor, stringId) ->
            genreColor.color to context.stringResource(stringId)
        }

    fun TextView.bindDrawable(context: Context, @DrawableRes drawable: Int) {
        ContextCompat.getDrawable(context, drawable)?.apply {
            setTint(context.getResourceColor(R.attr.colorAccent))
            setBounds(0, 0, 20.dpToPx, 20.dpToPx)
            setCompoundDrawables(this, null, null, null)
        }
    }

    /**
     * Returns the color for the given attribute.
     *
     * @param resource the attribute.
     * @param alphaFactor the alpha number [0,1].
     */
    @ColorInt
    fun Context.getResourceColor(@AttrRes resource: Int, alphaFactor: Float = 1f): Int {
        val typedArray = obtainStyledAttributes(intArrayOf(resource))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()

        if (alphaFactor < 1f) {
            val alpha = (color.alpha * alphaFactor).roundToInt()
            return Color.argb(alpha, color.red, color.green, color.blue)
        }

        return color
    }
}
