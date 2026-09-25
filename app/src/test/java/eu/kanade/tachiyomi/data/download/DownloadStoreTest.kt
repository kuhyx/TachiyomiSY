package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class DownloadStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sourceManager: SourceManager = mockk()
    private val getManga: GetManga = mockk()
    private val getChapter: GetChapter = mockk()
    private val source: HttpSource = mockk()
    private val manga = Manga.create().copy(id = 1L, source = 5L)
    private lateinit var store: DownloadStore

    private fun chapter(id: Long) = Chapter.create().copy(id = id, mangaId = 1L)

    private fun download(id: Long, owner: Manga = manga) = Download(source, owner, chapter(id))

    @Before
    fun setUp() {
        context.getSharedPreferences("active_downloads", Context.MODE_PRIVATE).edit { clear() }
        every { sourceManager.get(5L) } returns source
        coEvery { getManga.await(1L) } returns manga
        coEvery { getChapter.await(any<Long>()) } answers { chapter(firstArg()) }
        store = DownloadStore(
            context = context,
            sourceManager = sourceManager,
            json = Json,
            getManga = getManga,
            getChapter = getChapter,
        )
    }

    @Test
    fun restoreKeepsQueueOrder() {
        store.addAll(listOf(download(3L), download(1L), download(2L)))
        store.restore().map { it.chapter.id } shouldBe listOf(3L, 1L, 2L)
        store.restore() shouldBe emptyList()
        coVerify(exactly = 1) { getManga.await(1L) }
    }

    @Test
    fun removalsDropEntries() {
        store.addAll(listOf(download(1L), download(2L), download(3L)))
        store.remove(download(1L))
        store.removeAll(listOf(download(2L)))
        store.restore().map { it.chapter.id } shouldBe listOf(3L)
        store.addAll(listOf(download(4L)))
        store.clear()
        store.restore() shouldBe emptyList()
    }

    @Test
    fun brokenEntriesAreSkipped() {
        store.addAll(listOf(download(1L)))
        context.getSharedPreferences("active_downloads", Context.MODE_PRIVATE).edit {
            putString("garbage", "{not json")
            putInt("number", 4)
        }
        store.restore().map { it.chapter.id } shouldBe listOf(1L)
    }

    @Test
    fun goneDataIsSkipped() {
        val other = manga.copy(id = 2L, source = 6L)
        val missing = manga.copy(id = 3L)
        coEvery { getManga.await(2L) } returns other
        coEvery { getManga.await(3L) } returns null
        every { sourceManager.get(6L) } returns mockk<Source>()
        coEvery { getChapter.await(9L) } returns null
        store.addAll(listOf(download(9L), download(id = 7L, owner = other), download(id = 8L, owner = missing)))
        store.restore() shouldBe emptyList()
    }

    @Test
    fun defaultsComeFromInjekt() {
        startKoin {
            modules(
                module {
                    single { sourceManager }
                    single<Json> { Json }
                    single { getManga }
                    single { getChapter }
                },
            )
        }
        try {
            DownloadStore(context).restore() shouldBe emptyList()
        } finally {
            stopKoin()
        }
    }
}
