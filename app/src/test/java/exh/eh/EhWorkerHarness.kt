package exh.eh

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.ForegroundUpdater
import androidx.work.WorkerParameters
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.source.online.all.EHentai
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import exh.source.ExhPreferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import mihon.domain.source.interactor.UpdateMangaFromRemote
import mihon.domain.source.models.RemoteMangaUpdate
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetExhFavoriteMangaWithMetadata
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.InsertFlatMetadata
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.domain.source.service.SourceManager
import java.util.UUID

/** Stored metadata for a gallery, as the updater reads it back. */
internal fun ehMetadata(mangaId: Long, aged: Boolean = false, lastUpdateCheck: Long = 0): FlatMetadata =
    EHentaiSearchMetadata().apply {
        this.mangaId = mangaId
        gId = "$mangaId"
        gToken = "tok$mangaId"
        this.aged = aged
        this.lastUpdateCheck = lastUpdateCheck
    }.flatten()

/** Every collaborator an [EHentaiUpdateWorker] pulls, as mocks, registered in Koin. */
internal class EhWorkerHarness {
    val context: Application = ApplicationProvider.getApplicationContext()
    val store = InMemoryPreferenceStore()
    val exhPreferences = ExhPreferences(store)
    val libraryPreferences = LibraryPreferences(store)
    val securityPreferences = SecurityPreferences(store)

    // The debug toggles re-read their preference on every access, so they need a store that keeps writes.
    val toggleStore: PreferenceStore = AndroidPreferenceStore(context)
    val sourceManager = mockk<SourceManager>()
    val updateHelper = mockk<EHentaiUpdateHelper>()
    val updateMangaFromRemote = mockk<UpdateMangaFromRemote>()
    val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    val getFlatMetadataById = mockk<GetFlatMetadataById>()
    val insertFlatMetadata = mockk<InsertFlatMetadata>(relaxed = true)
    val getExhFavoriteMangaWithMetadata = mockk<GetExhFavoriteMangaWithMetadata>()
    val ehSource = mockk<EHentai>()
    val params = mockk<WorkerParameters>(relaxed = true)

    fun start() {
        XLog.init(LogConfiguration.Builder().build(), Printer { _, _, _ -> })
        securityPreferences.hideNotificationContent.set(true)
        stopKoin()
        startKoin {
            modules(
                module {
                    single<PreferenceStore> { toggleStore }
                    single { exhPreferences }
                    single { libraryPreferences }
                    single { securityPreferences }
                    single<SourceManager> { sourceManager }
                    single { updateHelper }
                    single { updateMangaFromRemote }
                    single { getChaptersByMangaId }
                    single { getFlatMetadataById }
                    single { insertFlatMetadata }
                    single { getExhFavoriteMangaWithMetadata }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
            )
        }
        every { params.id } returns UUID.randomUUID()
        every { params.foregroundUpdater } returns ForegroundUpdater { _, _, _ -> throw IllegalStateException("no fg") }
        every { sourceManager.get(any()) } returns null
        every { sourceManager.get(EH_SOURCE) } returns ehSource
        coEvery { getExhFavoriteMangaWithMetadata.await() } returns emptyList()
        coEvery { getFlatMetadataById.await(any()) } returns null
        coEvery { getChaptersByMangaId.await(any()) } returns emptyList()
    }

    fun stop() = stopKoin()

    fun worker() = EHentaiUpdateWorker(context, params)

    /** The remote update of [manga] yields [newChapters]. */
    fun stubRemote(manga: Manga, newChapters: List<Chapter>) {
        coEvery {
            updateMangaFromRemote(
                source = ehSource,
                manga = manga,
                fetchDetails = true,
                fetchChapters = true,
                manualFetch = false,
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } returns Result.success(RemoteMangaUpdate(manga, newChapters))
    }

    fun stubRemoteFailure(manga: Manga, error: Throwable) {
        coEvery {
            updateMangaFromRemote(
                source = ehSource,
                manga = manga,
                fetchDetails = true,
                fetchChapters = true,
                manualFetch = false,
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } returns Result.failure(error)
    }

    private object NoCustomInfo : CustomMangaRepository {
        override fun get(mangaId: Long): CustomMangaInfo? = null
        override fun set(mangaInfo: CustomMangaInfo) = Unit
    }
}
