package eu.kanade.tachiyomi.source.online.all

import android.net.Uri
import androidx.core.net.toUri

// Follow-up: Consider gallery updating when doing tabbed browsing (https://github.com/kuhyx/TachiyomiSY/issues/20)
// Positions inside e-hentai's gallery-list markup and its rating sprite.
private const val FIRST_GALLERY_YEAR = 2007
private const val LAST_SEEK_YEAR = 2099

private val MATCH_YEAR_REGEX = "^\\d{4}\$".toRegex()
private val MATCH_SEEK_REGEX = """^\d{2,4}-\d{1,2}(-\d{1,2})?""".toRegex()
private val MATCH_JUMP_REGEX = "^\\d+(\$|d\$|w\$|m\$|y\$|-\$)".toRegex()

// The site "seek"s to a date (or a whole year) and "jump"s by a count of days/weeks/months/years.
internal fun Uri.Builder.appendJumpOrSeek(value: String) {
    when {
        MATCH_SEEK_REGEX.matches(value) || value.isSeekYear() -> appendQueryParameter("seek", value)
        MATCH_JUMP_REGEX.matches(value) -> appendQueryParameter("jump", value)
    }
}

private fun String.isSeekYear(): Boolean =
    // Four ASCII digits always parse, so no null check is needed after the match.
    MATCH_YEAR_REGEX.matches(this) && toInt() in FIRST_GALLERY_YEAR..LAST_SEEK_YEAR

internal fun toplistUrl(toplist: ToplistOption, page: Int): String = "https://e-hentai.org".toUri().buildUpon()
    .appendPath("toplist.php")
    .appendQueryParameter("tl", toplist.index.toString())
    .appendQueryParameter("p", (page - 1).toString())
    .toString()
