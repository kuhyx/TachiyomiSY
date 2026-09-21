package exh.ui.metadata.adapters

import android.content.Context
import android.widget.TextView
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import kotlin.math.round

// Ratings are shown to two decimals.
private const val HUNDREDTHS = 100.0

/** Shows [genre] on its badge colour when the genre is known, else the raw name, else "unknown". */
internal fun TextView.bindGenre(context: Context, genre: String?) {
    val known = genre?.let { MetadataUIUtil.getGenreAndColour(context, it) }
    known?.let { (color, _) -> setBackgroundColor(color) }
    text = known?.second ?: genre ?: context.stringResource(MR.strings.unknown)
}

/** `x.xx - <word>`: [rating] to two decimals plus the label for [outOfTen], the same rating on a 0-10 scale. */
internal fun ratingText(context: Context, rating: Float?, outOfTen: Float?): String =
    (round((rating ?: 0F) * HUNDREDTHS) / HUNDREDTHS).toString() + " - " +
        MetadataUIUtil.getRatingString(context, outOfTen)

/** Long-pressing any of [views] copies its text to the clipboard. */
internal fun copyTextOnLongClick(context: Context, vararg views: TextView) {
    views.forEach { textView ->
        textView.setOnLongClickListener {
            context.copyToClipboard(textView.text.toString(), textView.text.toString())
            true
        }
    }
}
