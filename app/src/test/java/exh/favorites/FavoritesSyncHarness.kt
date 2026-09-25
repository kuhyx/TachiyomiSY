package exh.favorites

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.source.online.all.fetchFavorites
import exh.log.EHLogLevel
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.pref.DelegateSourcePreferences
import exh.source.EXH_SOURCE_ID
import exh.source.ExhPreferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import mihon.domain.source.interactor.UpdateMangaFromRemote
import mihon.domain.source.models.RemoteMangaUpdate
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.interactor.UpdateCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.DeleteFavoriteEntries
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetFavoriteEntries
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.InsertFavoriteEntries
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.FavoriteEntry
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.domain.source.service.SourceManager
import java.io.IOException

internal fun favManga(id: Long, gid: String, favorite: Boolean = true, source: Long = EXH_SOURCE_ID): Manga =
    Manga.create().copy(id = id, source = source, favorite = favorite, url = "/g/$gid/tok$gid", ogTitle = "m$gid")

internal fun libraryEntry(manga: Manga) = LibraryManga(
    manga = manga,
    categories = emptyList(),
    totalChapters = 0,
    readCount = 0,
    bookmarkCount = 0,
    latestUpload = 0,
    chapterFetchedAt = 0,
    lastRead = 0,
)

internal fun category(id: Long, name: String, order: Long) = Category(id = id, name = name, order = order, flags = 0)

internal fun favEntry(gid: String, category: Int = 0) =
    FavoriteEntry(title = "m$gid", gid = gid, token = "tok$gid", category = category)

internal fun parsedManga(gid: String, fav: Int = 0): EHentai.ParsedManga {
    val manga = SManga.create().apply {
        url = "/g/$gid/tok$gid"
        title = "m$gid"
    }
    return EHentai.ParsedManga(fav, manga, EHentaiSearchMetadata())
}

/** Every collaborator a [FavoritesSyncHelper] pulls, as mocks, registered in Koin. */
internal class FavoritesSyncHarness {
    val context: Application = ApplicationProvider.getApplicationContext()
    val exhPreferences = ExhPreferences(InMemoryPreferenceStore())
    val getLibraryManga = mockk<GetLibraryManga>()
    val getCategories = mockk<GetCategories>()
    val getManga = mockk<GetManga>()
    val updateManga = mockk<UpdateManga>(relaxed = true)
    val setMangaCategories = mockk<SetMangaCategories>(relaxed = true)
    val createCategoryWithName = mockk<CreateCategoryWithName>()
    val updateCategory = mockk<UpdateCategory>()
    val sourceManager = mockk<SourceManager>()
    val getFavorites = mockk<GetFavorites>()
    val deleteFavoriteEntries = mockk<DeleteFavoriteEntries>(relaxed = true)
    val getFavoriteEntries = mockk<GetFavoriteEntries>()
    val insertFavoriteEntries = mockk<InsertFavoriteEntries>(relaxed = true)
    val updateMangaFromRemote = mockk<UpdateMangaFromRemote>()
    val workManager = mockk<WorkManager>(relaxed = true)
    val exh = mockk<EHentai>()

    /** Every request the fake ExHentai received. */
    val requests = mutableListOf<Request>()

    /** What the fake ExHentai answers: an HTTP status, or null to fail the connection. */
    var remoteStatus: Int? = 200

    private val remote = Interceptor { chain ->
        requests += chain.request()
        val status = remoteStatus ?: throw IOException("offline")
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message("m")
            .body("".toResponseBody())
            .build()
    }

    fun start() {
        XLog.init(LogConfiguration.Builder().build(), Printer { _, _, _ -> })
        EHLogLevel.init(context)
        mockkObject(WorkManager)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        mockkStatic("eu.kanade.tachiyomi.source.online.all.EHentaiGalleryKt")
        stopKoin()
        startKoin { modules(koinModule()) }
        stubCollaborators()
    }

    private fun koinModule() = module {
        single<Application> { context }
        single { exhPreferences }
        single { getLibraryManga }
        single { getCategories }
        single { getManga }
        single { updateManga }
        single { setMangaCategories }
        single { createCategoryWithName }
        single { updateCategory }
        single<SourceManager> { sourceManager }
        single { getFavorites }
        single { deleteFavoriteEntries }
        single { getFavoriteEntries }
        single { insertFavoriteEntries }
        single { updateMangaFromRemote }
        single { mockk<NetworkToLocalManga>() }
        single { mockk<GetChapter>() }
        single { SourcePreferences(InMemoryPreferenceStore()) }
        single { GetCustomMangaInfo(NoCustomInfo) }
        single { mockk<NetworkHelper>(relaxed = true) { every { client } returns OkHttpClient() } }
        single { NetworkPreferences(InMemoryPreferenceStore()) }
        single { DelegateSourcePreferences(InMemoryPreferenceStore()) }
    }

    private fun stubCollaborators() {
        every { sourceManager.get(EXH_SOURCE_ID) } returns exh
        every { exh.id } returns EXH_SOURCE_ID
        every { exh.lang } returns "all"
        every { exh.baseUrl } returns "https://exhentai.org"
        every { exh.client } returns OkHttpClient.Builder().addInterceptor(remote).build()
        every { exh.matchesUri(any()) } returns true
        every { exh.mapUrlToChapterUrl(any()) } returns null
        coEvery { exh.mapChapterUrlToMangaUrl(any()) } returns null
        coEvery { exh.mapUrlToMangaUrl(any()) } answers { firstArg<Uri>().path }
        every { exh.cleanMangaUrl(any()) } answers { firstArg() }
        coEvery { exh.fetchFavorites() } returns (emptyList<EHentai.ParsedManga>() to emptyList())
        coEvery { getLibraryManga.await() } returns emptyList()
        coEvery { getCategories.await() } returns emptyList()
        coEvery { getCategories.await(any()) } returns emptyList()
        coEvery { getFavorites.await() } returns emptyList()
        coEvery { getFavoriteEntries.await() } returns emptyList()
        coEvery { getManga.await(any<String>(), any()) } returns null
        coEvery {
            updateMangaFromRemote(
                manga = any(),
                fetchDetails = any(),
                fetchChapters = any(),
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } answers { Result.success(RemoteMangaUpdate(firstArg(), emptyList())) }
    }

    fun stop() {
        unmockkAll()
        stopKoin()
    }

    fun helper() = FavoritesSyncHelper(context)

    private object NoCustomInfo : CustomMangaRepository {
        override fun get(mangaId: Long): CustomMangaInfo? = null
        override fun set(mangaInfo: CustomMangaInfo) = Unit
    }
}
