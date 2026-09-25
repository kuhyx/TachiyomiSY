package eu.kanade.tachiyomi.ui.history

import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.customInfoModule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.source.service.SourceManager
import java.util.Date

/** The collaborators of [HistoryScreenModel]: mockk interactors, real preferences. */
internal class HistoryHarness {
    val addTracks: AddTracks = mockk(relaxed = true)
    val getCategories: GetCategories = mockk()
    val getDuplicate: GetDuplicateLibraryManga = mockk()
    val getHistory: GetHistory = mockk { every { subscribe(any()) } returns MutableStateFlow(emptyList()) }
    val getManga: GetManga = mockk()
    val getNextChapters: GetNextChapters = mockk()
    val libraryPreferences: LibraryPreferences = LibraryPreferences(MapPreferenceStore())
    val removeHistory: RemoveHistory = mockk(relaxed = true)
    val setMangaCategories: SetMangaCategories = mockk(relaxed = true)
    val updateManga: UpdateManga = mockk()
    val source: Source = mockk()
    val sourceManager: SourceManager = mockk { every { getOrStub(any()) } returns source }

    fun model(): HistoryScreenModel = HistoryScreenModel(
        addTracks = addTracks,
        getCategories = getCategories,
        getDuplicateLibraryManga = getDuplicate,
        getHistory = getHistory,
        getManga = getManga,
        getNextChapters = getNextChapters,
        libraryPreferences = libraryPreferences,
        removeHistory = removeHistory,
        setMangaCategories = setMangaCategories,
        updateManga = updateManga,
        sourceManager = sourceManager,
    )

    fun koinModules(): List<Module> = listOf(
        customInfoModule(),
        module {
            single { addTracks }
            single { getCategories }
            single { getDuplicate }
            single { getHistory }
            single { getManga }
            single { getNextChapters }
            single { libraryPreferences }
            single { removeHistory }
            single { setMangaCategories }
            single { updateManga }
            single { sourceManager }
        },
    )
}

internal fun history(id: Long, readAt: Date?): HistoryWithRelations = HistoryWithRelations(
    id = id,
    chapterId = id,
    mangaId = id,
    ogTitle = "Title $id",
    chapterNumber = 1.0,
    readAt = readAt,
    readDuration = 0,
    coverData = MangaCover(mangaId = id, sourceId = 1, isMangaFavorite = false, ogUrl = null, lastModified = 0),
)

internal fun category(id: Long): Category = Category(id = id, name = "C$id", order = id, flags = 0)
