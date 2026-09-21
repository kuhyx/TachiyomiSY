package eu.kanade.tachiyomi.source.online.all

import android.net.Uri
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import exh.eh.EHTags
import exh.log.xLogD
import exh.util.UriFilter
import exh.util.UriGroup
import exh.util.dropBlank
import exh.util.trimAll

/*
 * The e-hentai search filters and the query they combine into. Top-level so the source class
 * stays about requests and parsing.
 */

internal class Watched(internal val isEnabled: Boolean) : Filter.CheckBox("Watched List", isEnabled), UriFilter {
    override fun addToUri(builder: Uri.Builder) {
        if (state) {
            builder.appendPath("watched")
        }
    }
}

internal enum class ToplistOption(internal val humanName: String, internal val index: Int) {
    NONE("None", index = 0),
    ALL_TIME("All time", index = 11),
    PAST_YEAR("Past year", index = 12),
    PAST_MONTH("Past month", index = 13),
    YESTERDAY("Yesterday", index = 15),
    ;

    override fun toString(): String = humanName
}

internal class ToplistOptions : Filter.Select<ToplistOption>(
    "Toplists",
    ToplistOption.entries.toTypedArray(),
)

internal class GenreOption(name: String, internal val genreId: Int) : Filter.CheckBox(name, false)
internal class GenreGroup :
    Filter.Group<GenreOption>(
        "Genres",
        listOf(
            GenreOption("Dōjinshi", genreId = 2),
            GenreOption("Manga", genreId = 4),
            GenreOption("Artist CG", genreId = 8),
            GenreOption("Game CG", genreId = 16),
            GenreOption("Western", genreId = 512),
            GenreOption("Non-H", genreId = 256),
            GenreOption("Image Set", genreId = 32),
            GenreOption("Cosplay", genreId = 64),
            GenreOption("Asian Porn", genreId = 128),
            GenreOption("Misc", genreId = 1),
        ),
    ),
    UriFilter {
    override fun addToUri(builder: Uri.Builder) {
        val bits = state.fold(0) { acc, genre ->
            if (!genre.state) acc + genre.genreId else acc
        }
        builder.appendQueryParameter("f_cats", bits.toString())
    }
}

internal class AdvancedOption(
    name: String,
    internal val param: String,
    defValue: Boolean = false,
) : Filter.CheckBox(name, defValue), UriFilter {
    override fun addToUri(builder: Uri.Builder) {
        if (state) {
            builder.appendQueryParameter(param, "on")
        }
    }
}

internal open class PageOption(name: String, private val queryKey: String) : Filter.Text(name), UriFilter {
    override fun addToUri(builder: Uri.Builder) {
        if (state.isNotBlank()) {
            if (builder.build().getQueryParameters("f_sp").isEmpty()) {
                builder.appendQueryParameter("f_sp", "on")
            }

            builder.appendQueryParameter(queryKey, state.trim())
        }
    }
}

/** Builds the `f_search` query from the selected filters (logged at debug). */
internal object EHentaiQuery {
    fun combine(filters: FilterList): String {
        val stringBuilder = StringBuilder()
        val advSearch = filters.filterIsInstance<Filter.AutoComplete>().flatMap { filter ->
            filter.state.trimAll().dropBlank().mapNotNull { tag ->
                val split = tag.split(":").filterNot { it.isBlank() }
                if (split.size > 1) {
                    val namespace = split[0].removePrefix("-").removePrefix("~")
                    val exclude = split[0].startsWith("-")
                    val or = split[0].startsWith("~")

                    AdvSearchEntry(namespace to split[1], exclude, or)
                } else if (split.size == 1) {
                    val item = split.first()
                    val exclude = item.startsWith("-")
                    val or = item.startsWith("~")
                    AdvSearchEntry(null to item, exclude, or)
                } else {
                    null
                }
            }
        }

        advSearch.forEach { entry ->
            if (entry.exclude) stringBuilder.append("-")
            if (entry.or) stringBuilder.append("~")
            val namespace = entry.search.first?.let { "$it:" }.orEmpty()
            if (entry.search.second.contains(" ")) {
                stringBuilder.append("""$namespace"${entry.search.second}$"""")
            } else {
                stringBuilder.append("$namespace${entry.search.second}$")
            }
            stringBuilder.append(" ")
        }

        return stringBuilder.toString().trim().also { xLogD(it) }
    }
}

internal data class AdvSearchEntry(val search: Pair<String?, String>, val exclude: Boolean, val or: Boolean)

internal class AutoCompleteTags :
    Filter.AutoComplete(
        name = "Tags",
        hint = "Search tags here (limit of 8)",
        values = EHTags.getNamespaces().map { "$it:" } + EHTags.getAllTags(),
        skipAutoFillTags = EHTags.getNamespaces().map { "$it:" },
        validPrefixes = listOf("-", "~"),
        state = emptyList(),
    )

internal class MinPagesOption : PageOption("Minimum Pages", "f_spf")
internal class MaxPagesOption : PageOption("Maximum Pages", "f_spt")

internal class RatingOption :
    Filter.Select<String>(
        "Minimum Rating",
        arrayOf(
            "Any",
            "2 stars",
            "3 stars",
            "4 stars",
            "5 stars",
        ),
    ),
    UriFilter {
    override fun addToUri(builder: Uri.Builder) {
        if (state > 0) {
            builder.appendQueryParameter("f_srdd", (state + 1).toString())
            builder.appendQueryParameter("f_sr", "on")
        }
    }
}

internal class AdvancedGroup : UriGroup<Filter<*>>(
    "Advanced Options",
    listOf(
        AdvancedOption("Browse Expunged Galleries", "f_sh"),
        AdvancedOption("Require Gallery Torrent", "f_sto"),
        RatingOption(),
        MinPagesOption(),
        MaxPagesOption(),
        AdvancedOption("Disable custom Language filters", "f_sfl"),
        AdvancedOption("Disable custom Uploader filters", "f_sfu"),
        AdvancedOption("Disable custom Tag filters", "f_sft"),
    ),
)

internal class ReverseFilter : Filter.CheckBox("Reverse search results")

internal class JumpSeekFilter : Filter.Text("Jump/Seek")

internal fun EHentai.filterList(): FilterList {
    return FilterList(
        Filter.Header("Note: Will ignore other parameters!"),
        ToplistOptions(),
        Filter.Separator(),
        AutoCompleteTags(),
        Watched(isEnabled = exhPreferences.exhWatchedListDefaultState.get()),
        GenreGroup(),
        AdvancedGroup(),
        ReverseFilter(),
        JumpSeekFilter(),
    )
}
