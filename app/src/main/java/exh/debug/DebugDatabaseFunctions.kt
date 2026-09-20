package exh.debug

import kotlinx.coroutines.runBlocking
import tachiyomi.data.Database
import tachiyomi.domain.manga.interactor.GetAllManga
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetSearchMetadata
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/** Database counts and bulk edits. Listed in the debug menu through [DebugFunctions]. */
@Suppress("unused")
internal object DebugDatabaseFunctions {
    private val database: Database by injectLazy()
    private val getFavorites: GetFavorites by injectLazy()
    private val getSearchMetadata: GetSearchMetadata by injectLazy()
    private val getAllManga: GetAllManga by injectLazy()

    fun addAllMangaInDatabaseToLibrary() {
        runBlocking { database.ehQueries.addAllMangaInDatabaseToLibrary() }
    }

    fun countMangaInDatabaseInLibrary() = runBlocking { getFavorites.await().size }

    fun countMangaNotInLibrary() = runBlocking { getAllManga.await() }.count { !it.favorite }

    fun countMangaInDatabase() = runBlocking { getAllManga.await() }.size

    fun countMetadataInDatabase() = runBlocking { getSearchMetadata.await().size }

    fun countLibraryMissingMetadata() = runBlocking {
        getAllManga.await().count {
            it.favorite && getSearchMetadata.await(it.id) == null
        }
    }

    fun clearSavedSearches() = runBlocking { database.saved_searchQueries.deleteAll() }
}
