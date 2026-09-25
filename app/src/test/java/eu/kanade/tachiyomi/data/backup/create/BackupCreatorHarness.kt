package eu.kanade.tachiyomi.data.backup.create

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.tachiyomi.data.backup.create.creators.CategoriesBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.ExtensionStoresBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.MangaBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.PreferenceBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.SavedSearchBackupCreator
import eu.kanade.tachiyomi.data.backup.create.creators.SourcesBackupCreator
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.track.TrackerManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetMergedManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.source.service.SourceManager

/**
 * A [BackupCreator] over mocked section creators: the library is [library] (favourites, read
 * entries and merged entries), every other section is empty. The file validator the creator runs
 * afterwards gets its source and tracker managers from Koin.
 */
internal class BackupCreatorHarness {
    val context: Application = ApplicationProvider.getApplicationContext()
    val backupPreferences = BackupPreferences(FlowPreferenceStore())
    val favorite: Manga = Manga.create().copy(id = 1L, url = "/fav")
    val read: Manga = Manga.create().copy(id = 2L, url = "/read")
    val merged: Manga = Manga.create().copy(id = 3L, url = "/merged")
    val getFavorites = mockk<GetFavorites> { coEvery { await() } returns listOf(favorite) }
    val mangaRepository = mockk<MangaRepository> { coEvery { getReadMangaNotInLibrary() } returns listOf(read) }
    val getMergedManga = mockk<GetMergedManga> { coEvery { await() } returns listOf(merged) }
    val mangaCreator = mockk<MangaBackupCreator>()
    val preferenceCreator = mockk<PreferenceBackupCreator>()
    val categoriesCreator = mockk<CategoriesBackupCreator>()
    private val sourceManager = mockk<SourceManager>()
    private val storesCreator = mockk<ExtensionStoresBackupCreator>()
    private val sourcesCreator = mockk<SourcesBackupCreator>()
    private val searchesCreator = mockk<SavedSearchBackupCreator>()

    /** The manga the library section yields, whatever it is asked for. */
    var library: List<BackupManga> = listOf(BackupManga(source = 1L, url = "/fav"))

    fun start() {
        coEvery { mangaCreator(any(), any()) } answers { library }
        every { sourceManager.get(any<Long>()) } returns mockk()
        coEvery { categoriesCreator() } returns emptyList()
        coEvery { storesCreator() } returns emptyList()
        every { sourcesCreator(any()) } returns emptyList()
        coEvery { searchesCreator() } returns emptyList()
        every { preferenceCreator.createApp(any()) } returns emptyList()
        every { preferenceCreator.createSource(any()) } returns emptyList()
        startKoin {
            modules(
                module {
                    single<ProtoBuf> { ProtoBuf }
                    single { sourceManager }
                    single<TrackerManager> { mockk() }
                },
            )
        }
    }

    fun stop() = stopKoin()

    fun creator(isAutoBackup: Boolean): BackupCreator = BackupCreator(
        context = context,
        isAutoBackup = isAutoBackup,
        parser = ProtoBuf,
        getFavorites = getFavorites,
        backupPreferences = backupPreferences,
        mangaRepository = mangaRepository,
        categoriesBackupCreator = categoriesCreator,
        mangaBackupCreator = mangaCreator,
        preferenceBackupCreator = preferenceCreator,
        extensionStoresBackupCreator = storesCreator,
        sourcesBackupCreator = sourcesCreator,
        savedSearchBackupCreator = searchesCreator,
        getMergedManga = getMergedManga,
    )
}
