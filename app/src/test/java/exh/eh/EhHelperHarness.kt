package exh.eh

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import eu.kanade.domain.manga.interactor.UpdateManga
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.chapter.interactor.GetChapterByUrl
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.History
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.InsertFavoriteEntryAlternative
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.CustomMangaRepository
import java.util.Date

internal const val EH_SOURCE = 6901L

internal fun ehManga(id: Long, favorite: Boolean = false, source: Long = EH_SOURCE): Manga =
    Manga.create().copy(id = id, source = source, favorite = favorite, url = "/g/$id/tok$id", ogTitle = "m$id")

internal fun ehChapter(id: Long, mangaId: Long, url: String, dateUpload: Long = id): Chapter = Chapter.create().copy(
    id = id,
    mangaId = mangaId,
    url = url,
    name = "v1: c$id",
    dateUpload = dateUpload,
)

internal fun ehHistory(id: Long, chapterId: Long, readAt: Long?): History =
    History(id = id, chapterId = chapterId, readAt = readAt?.let(::Date), readDuration = id)

/** Every collaborator an [EHentaiUpdateHelper] pulls, as mocks, registered in Koin. */
internal class EhHelperHarness {
    val context: Application = ApplicationProvider.getApplicationContext()
    val getChapterByUrl = mockk<GetChapterByUrl>()
    val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    val getManga = mockk<GetManga>()
    val updateManga = mockk<UpdateManga>()
    val setMangaCategories = mockk<SetMangaCategories>(relaxed = true)
    val getCategories = mockk<GetCategories>()
    val chapterRepository = mockk<ChapterRepository>(relaxed = true)
    val upsertHistory = mockk<UpsertHistory>(relaxed = true)
    val removeHistory = mockk<RemoveHistory>(relaxed = true)
    val getHistory = mockk<GetHistory>()
    val insertFavoriteEntryAlternative = mockk<InsertFavoriteEntryAlternative>(relaxed = true)

    fun start() {
        XLog.init(LogConfiguration.Builder().build(), Printer { _, _, _ -> })
        stopKoin()
        startKoin {
            modules(
                module {
                    single { getChapterByUrl }
                    single { getChaptersByMangaId }
                    single { getManga }
                    single { updateManga }
                    single { setMangaCategories }
                    single { getCategories }
                    single { chapterRepository }
                    single { upsertHistory }
                    single { removeHistory }
                    single { getHistory }
                    single { insertFavoriteEntryAlternative }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
            )
        }
        coEvery { getChapterByUrl.await(any()) } returns emptyList()
        coEvery { getChaptersByMangaId.await(any()) } returns emptyList()
        coEvery { getManga.await(any<Long>()) } returns null
        coEvery { getHistory.await(any()) } returns emptyList()
        coEvery { getCategories.await(any()) } returns emptyList()
        coEvery { updateManga.awaitAll(any()) } returns true
    }

    fun stop() = stopKoin()

    fun helper() = EHentaiUpdateHelper(context)

    private object NoCustomInfo : CustomMangaRepository {
        override fun get(mangaId: Long): CustomMangaInfo? = null
        override fun set(mangaInfo: CustomMangaInfo) = Unit
    }
}
