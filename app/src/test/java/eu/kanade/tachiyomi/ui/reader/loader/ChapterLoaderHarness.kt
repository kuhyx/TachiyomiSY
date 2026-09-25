package eu.kanade.tachiyomi.ui.reader.loader

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.readerThreads
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.koin.core.context.startKoin
import org.koin.dsl.module
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.source.service.SourceManager

/**
 * A [ChapterLoader] over mocks: every page loader it may build is constructor-mocked to serve
 * [pages], and Koin provides what their constructors pull from Injekt.
 */
internal class ChapterLoaderHarness {
    val context: Context = ApplicationProvider.getApplicationContext()
    val store: MapPreferenceStore = MapPreferenceStore()
    val readerPrefs: ReaderPreferences = ReaderPreferences(store).also { it.readerThreads.set(0) }
    val downloadManager: DownloadManager = mockk()
    val downloadProvider: DownloadProvider = mockk()
    val sourceManager: SourceManager = mockk()
    val manga: Manga = Manga.create().copy(id = 10L, source = 1L, ogTitle = "Title")
    var pages: List<ReaderPage> = listOf(ReaderPage(0), ReaderPage(1))

    fun start() {
        startKoin {
            modules(
                module {
                    single { context as Application }
                    single { readerPrefs }
                    single { SourcePreferences(store) }
                    single<ChapterCache> { mockk(relaxUnitFun = true) }
                },
            )
        }
        listOf(
            DownloadPageLoader::class,
            HttpPageLoader::class,
            DirectoryPageLoader::class,
            ArchivePageLoader::class,
            EpubPageLoader::class,
        ).forEach { mockkConstructor(it) }
        coEvery { anyConstructed<DownloadPageLoader>().getPages() } answers { pages }
        coEvery { anyConstructed<HttpPageLoader>().getPages() } answers { pages }
        coEvery { anyConstructed<DirectoryPageLoader>().getPages() } answers { pages }
        coEvery { anyConstructed<ArchivePageLoader>().getPages() } answers { pages }
        coEvery { anyConstructed<EpubPageLoader>().getPages() } answers { pages }
        downloaded(false)
    }

    fun downloaded(value: Boolean) {
        every {
            downloadManager.isChapterDownloaded(
                chapterName = any(),
                chapterScanlator = any(),
                chapterUrl = any(),
                mangaTitle = any(),
                sourceId = any(),
                skipCache = true,
            )
        } returns value
    }

    fun loader(
        source: Source,
        references: List<MergedMangaReference> = emptyList(),
        merged: Map<Long, Manga> = emptyMap(),
    ): ChapterLoader = ChapterLoader(
        services = ChapterLoader.Services(
            context = context,
            downloadManager = downloadManager,
            downloadProvider = downloadProvider,
            sourceManager = sourceManager,
            readerPrefs = readerPrefs,
        ),
        manga = manga,
        source = source,
        merged = ChapterLoader.MergedData(references, merged),
    )
}
