package mihon.core.migration.migrations

import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.source.Source
import exh.source.MERGED_SOURCE_ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.data.Database
import tachiyomi.data.awaitList
import tachiyomi.data.chapter.ChapterMapper
import tachiyomi.domain.chapter.interactor.DeleteChapters
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMangaBySource
import tachiyomi.domain.manga.interactor.InsertMergedReference
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.source.service.SourceManager

private const val VERSION = 7f

internal class MergedMangaRewriteMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val collaborators = Collaborators.from(migrationContext) ?: return false
        withIOContext { collaborators.migrate() }
        return true
    }

    private suspend fun Collaborators.migrate() {
        val mergedMangas = getMangaBySource.await(MERGED_SOURCE_ID)
        val mangaConfigs = mergedMangas.mapNotNull { mergedManga ->
            readMangaConfig(mergedManga)?.let { mergedManga to it }
        }
        if (mangaConfigs.isEmpty()) return

        rewriteReferences(mangaConfigs)

        val loadedMangaList = mangaConfigs
            .map { it.second.children }
            .flatten()
            .mapNotNull { it.load(getManga, sourceManager) }
            .distinct()
        val chapters = database.ehQueries
            .getChaptersByMangaIds(mergedMangas.map { it.id })
            .awaitList(ChapterMapper::mapChapter)
        val mergedMangaChapters = database.ehQueries
            .getChaptersByMangaIds(loadedMangaList.map { it.manga.id })
            .awaitList(ChapterMapper::mapChapter)

        deleteChapters.await(mergedMangaChapters.map { it.id })
        updateChapter.awaitAll(carriedReadProgress(chapters, mergedMangaChapters, loadedMangaList))
    }

    // Each merged entry becomes a MergedMangaReference row per child, and the merged manga itself
    // moves onto its first child's url when that url is not taken yet.
    private suspend fun Collaborators.rewriteReferences(mangaConfigs: List<Pair<Manga, MangaConfig>>) {
        val mangaToUpdate = mutableListOf<MangaUpdate>()
        val mergedMangaReferences = mutableListOf<MergedMangaReference>()
        for ((mergedManga, config) in mangaConfigs) {
            val firstUrl = config.children.firstOrNull()?.url
            val newFirst = if (firstUrl == null) {
                mergedManga
            } else {
                if (getManga.await(firstUrl, MERGED_SOURCE_ID) != null) continue
                mangaToUpdate += MangaUpdate(id = mergedManga.id, url = firstUrl)
                mergedManga.copy(url = firstUrl)
            }
            mergedMangaReferences += MergedMangaReference(
                id = -1,
                isInfoManga = false,
                getChapterUpdates = false,
                chapterSortMode = 0,
                chapterPriority = 0,
                downloadChapters = false,
                mergeId = newFirst.id,
                mergeUrl = newFirst.url,
                mangaId = newFirst.id,
                mangaUrl = newFirst.url,
                mangaSourceId = MERGED_SOURCE_ID,
            )
            config.children.distinct().forEachIndexed { index, mangaSource ->
                val load = mangaSource.load(getManga, sourceManager)
                if (load != null) {
                    mergedMangaReferences += MergedMangaReference(
                        id = -1,
                        isInfoManga = index == 0,
                        getChapterUpdates = true,
                        chapterSortMode = 0,
                        chapterPriority = 0,
                        downloadChapters = true,
                        mergeId = newFirst.id,
                        mergeUrl = newFirst.url,
                        mangaId = load.manga.id,
                        mangaUrl = load.manga.url,
                        mangaSourceId = load.source.id,
                    )
                }
            }
        }

        updateManga.awaitAll(mangaToUpdate)
        insertMergedReference.awaitAll(mergedMangaReferences)
    }

    // Read state of the old merged chapters, re-keyed onto the matching chapter of the real source.
    private fun carriedReadProgress(
        chapters: List<Chapter>,
        mergedMangaChapters: List<Chapter>,
        loadedMangaList: List<LoadedMangaSource>,
    ): List<ChapterUpdate> {
        val mergedMangaChaptersMatched = mergedMangaChapters.mapNotNull { chapter ->
            loadedMangaList.firstOrNull { it.manga.id == chapter.id }?.let { it to chapter }
        }
        val parsedChapters = chapters
            .filter { it.read || it.lastPageRead != 0L }
            .mapNotNull { chapter -> readUrlConfig(chapter.url)?.let { chapter to it } }
        return parsedChapters.mapNotNull { (chapter, urlConfig) ->
            mergedMangaChaptersMatched.firstOrNull { (loaded, mergedChapter) ->
                mergedChapter.url == urlConfig.url &&
                    loaded.source.id == urlConfig.source &&
                    loaded.manga.url == urlConfig.mangaUrl
            }?.let { (_, mergedChapter) ->
                ChapterUpdate(mergedChapter.id, read = chapter.read, lastPageRead = chapter.lastPageRead)
            }
        }
    }

    // Everything the rewrite touches; absent when any of them is not registered yet.
    private data class Collaborators(
        val database: Database,
        val getMangaBySource: GetMangaBySource,
        val getManga: GetManga,
        val updateManga: UpdateManga,
        val insertMergedReference: InsertMergedReference,
        val sourceManager: SourceManager,
        val deleteChapters: DeleteChapters,
        val updateChapter: UpdateChapter,
    ) {
        companion object {
            fun from(migrationContext: MigrationContext): Collaborators? = runCatching {
                Collaborators(
                    database = migrationContext.require(),
                    getMangaBySource = migrationContext.require(),
                    getManga = migrationContext.require(),
                    updateManga = migrationContext.require(),
                    insertMergedReference = migrationContext.require(),
                    sourceManager = migrationContext.require(),
                    deleteChapters = migrationContext.require(),
                    updateChapter = migrationContext.require(),
                )
            }.getOrNull()
        }
    }

    @Serializable
    private data class UrlConfig(
        @SerialName("s")
        val source: Long,
        @SerialName("u")
        val url: String,
        @SerialName("m")
        val mangaUrl: String,
    )

    @Serializable
    private data class MangaConfig(
        @SerialName("c")
        val children: List<MangaSource>,
    ) {
        companion object {
            fun readFromUrl(url: String): MangaConfig? {
                return try {
                    Json.decodeFromString(url)
                } catch (_: Exception) {
                    // Any failure ends here and the fallback below applies.
                    null
                }
            }
        }
    }

    private fun readMangaConfig(manga: Manga): MangaConfig? = MangaConfig.readFromUrl(manga.url)

    @Serializable
    private data class MangaSource(
        @SerialName("s")
        val source: Long,
        @SerialName("u")
        val url: String,
    ) {
        suspend fun load(getManga: GetManga, sourceManager: SourceManager): LoadedMangaSource? {
            val manga = getManga.await(url, source) ?: return null
            val source = sourceManager.getOrStub(source)
            return LoadedMangaSource(source, manga)
        }
    }

    private fun readUrlConfig(url: String): UrlConfig? {
        return try {
            Json.decodeFromString(url)
        } catch (_: Exception) {
            // Any failure ends here and the fallback below applies.
            null
        }
    }

    private data class LoadedMangaSource(val source: Source, val manga: Manga)
}
