package eu.kanade.tachiyomi.data.download

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkStatic
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.Before
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/**
 * A [DownloadManager] over the [DownloaderTestBase] fixtures: its own [Downloader], store and pending
 * deleter are real; the cache is the relaxed mock, with its extension functions stubbed.
 */
internal abstract class DownloadManagerTestBase : DownloaderTestBase() {

    protected lateinit var manager: DownloadManager

    @Before
    fun setUpManager() {
        loadKoinModules(
            module {
                single { chapterCache }
                single { provider.downloadPreferences }
                single { XML.v1 {} }
                single { getCategories }
                single { getTracks }
                single { sourcePreferences }
            },
        )
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadCacheRemovalsKt")
        coEvery { cache.removeChapters(any(), any()) } returns Unit
        coEvery { cache.removeManga(any()) } returns Unit
        coEvery { cache.removeSource(any()) } returns Unit
        coEvery { cache.removeFolders(any(), any()) } returns Unit
        coEvery { cache.removeChapter(any(), any()) } returns Unit
        coEvery { cache.renameManga(any(), any(), any()) } returns Unit
        every { sourceManager.getOrStub(5L) } returns source
        manager = DownloadManager(
            context = context,
            provider = provider.provider,
            cache = cache,
            getCategories = getCategories,
            sourceManager = sourceManager,
            downloadPreferences = provider.downloadPreferences,
        )
    }
}
