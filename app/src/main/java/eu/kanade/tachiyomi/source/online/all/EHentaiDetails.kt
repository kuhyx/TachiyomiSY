package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.model.copy
import eu.kanade.tachiyomi.source.online.all.EHentai.GalleryNotFoundException
import eu.kanade.tachiyomi.util.asJsoup
import exh.debug.DebugToggles
import exh.metadata.metadata.EHentaiSearchMetadata
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import java.io.IOException
import java.net.HttpURLConnection

internal suspend fun EHentai.getMangaUpdate(
    manga: SManga,
    chapters: List<SChapter>,
    fetchDetails: Boolean,
    fetchChapters: Boolean,
    throttleFunc: suspend () -> Unit,
): SMangaUpdate = supervisorScope {
    val mangaDetails = if (fetchDetails) async { getMangaDetails(manga) } else null
    val chapterDetails = if (fetchChapters) async { getChapterList(manga, throttleFunc) } else null

    SMangaUpdate(mangaDetails?.await() ?: manga, chapterDetails?.await() ?: chapters)
}

internal suspend fun EHentai.getMangaDetails(manga: SManga): SManga {
    val exception = Exception("Async stacktrace")
    val response = client.newCall(galleryRequest(manga)).await()
    if (response.isSuccessful) {
        // Pull to most recent
        val doc = response.asJsoup()
        val newerGallery = doc.select("#gnd a").lastOrNull()
        val pre = if (
            newerGallery != null && DebugToggles.PULL_TO_ROOT_WHEN_LOADING_EXH_MANGA_DETAILS.enabled
        ) {
            val sManga = manga.copy(
                url = EHentaiSearchMetadata.normalizeUrl(newerGallery.attr("href")),
            )
            client.newCall(galleryRequest(sManga)).awaitSuccess().asJsoup()
        } else {
            doc
        }
        return parseToManga(manga, pre).apply {
            initialized = true
        }
    } else {
        response.close()

        if (response.code == HttpURLConnection.HTTP_NOT_FOUND) {
            throw GalleryNotFoundException(exception)
        } else {
            throw IOException("HTTP error ${response.code}", exception)
        }
    }
}
