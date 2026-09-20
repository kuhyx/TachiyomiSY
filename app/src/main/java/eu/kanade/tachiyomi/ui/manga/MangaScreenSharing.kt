package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.search
import eu.kanade.tachiyomi.ui.browse.source.browse.searchGenre
import eu.kanade.tachiyomi.ui.browse.source.feed.SourceFeedScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

internal fun getMangaUrl(manga: Manga?, source: Source?): String? {
    val httpSource = source as? HttpSource
    if (manga == null || httpSource == null) return null

    return try {
        httpSource.getMangaUrl(manga.toSManga())
    } catch (expected: Exception) {
        // A source may reject the manga; the caller treats "no URL" as the outcome.
        null
    }
}

internal fun shareManga(context: Context, manga: Manga?, source: Source?) {
    try {
        getMangaUrl(manga, source)?.let { url ->
            val intent = url.toUri().toShareIntent(context, type = "text/plain")
            context.startActivity(intent)
        }
    } catch (expected: Exception) {
        // Any failure ends here and the fallback below applies.
        context.toast(expected.message)
    }
}

// Perform a search using the provided query.
// @param query the search query to the parent controller
internal suspend fun performSearch(navigator: Navigator, query: String, global: Boolean) {
    if (global) {
        navigator.push(GlobalSearchScreen(query))
        return
    }

    if (navigator.size < 2) {
        return
    }

    when (val previousController = navigator.items[navigator.size - 2]) {
        is HomeScreen -> {
            navigator.pop()
            previousController.search(query)
        }
        is BrowseSourceScreen -> {
            navigator.pop()
            previousController.search(query)
        }
        // SY -->
        is SourceFeedScreen -> {
            navigator.pop()
            navigator.replace(BrowseSourceScreen(previousController.sourceId, query))
        }
        // SY <--
    }
}

// Performs a genre search using the provided genre name.
// @param genreName the search genre to the parent controller
internal suspend fun performGenreSearch(navigator: Navigator, genreName: String, source: Source) {
    if (navigator.size < 2) {
        return
    }

    val previousController = navigator.items[navigator.size - 2]
    if (previousController is BrowseSourceScreen && source is HttpSource) {
        navigator.pop()
        previousController.searchGenre(genreName)
    } else {
        performSearch(navigator, genreName, global = false)
    }
}

// Copy Manga URL to Clipboard.
internal fun copyMangaUrl(context: Context, manga: Manga?, source: Source?) {
    val httpSource = source as? HttpSource
    if (manga == null || httpSource == null) return
    val url = httpSource.getMangaUrl(manga.toSManga())
    context.copyToClipboard(url, url)
}
