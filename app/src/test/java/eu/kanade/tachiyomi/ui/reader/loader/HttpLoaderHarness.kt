package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.readerThreads
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.File

/**
 * The collaborators of one [HttpPageLoader]: a mock source and chapter cache, real preferences.
 * [threads] worker loops are started; 0 keeps the queue untouched so a test can inspect it.
 */
internal class HttpLoaderHarness(threads: Int = 0, sourceId: Long = 1L) {
    val store: MapPreferenceStore = MapPreferenceStore()
    val readerPreferences: ReaderPreferences = ReaderPreferences(store).also { it.readerThreads.set(threads) }
    val sourcePreferences: SourcePreferences = SourcePreferences(store)
    val chapterCache: ChapterCache = mockk(relaxUnitFun = true)
    val source: HttpSource = mockk<HttpSource>().also { every { it.id } returns sourceId }
    val chapter: ReaderChapter = readerChapter()

    fun loader(): HttpPageLoader = HttpPageLoader(
        chapter = chapter,
        source = source,
        chapterCache = chapterCache,
        readerPreferences = readerPreferences,
        sourcePreferences = sourcePreferences,
    )

    /** Serves [bytes] for every cached image file. */
    fun cacheServes(file: File, bytes: ByteArray) {
        file.writeBytes(bytes)
        every { chapterCache.getImageFile(any()) } returns file
    }
}

internal fun imageResponse(): Response = Response.Builder()
    .request(Request.Builder().url("https://img/0").build())
    .protocol(Protocol.HTTP_1_1)
    .code(200)
    .message("OK")
    .build()

/** Waits (on real threads) until [page] reaches a status matching [done]. */
internal fun awaitStatus(page: Page, done: (Page.State) -> Boolean) {
    runBlocking {
        withTimeout(10_000) {
            while (!done(page.status)) {
                delay(5)
            }
        }
    }
}
