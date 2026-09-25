package exh

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.online.UrlImportableSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import mihon.domain.source.interactor.UpdateMangaFromRemote
import mihon.domain.source.models.RemoteMangaUpdate
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.domain.source.service.SourceManager

internal const val GALLERY_URL = "https://gallery.test/g/1/abc"
internal const val MANGA_URL = "/g/1/abc"

/** Every collaborator a [GalleryAdder] pulls, as mocks, registered in Koin. */
internal class GalleryAdderHarness {
    val context: Application = ApplicationProvider.getApplicationContext()
    val getManga = mockk<GetManga>()
    val updateManga = mockk<UpdateManga>()
    val updateMangaFromRemote = mockk<UpdateMangaFromRemote>()
    val networkToLocalManga = mockk<NetworkToLocalManga>()
    val getChapter = mockk<GetChapter>()
    val sourceManager = mockk<SourceManager>()
    val preferences = SourcePreferences(InMemoryPreferenceStore())
    val source = importableSource(sourceId = 1L)
    val manga: Manga = Manga.create().copy(id = 5L, source = 1L, url = MANGA_URL, ogTitle = "Title")
    val chapter: Chapter = Chapter.create().copy(id = 9L, mangaId = 5L, url = "/s/1/2")

    fun start() {
        XLog.init(LogConfiguration.Builder().build(), Printer { _, _, _ -> })
        preferences.enabledLanguages.set(setOf("en"))
        preferences.disabledSources.set(setOf("2"))
        stopKoin()
        startKoin {
            modules(
                module {
                    single { getManga }
                    single { updateManga }
                    single { updateMangaFromRemote }
                    single { networkToLocalManga }
                    single { getChapter }
                    single<SourceManager> { sourceManager }
                    single { preferences }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
            )
        }
        source.stubImport()
        every { sourceManager.getVisibleSources() } returns listOf(source)
        coEvery { getManga.await(any<String>(), any()) } returns manga
        coEvery { getChapter.await(any<String>(), any()) } returns chapter
        coEvery { updateManga.awaitUpdateFavorite(any(), any()) } returns true
        coEvery { networkToLocalManga(any<Manga>()) } returns manga
        coEvery {
            updateMangaFromRemote(
                manga = any(),
                fetchDetails = any(),
                fetchChapters = any(),
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } returns Result.success(RemoteMangaUpdate(manga, emptyList()))
    }

    fun stop() = stopKoin()

    fun adder() = GalleryAdder(
        getManga = getManga,
        updateManga = updateManga,
        updateMangaFromRemote = updateMangaFromRemote,
        networkToLocalManga = networkToLocalManga,
        getChapter = getChapter,
        sourceManager = sourceManager,
    )

    private object NoCustomInfo : CustomMangaRepository {
        override fun get(mangaId: Long): CustomMangaInfo? = null
        override fun set(mangaInfo: CustomMangaInfo) = Unit
    }
}

/** An importable source that claims every url and maps it straight to [MANGA_URL]. */
internal fun importableSource(sourceId: Long, sourceLang: String = "en"): UrlImportableSource {
    val source = mockk<UrlImportableSource> {
        every { id } returns sourceId
        every { lang } returns sourceLang
    }
    source.stubImport()
    return source
}

/** (Re)stubs the url mapping of an [importableSource] so a test's overrides do not leak. */
internal fun UrlImportableSource.stubImport() {
    every { matchesUri(any()) } returns true
    every { mapUrlToChapterUrl(any()) } returns null
    coEvery { mapChapterUrlToMangaUrl(any()) } returns null
    coEvery { mapUrlToMangaUrl(any()) } returns MANGA_URL
    every { cleanMangaUrl(any()) } answers { firstArg() }
    every { cleanChapterUrl(any()) } answers { firstArg() }
}
