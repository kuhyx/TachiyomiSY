package exh

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.online.UrlImportableSource
import eu.kanade.tachiyomi.source.online.all.EHentai
import exh.log.xLogStack
import exh.source.getMainSource
import mihon.domain.source.interactor.UpdateMangaFromRemote
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class GalleryAdder(
    private val getManga: GetManga = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val updateMangaFromRemote: UpdateMangaFromRemote = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val getChapter: GetChapter = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) {

    private val filters: Pair<Set<String>, Set<Long>> = Injekt.get<SourcePreferences>().run {
        enabledLanguages.get() to disabledSources.get().map { it.toLong() }.toSet()
    }

    private val Pair<Set<String>, Set<Long>>.enabledLangs
        get() = first
    private val Pair<Set<String>, Set<Long>>.disabledSources
        get() = second

    private val logger = xLogStack()

    fun pickSource(url: String): List<UrlImportableSource> {
        val uri = url.toUri()
        return sourceManager.getVisibleSources()
            .mapNotNull { it.getMainSource<UrlImportableSource>() }
            .filter {
                it.lang in filters.enabledLangs &&
                    it.id !in filters.disabledSources &&
                    try {
                        it.matchesUri(uri)
                    } catch (_: Exception) {
                        // Any failure ends here and the fallback below applies.
                        false
                    }
            }
    }

    suspend fun addGallery(
        context: Context,
        url: String,
        fav: Boolean = false,
        forceSource: UrlImportableSource? = null,
        throttleFunc: suspend () -> Unit = {},
        retry: Int = 1,
    ): GalleryAddEvent {
        logger.d(
            context.stringResource(
                SYMR.strings.gallery_adder_importing_gallery,
                url,
                fav.toString(),
                forceSource?.toString().orEmpty(),
            ),
        )
        return try {
            val uri = url.toUri()
            when (val match = matchSource(uri, url, forceSource, context)) {
                is SourceMatch.Failed -> match.event
                is SourceMatch.Found -> importFrom(match.source, uri, url, fav, throttleFunc, retry, context)
            }
        } catch (notFound: EHentai.GalleryNotFoundException) {
            logger.w(context.stringResource(SYMR.strings.gallery_adder_could_not_add_gallery, url), notFound)
            GalleryAddEvent.Fail.NotFound(url, context)
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logger.w(context.stringResource(SYMR.strings.gallery_adder_could_not_add_gallery, url), expected)
            GalleryAddEvent.Fail.Error(url, ((expected.message ?: "Unknown error!") + " (Gallery: $url)").trim())
        }
    }

    private suspend fun importFrom(
        source: UrlImportableSource,
        uri: Uri,
        url: String,
        fav: Boolean,
        throttleFunc: suspend () -> Unit,
        retry: Int,
        context: Context,
    ): GalleryAddEvent {
        val urls = resolveUrls(source, uri, context) ?: return GalleryAddEvent.Fail.UnknownType(url, context)
        val manga = importManga(source, urls.mangaUrl, fav, throttleFunc, retry)
        return successEvent(url, manga, urls.chapterUrl, context)
    }

    private sealed interface SourceMatch {
        class Found(val source: UrlImportableSource) : SourceMatch
        class Failed(val event: GalleryAddEvent.Fail) : SourceMatch
    }

    // The forced source if it claims [uri], otherwise the first enabled importable source that does.
    private fun matchSource(uri: Uri, url: String, forceSource: UrlImportableSource?, context: Context): SourceMatch {
        if (forceSource != null) {
            return try {
                if (forceSource.matchesUri(uri)) {
                    SourceMatch.Found(forceSource)
                } else {
                    SourceMatch.Failed(GalleryAddEvent.Fail.UnknownSource(url, context))
                }
            } catch (expected: Exception) {
                // Logged whatever the cause; the caller carries on.
                logger.e(context.stringResource(SYMR.strings.gallery_adder_source_uri_must_match), expected)
                SourceMatch.Failed(GalleryAddEvent.Fail.UnknownType(url, context))
            }
        }
        val source = sourceManager.getVisibleSources()
            .mapNotNull { it.getMainSource<UrlImportableSource>() }
            .find {
                it.lang in filters.enabledLangs &&
                    it.id !in filters.disabledSources &&
                    try {
                        it.matchesUri(uri)
                    } catch (_: Exception) {
                        // Any failure ends here and the fallback below applies.
                        false
                    }
            }
        return source?.let(SourceMatch::Found) ?: SourceMatch.Failed(GalleryAddEvent.Fail.UnknownSource(url, context))
    }

    private data class ResolvedUrls(val mangaUrl: String, val chapterUrl: String?)

    // The cleaned manga url the link points at (via its chapter, when it is a chapter link), or null when
    // unmappable.
    private suspend fun resolveUrls(source: UrlImportableSource, uri: Uri, context: Context): ResolvedUrls? {
        val realChapterUrl = logged(context, SYMR.strings.gallery_adder_uri_map_to_chapter_error) {
            source.mapUrlToChapterUrl(uri)
        }
        val cleanedChapterUrl = realChapterUrl?.let { chapterUrl ->
            logged(context, SYMR.strings.gallery_adder_uri_clean_error) { source.cleanChapterUrl(chapterUrl) }
        }
        val chapterMangaUrl = realChapterUrl?.let { source.mapChapterUrlToMangaUrl(it.toUri()) }
        // Map URL to manga URL
        val realMangaUrl = logged(context, SYMR.strings.gallery_adder_uri_map_to_gallery_error) {
            chapterMangaUrl ?: source.mapUrlToMangaUrl(uri)
        }
        // Clean URL
        val cleanedMangaUrl = realMangaUrl?.let { mangaUrl ->
            logged(context, SYMR.strings.gallery_adder_uri_clean_error) { source.cleanMangaUrl(mangaUrl) }
        }
        return cleanedMangaUrl?.let { ResolvedUrls(it, cleanedChapterUrl) }
    }

    // Runs [block], logging any failure under [message] and yielding null in its place.
    private inline fun <T> logged(context: Context, message: StringResource, block: () -> T?): T? {
        return try {
            block()
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logger.e(context.stringResource(message), expected)
            null
        }
    }

    // Use manga in DB if possible, otherwise make a new one; then fetch details and chapters.
    private suspend fun importManga(
        source: UrlImportableSource,
        mangaUrl: String,
        fav: Boolean,
        throttleFunc: suspend () -> Unit,
        retryCount: Int,
    ): Manga {
        var manga = getManga.await(mangaUrl, source.id)
            ?: networkToLocalManga(Manga.create().copy(source = source.id, url = mangaUrl))
        // Fetch and copy details
        manga = retry(retryCount) {
            updateMangaFromRemote(
                manga,
                fetchDetails = true,
                fetchChapters = true,
                manualFetch = false,
                throttleFunc = throttleFunc,
            ).getOrThrow().manga
        }
        if (fav) {
            updateManga.awaitUpdateFavorite(manga.id, true)
            manga = manga.copy(favorite = true)
        }
        return manga
    }

    private suspend fun successEvent(
        url: String,
        manga: Manga,
        chapterUrl: String?,
        context: Context,
    ): GalleryAddEvent {
        if (chapterUrl == null) return GalleryAddEvent.Success(url, manga, context)
        val chapter = getChapter.await(chapterUrl, manga.id)
        return if (chapter != null) {
            GalleryAddEvent.Success(url, manga, context, chapter)
        } else {
            GalleryAddEvent.Fail.Error(
                url,
                context.stringResource(SYMR.strings.gallery_adder_could_not_identify_chapter, url),
            )
        }
    }

    private inline fun <T : Any> retry(retryCount: Int, block: () -> T): T {
        var result: T? = null
        var lastError: Exception? = null

        repeat(retryCount) {
            if (result == null) {
                try {
                    result = block()
                } catch (notFound: EHentai.GalleryNotFoundException) {
                    throw notFound
                } catch (expected: Exception) {
                    // Remembered for the last attempt; a later success discards it.
                    lastError = expected
                }
            }
        }

        if (lastError != null) {
            throw lastError
        }

        return result!!
    }
}

internal sealed class GalleryAddEvent {
    abstract val logMessage: String
    abstract val galleryUrl: String
    open val galleryTitle: String? = null

    class Success(
        override val galleryUrl: String,
        val manga: Manga,
        val context: Context,
        val chapter: Chapter? = null,
    ) : GalleryAddEvent() {
        override val galleryTitle = manga.title
        override val logMessage = context.stringResource(SYMR.strings.batch_add_success_log_message, galleryTitle)
    }

    sealed class Fail : GalleryAddEvent() {
        class UnknownType(override val galleryUrl: String, val context: Context) : Fail() {
            override val logMessage =
                context.stringResource(SYMR.strings.batch_add_unknown_type_log_message, galleryUrl)
        }

        open class Error(
            override val galleryUrl: String,
            override val logMessage: String,
        ) : Fail()

        class NotFound(galleryUrl: String, context: Context) :
            Error(galleryUrl, context.stringResource(SYMR.strings.batch_add_not_exist_log_message, galleryUrl))

        class UnknownSource(override val galleryUrl: String, val context: Context) : Fail() {
            override val logMessage =
                context.stringResource(SYMR.strings.batch_add_unknown_source_log_message, galleryUrl)
        }
    }
}
