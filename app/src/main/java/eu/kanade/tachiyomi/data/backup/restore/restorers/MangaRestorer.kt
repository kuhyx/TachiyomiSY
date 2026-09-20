package eu.kanade.tachiyomi.data.backup.restore.restorers

import app.cash.sqldelight.async.coroutines.awaitAsList
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import exh.EXHMigrations
import tachiyomi.data.Database
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetMangaByUrlAndSourceId
import tachiyomi.domain.manga.interactor.InsertFlatMetadata
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZonedDateTime

internal class MangaRestorer(
    internal var isSync: Boolean = false,

    internal val database: Database = Injekt.get(),
    internal val getCategories: GetCategories = Injekt.get(),
    internal val getMangaByUrlAndSourceId: GetMangaByUrlAndSourceId = Injekt.get(),
    internal val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    internal val updateManga: UpdateManga = Injekt.get(),
    internal val getTracks: GetTracks = Injekt.get(),
    internal val insertTrack: InsertTrack = Injekt.get(),
    fetchInterval: FetchInterval = Injekt.get(),
    // SY -->
    internal val setCustomMangaInfo: SetCustomMangaInfo = Injekt.get(),
    internal val insertFlatMetadata: InsertFlatMetadata = Injekt.get(),
    internal val getFlatMetadataById: GetFlatMetadataById = Injekt.get(),
    // SY <--
) {
    internal var now = ZonedDateTime.now()
    internal var currentFetchWindow = fetchInterval.getWindow(now)

    init {
        now = ZonedDateTime.now()
        currentFetchWindow = fetchInterval.getWindow(now)
    }

    suspend fun sortByNew(backupMangas: List<BackupManga>): List<BackupManga> {
        val urlsBySource = database.mangasQueries
            .getAllMangaSourceAndUrl()
            .awaitAsList()
            .groupBy({ it.source }, { it.url })

        return backupMangas
            .sortedWith(
                compareBy<BackupManga> { it.url in urlsBySource[it.source].orEmpty() }
                    .then(compareByDescending { it.lastModifiedAt }),
            )
    }

    suspend fun restore(
        backupManga: BackupManga,
        backupCategories: List<BackupCategory>,
    ) {
        database.transaction {
            val dbManga = findExistingManga(backupManga)
            var manga = backupManga.getMangaImpl()
            // SY -->
            manga = EXHMigrations.migrateBackupEntry(manga)
            // SY <--
            val restoredManga = if (dbManga == null) {
                restoreNewManga(manga)
            } else {
                restoreExistingManga(manga, dbManga)
            }

            restoreMangaDetails(restoredManga, backupManga, backupCategories)

            if (isSync) {
                database.mangasQueries.resetIsSyncing()
                database.chaptersQueries.resetIsSyncing()
            }
        }
    }

    internal fun Manga.copyFrom(newer: Manga): Manga {
        return this.copy(
            favorite = this.favorite || newer.favorite,
            // SY -->
            ogTitle = newer.ogTitle,
            ogAuthor = newer.ogAuthor,
            ogArtist = newer.ogArtist,
            ogDescription = newer.ogDescription,
            ogGenre = newer.ogGenre,
            ogThumbnailUrl = newer.ogThumbnailUrl,
            ogStatus = newer.ogStatus,
            // SY <--
            chapterFlags = newer.chapterFlags,
            viewerFlags = newer.viewerFlags,
            updateStrategy = newer.updateStrategy,
            initialized = this.initialized || newer.initialized,
            version = newer.version,
        )
    }

    internal fun Chapter.forComparison() =
        this.copy(id = 0L, mangaId = 0L, dateFetch = 0L, dateUpload = 0L, lastModifiedAt = 0L, version = 0L)

    // SY -->

    fun BackupManga.getCustomMangaInfo(): CustomMangaInfo? {
        val customTexts = listOf(
            customTitle,
            customArtist,
            customAuthor,
            customThumbnailUrl,
            customDescription,
            customGenre,
        )
        if (customTexts.any { it != null } || customStatus != 0) {
            return CustomMangaInfo(
                id = 0L,
                title = customTitle,
                author = customAuthor,
                artist = customArtist,
                thumbnailUrl = customThumbnailUrl,
                description = customDescription,
                genre = customGenre,
                status = customStatus.takeUnless { it == 0 }?.toLong(),
            )
        }
        return null
    }
    // SY <--

    internal fun Track.forComparison() = this.copy(id = 0L, mangaId = 0L)
}
