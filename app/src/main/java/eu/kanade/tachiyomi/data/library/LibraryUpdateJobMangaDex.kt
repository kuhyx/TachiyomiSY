@file:OptIn(ExperimentalAtomicApi::class)

package eu.kanade.tachiyomi.data.library

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.online.all.MangaDex
import exh.md.utils.FollowStatus
import exh.md.utils.MdUtil
import exh.md.utils.getEnabledMangaDex
import exh.source.mangaDexSourceIds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.concurrent.atomics.ExperimentalAtomicApi

// filter all follows from Mangadex and only add reading or rereading manga to library.
internal suspend fun LibraryUpdateJob.syncFollows() {
    val preferences = Injekt.get<SourcePreferences>()
    val mangaDex = MdUtil.getEnabledMangaDex(preferences, sourceManager = sourceManager) ?: return
    syncFollows(mangaDex, preferences)
}

internal suspend fun LibraryUpdateJob.syncFollows(mangaDex: MangaDex, preferences: SourcePreferences) = coroutineScope {
    var count = 0
    val syncFollowStatusInts = preferences.mangadexSyncToLibraryIndexes.get().map { it.toInt() }

    val size: Int
    mangaDex.fetchAllFollows()
        .filter { (_, metadata) ->
            syncFollowStatusInts.contains(metadata.followStatus)
        }
        .also { size = it.size }
        .forEach { (networkManga, metadata) ->
            ensureActive()

            count++
            notifier.showProgressNotification(
                listOf(Manga.create().copy(ogTitle = networkManga.title)),
                count,
                size,
            )

            var dbManga = getManga.await(networkManga.url, mangaDex.id)

            if (dbManga == null) {
                dbManga = networkToLocalManga(
                    Manga.create().copy(
                        url = networkManga.url,
                        ogTitle = networkManga.title,
                        source = mangaDex.id,
                        favorite = true,
                        dateAdded = System.currentTimeMillis(),
                    ),
                )
            } else if (!dbManga.favorite) {
                updateManga.awaitUpdateFavorite(dbManga.id, true)
            }

            updateMangaFromRemote(
                dbManga,
                fetchDetails = false,
                fetchChapters = false,
            )

            metadata.mangaId = dbManga.id
            insertFlatMetadata.await(metadata)
        }

    notifier.cancelProgressNotification()
}

// Method that updates the all mangas which are not tracked as "reading" on mangadex.
internal suspend fun LibraryUpdateJob.pushFavorites() = coroutineScope {
    var count = 0
    val listManga = getFavorites.await().filter { it.source in mangaDexSourceIds }

    // filter all follows from Mangadex and only add reading or rereading manga to library
    if (mdList.isLoggedIn) {
        listManga.forEach { manga ->
            ensureActive()

            count++
            notifier.showProgressNotification(listOf(manga), count, listManga.size)

            // Get this manga's trackers from the database
            val dbTracks = getTracks.await(manga.id)

            // find the mdlist entry if its unfollowed the follow it
            var tracker = dbTracks.firstOrNull { it.trackerId == TrackerManager.MDLIST }
                ?: mdList.createInitialTracker(manga).toDomainTrack(idRequired = false)

            if (tracker?.status == FollowStatus.UNFOLLOWED.long) {
                tracker = tracker.copy(
                    status = FollowStatus.READING.long,
                )
                val updatedTrack = mdList.update(tracker.toDbTrack())
                insertTrack.await(updatedTrack.toDomainTrack(false)!!)
            }
        }
    }

    notifier.cancelProgressNotification()
}
