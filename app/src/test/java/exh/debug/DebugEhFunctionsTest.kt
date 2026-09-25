package exh.debug

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import eu.kanade.tachiyomi.source.Source
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.ExhPreferences
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import mihon.domain.source.interactor.UpdateMangaFromRemote
import mihon.domain.source.models.RemoteMangaUpdate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.manga.interactor.GetExhFavoriteMangaWithMetadata
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.InsertFlatMetadata
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class DebugEhFunctionsTest {
    @Before
    fun setUp() {
        XLog.init(LogConfiguration.Builder().build(), Printer { _, _, _ -> })
        // The mocks live in the companion because the debug object resolves them once per JVM.
        clearMocks(updateMangaFromRemote, getFlatMetadataById, insertFlatMetadata, getExhFavoriteMangaWithMetadata)
        clearMocks(workManager, sourceManager)
        every { sourceManager.getOrStub(EH_SOURCE_ID) } returns ehSource
        every { sourceManager.getOrStub(EXH_SOURCE_ID) } returns exhSource
        mockkObject(WorkManager)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        stopKoin()
        startKoin {
            modules(
                module {
                    single<Application> { context }
                    single<SourceManager> { sourceManager }
                    single { updateMangaFromRemote }
                    single { getFlatMetadataById }
                    single { insertFlatMetadata }
                    single { getExhFavoriteMangaWithMetadata }
                    single { preferences }
                },
            )
        }
        coEvery { getExhFavoriteMangaWithMetadata.await() } returns listOf(eh, exh, other)
        coEvery { getFlatMetadataById.await(any()) } returns null
        coEvery { getFlatMetadataById.await(1L) } returns metadata(1, aged = true)
        coEvery { getFlatMetadataById.await(2L) } returns metadata(2, aged = false)
        coEvery {
            updateMangaFromRemote(any<Source>(), any(), any(), any(), any(), any(), any())
        } answers { Result.success(RemoteMangaUpdate(secondArg(), emptyList())) }
    }

    @After
    fun tearDown() = stopKoin()

    private fun metadata(id: Long, aged: Boolean) = EHentaiSearchMetadata().apply {
        mangaId = id
        gId = "$id"
        gToken = "tok$id"
        this.aged = aged
    }.flatten()

    @Test
    fun agedFlagsAreClearedAndCounted() {
        DebugEhFunctions.countAgedFlagInEXHManga() shouldBe 1
        // Only the galleries that have metadata are listed, each with its aged flag.
        DebugEhFunctions.getEHMangaListWithAgedFlagInfo() shouldBe
            """
                Aged: true	 Title: m1,
                Aged: false	 Title: m2
            """.trimIndent()
        DebugEhFunctions.resetAgedFlagInEXHManga()
        coVerify(exactly = 2) { insertFlatMetadata.await(any<RaisedSearchMetadata>()) }
    }

    @Test
    fun updaterResetsKnownSources() {
        DebugEhFunctions.resetEHGalleriesForUpdater()
        coVerify(exactly = 1) {
            updateMangaFromRemote(ehSource, eh, fetchDetails = true, fetchChapters = false, any(), any(), any())
        }
        coVerify(exactly = 1) {
            updateMangaFromRemote(exhSource, exh, fetchDetails = true, fetchChapters = false, any(), any(), any())
        }
        coVerify(exactly = 2) { updateMangaFromRemote(any<Source>(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun updaterJobsAreScheduled() {
        DebugEhFunctions.testLaunchEhUpdater()
        verify(exactly = 1) { workManager.enqueue(any<androidx.work.WorkRequest>()) }
        DebugEhFunctions.rescheduleEhUpdater()
        verify(exactly = 1) { workManager.enqueueUniquePeriodicWork(any(), any(), any()) }
    }

    private companion object {
        val context: Application = ApplicationProvider.getApplicationContext()
        val ehSource = mockk<Source>()
        val exhSource = mockk<Source>()
        val sourceManager = mockk<SourceManager> {
            every { getOrStub(EH_SOURCE_ID) } returns ehSource
            every { getOrStub(EXH_SOURCE_ID) } returns exhSource
        }
        val updateMangaFromRemote = mockk<UpdateMangaFromRemote>()
        val getFlatMetadataById = mockk<GetFlatMetadataById>()
        val insertFlatMetadata = mockk<InsertFlatMetadata>(relaxed = true)
        val getExhFavoriteMangaWithMetadata = mockk<GetExhFavoriteMangaWithMetadata>()
        val workManager = mockk<WorkManager>(relaxed = true)
        val preferences = ExhPreferences(InMemoryPreferenceStore())
        val eh: Manga = Manga.create().copy(id = 1, source = EH_SOURCE_ID, ogTitle = "m1")
        val exh: Manga = Manga.create().copy(id = 2, source = EXH_SOURCE_ID, ogTitle = "m2")
        val other: Manga = Manga.create().copy(id = 3, source = 99L, ogTitle = "m3")
    }
}
