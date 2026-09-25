package mihon.feature.migration.list

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mihon.domain.migration.usecases.MigrateMangaUseCase
import mihon.domain.source.interactor.UpdateMangaFromRemote
import mihon.domain.source.models.RemoteMangaUpdate
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

internal const val WAIT_MS = 20_000L

/** Every collaborator of [MigrationListScreenModel], mocked; library manga 1..n live on source 1. */
internal class MigrationListHarness {
    val preferences = SourcePreferences(InMemoryPreferenceStore())
    val sourceManager = mockk<SourceManager>()
    val getManga = mockk<GetManga>()
    val networkToLocalManga = mockk<NetworkToLocalManga>()
    val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    val migrateManga = mockk<MigrateMangaUseCase>()
    val updateMangaFromRemote = mockk<UpdateMangaFromRemote>()

    /** What each search source answers, by source id. */
    val hits = mutableMapOf<Long, (String) -> List<SManga>>()

    /** Chapter numbers per manga id; unknown ids have none. */
    val chapters = mutableMapOf<Long, List<Double>>()

    fun start() {
        stopKoin()
        startKoin { modules(module { single { preferences } }) }
        preferences.enabledLanguages.set(setOf("en"))
        every { sourceManager.getOrStub(any()) } answers { searchSource(firstArg()) }
        every { sourceManager.get(any()) } answers { searchSource(firstArg()) }
        coEvery { getManga.await(any<Long>()) } answers {
            val id = firstArg<Long>()
            if (id < 100L) listManga(id, "Entry $id") else null
        }
        coEvery { networkToLocalManga(any<Manga>()) } answers {
            val manga = firstArg<Manga>()
            manga.copy(id = manga.url.substringAfterLast('/').toLong())
        }
        coEvery { getChaptersByMangaId.await(any(), any()) } answers {
            chapters[firstArg()].orEmpty().map { Chapter.create().copy(chapterNumber = it) }
        }
        coEvery { migrateManga(any(), any(), any(), any()) } returns Unit
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

    fun stop() = stopKoin()

    /** A search source whose results come from [hits]; a result's url ends in its manga id. */
    fun searchSource(id: Long): Source {
        val source = mockk<Source>()
        every { source.id } returns id
        every { source.name } returns "Source $id"
        every { source.lang } returns "en"
        every { source.getFilterList() } returns FilterList()
        coEvery { source.getSearchManga(any(), any(), any()) } answers {
            MangasPage(hits[id]?.invoke(secondArg()).orEmpty(), false)
        }
        return source
    }

    fun model(ids: List<Long>, extraQuery: String? = null) = MigrationListScreenModel(
        mangaIds = ids,
        extraSearchQuery = extraQuery,
        preferences = preferences,
        sourceManager = sourceManager,
        getManga = getManga,
        networkToLocalManga = networkToLocalManga,
        getChaptersByMangaId = getChaptersByMangaId,
        migrateManga = migrateManga,
        updateMangaFromRemote = updateMangaFromRemote,
    )
}

/** A search hit titled [title] that maps to local manga [id]. */
internal fun hit(title: String, id: Long, thumbnail: String? = "cover"): SManga = SManga.create().apply {
    this.title = title
    url = "/manga/$id"
    thumbnail_url = thumbnail
}

/** Waits until [predicate] holds for the model's state. */
internal fun MigrationListScreenModel.awaitState(
    predicate: (MigrationListScreenModel.State) -> Boolean,
): MigrationListScreenModel.State = runBlocking { withTimeout(WAIT_MS) { state.first(predicate) } }
