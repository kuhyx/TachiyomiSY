package eu.kanade.tachiyomi.source.online.all

import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.model.copy
import exh.source.MERGED_SOURCE_ID
import exh.source.UnsupportedHelpersHttpSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import mihon.domain.source.interactor.UpdateMangaFromRemote
import okhttp3.Response
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.injectLazy

private const val MAX_CONCURRENT_SOURCES = 5

internal class MergedSource : UnsupportedHelpersHttpSource() {
    private val getManga: GetManga by injectLazy()
    private val getMergedReferencesById: GetMergedReferencesById by injectLazy()
    private val networkToLocalManga: NetworkToLocalManga by injectLazy()
    private val updateMangaFromRemote: UpdateMangaFromRemote by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val downloadManager: DownloadManager by injectLazy()
    private val filterChaptersForDownload: FilterChaptersForDownload by injectLazy()

    override val id: Long = MERGED_SOURCE_ID

    override val baseUrl = ""

    override val lang = "all"
    override val supportsLatest = false
    override val name = "MergedSource"

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getChapterList"))
    override fun fetchChapterList(manga: SManga) = throw UnsupportedOperationException()
    override suspend fun getImage(page: Page, existingSize: Long): Response = throw UnsupportedOperationException()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(page: Page) = throw UnsupportedOperationException()
    override suspend fun getImageUrl(page: Page) = throw UnsupportedOperationException()

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getPageList"))
    override fun fetchPageList(chapter: SChapter) = throw UnsupportedOperationException()
    override suspend fun getPageList(chapter: SChapter) = throw UnsupportedOperationException()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int) = throw UnsupportedOperationException()
    override suspend fun getLatestUpdates(page: Int) = throw UnsupportedOperationException()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getPopularManga"))
    override fun fetchPopularManga(page: Int) = throw UnsupportedOperationException()
    override suspend fun getPopularManga(page: Int) = throw UnsupportedOperationException()

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        return SMangaUpdate(
            getMangaDetails(manga),
            emptyList(),
        )
    }

    suspend fun getMangaDetails(manga: SManga): SManga {
        return withIOContext {
            val mergedManga = requireNotNull(getManga.await(manga.url, id)) { "merged manga not in db" }
            val mangaReferences = getMergedReferencesById.await(mergedManga.id)
                .apply {
                    require(isNotEmpty()) { "Manga references are empty, info unavailable, merge is likely corrupted" }
                    require(!(size == 1 && first().mangaSourceId == MERGED_SOURCE_ID)) {
                        "Manga references contain only the merged reference, merge is likely corrupted"
                    }
                }

            val mangaInfoReference = mangaReferences.firstOrNull { it.isInfoManga }
                ?: mangaReferences.firstOrNull { it.mangaId != it.mergeId }
            val dbManga = mangaInfoReference?.run {
                getManga.await(mangaUrl, mangaSourceId)?.toSManga()
            }
            (dbManga ?: mergedManga.toSManga()).copy(
                url = manga.url,
            )
        }
    }

    suspend fun fetchChaptersForMergedManga(
        manga: Manga,
        downloadChapters: Boolean = true,
    ) {
        fetchChaptersAndSync(manga, downloadChapters)
    }

    suspend fun fetchChaptersAndSync(manga: Manga, downloadChapters: Boolean = true): List<Chapter> {
        val mangaReferences = getMergedReferencesById.await(manga.id)
        require(mangaReferences.isNotEmpty()) {
            "Manga references are empty, chapters unavailable, merge is likely corrupted"
        }

        val semaphore = Semaphore(MAX_CONCURRENT_SOURCES)
        var exception: Exception? = null
        return supervisorScope {
            mangaReferences
                .groupBy(MergedMangaReference::mangaSourceId)
                .minus(MERGED_SOURCE_ID)
                .map { (_, values) ->
                    async {
                        semaphore.withPermit {
                            fetchParts(manga, values, downloadChapters) { exception = it }
                        }
                    }
                }
                .awaitAll()
                .flatten()
        }.also {
            exception?.let { throw it }
        }
    }

    // A failing part reports its error and contributes nothing; the others still load.
    private suspend fun fetchParts(
        manga: Manga,
        references: List<MergedMangaReference>,
        downloadChapters: Boolean,
        onError: (Exception) -> Unit,
    ): List<Chapter> = references.flatMap { reference ->
        try {
            fetchPartChapters(manga, reference, downloadChapters)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (expected: Exception) {
            // Rethrown (or wrapped) whatever the cause.
            onError(expected)
            emptyList()
        }
    }

    // One part's new chapters, queued for download when its reference asks; none when it is not followed.
    private suspend fun fetchPartChapters(
        manga: Manga,
        reference: MergedMangaReference,
        downloadChapters: Boolean,
    ): List<Chapter> {
        val (source, loadedManga, loaded) = reference.load()
        if (loadedManga == null || !loaded.getChapterUpdates) return emptyList()

        val results = updateMangaFromRemote(
            source,
            loadedManga,
            fetchDetails = false,
            fetchChapters = true,
        ).getOrThrow().newChapters

        if (downloadChapters && loaded.downloadChapters) {
            val chaptersToDownload = filterChaptersForDownload.await(manga, results)
            if (chaptersToDownload.isNotEmpty()) {
                downloadManager.downloadChapters(loadedManga, chaptersToDownload)
            }
        }
        return results
    }

    suspend fun MergedMangaReference.load(): LoadedMangaSource {
        var manga = getManga.await(mangaUrl, mangaSourceId)
        val source = sourceManager.getOrStub(manga?.source ?: mangaSourceId)
        if (manga == null) {
            val newManga = networkToLocalManga(
                Manga.create().copy(
                    source = mangaSourceId,
                    url = mangaUrl,
                ),
            )
            manga = updateMangaFromRemote(
                source,
                newManga,
                fetchDetails = true,
                fetchChapters = false,
            ).getOrThrow().manga
        }
        return LoadedMangaSource(source, manga, this)
    }

    data class LoadedMangaSource(val source: Source, val manga: Manga?, val reference: MergedMangaReference)
}
