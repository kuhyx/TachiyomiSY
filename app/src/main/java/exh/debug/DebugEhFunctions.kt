package exh.debug

import android.app.Application
import exh.eh.EHentaiUpdateWorker
import exh.eh.launchBackgroundTest
import exh.eh.scheduleBackground
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.base.raise
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.util.ThrottleManager
import kotlinx.coroutines.runBlocking
import mihon.domain.source.interactor.UpdateMangaFromRemote
import tachiyomi.domain.manga.interactor.GetExhFavoriteMangaWithMetadata
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.InsertFlatMetadata
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/** E-Hentai / ExHentai library and updater probes. Listed in the debug menu through [DebugFunctions]. */
@Suppress("unused")
internal object DebugEhFunctions {
    private val app: Application by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val updateMangaFromRemote: UpdateMangaFromRemote by injectLazy()
    private val getFlatMetadataById: GetFlatMetadataById by injectLazy()
    private val insertFlatMetadata: InsertFlatMetadata by injectLazy()
    private val getExhFavoriteMangaWithMetadata: GetExhFavoriteMangaWithMetadata by injectLazy()
    private val throttleManager = ThrottleManager()

    fun resetAgedFlagInEXHManga() {
        runBlocking {
            getExhFavoriteMangaWithMetadata.await().forEach { manga ->
                val meta = getFlatMetadataById.await(manga.id)?.raise(EHentaiSearchMetadata::class)
                if (meta != null) {
                    // remove age flag
                    meta.aged = false
                    insertFlatMetadata.await(meta)
                }
            }
        }
    }

    fun resetEHGalleriesForUpdater() {
        throttleManager.resetThrottle()
        runBlocking {
            val allManga = getExhFavoriteMangaWithMetadata.await()

            val eh = sourceManager.getOrStub(EH_SOURCE_ID)
            val ex = sourceManager.getOrStub(EXH_SOURCE_ID)

            allManga.forEach { manga ->
                throttleManager.throttle()

                val source = when (manga.source) {
                    EH_SOURCE_ID -> eh
                    EXH_SOURCE_ID -> ex
                    else -> null
                }
                if (source != null) {
                    updateMangaFromRemote(source, manga, fetchDetails = true, fetchChapters = false)
                }
            }
        }
    }

    fun getEHMangaListWithAgedFlagInfo(): String {
        return runBlocking {
            getExhFavoriteMangaWithMetadata.await().mapNotNull { manga ->
                val meta = getFlatMetadataById.await(manga.id)?.raise(EHentaiSearchMetadata::class)
                meta?.let { "Aged: ${it.aged}\t Title: ${manga.title}" }
            }
        }.joinToString(",\n")
    }

    fun countAgedFlagInEXHManga(): Int {
        return runBlocking {
            getExhFavoriteMangaWithMetadata.await()
                .count { manga ->
                    getFlatMetadataById.await(manga.id)?.raise(EHentaiSearchMetadata::class)?.aged == true
                }
        }
    }

    fun testLaunchEhUpdater() {
        EHentaiUpdateWorker.launchBackgroundTest(app)
    }

    fun rescheduleEhUpdater() {
        EHentaiUpdateWorker.scheduleBackground(app)
    }
}
