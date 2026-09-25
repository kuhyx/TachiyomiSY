package eu.kanade.tachiyomi.ui.updates

import android.app.Application
import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.customInfoModule
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.service.UpdatesPreferences

/** The collaborators of [UpdatesScreenModel]: mockk interactors and managers, real preferences. */
internal class UpdatesHarness {
    val store: MapPreferenceStore = MapPreferenceStore()
    val sourceManager: SourceManager = mockk()
    val queue: MutableStateFlow<List<Download>> = MutableStateFlow(emptyList())

    /** What the download manager's status flow emits; finite, so the observer's collect returns. */
    var statuses: Flow<Download> = emptyFlow()
    val downloadManager: DownloadManager = mockk(relaxed = true) {
        every { queueState } returns queue
        every { statusFlow() } answers { statuses }
        every { progressFlow() } returns emptyFlow()
        every { isChapterDownloaded(any(), any(), any(), any(), any(), any()) } returns false
    }
    val downloadCache: DownloadCache = mockk { every { changes } returns MutableStateFlow(Unit) }
    val updateChapter: UpdateChapter = mockk(relaxed = true)
    val setReadStatus: SetReadStatus = mockk(relaxed = true)
    val updates: MutableStateFlow<List<UpdatesWithRelations>> = MutableStateFlow(emptyList())
    val getUpdates: GetUpdates = mockk { every { subscribe(any(), any(), any(), any(), any()) } returns updates }
    val getManga: GetManga = mockk()
    val getChapter: GetChapter = mockk()
    val libraryPreferences: LibraryPreferences = LibraryPreferences(store)
    val updatesPreferences: UpdatesPreferences = UpdatesPreferences(store)
    val readerPreferences: ReaderPreferences = ReaderPreferences(store)
    val application: Application = mockk()

    fun model(): UpdatesScreenModel = UpdatesScreenModel(
        sourceManager = sourceManager,
        downloadManager = downloadManager,
        downloadCache = downloadCache,
        updateChapter = updateChapter,
        setReadStatus = setReadStatus,
        getUpdates = getUpdates,
        getManga = getManga,
        getChapter = getChapter,
        libraryPreferences = libraryPreferences,
        updatesPreferences = updatesPreferences,
        readerPreferences = readerPreferences,
    )

    fun koinModules(): List<Module> = listOf(
        customInfoModule(),
        module {
            single { sourceManager }
            single { downloadManager }
            single { downloadCache }
            single { updateChapter }
            single { setReadStatus }
            single { getUpdates }
            single { getManga }
            single { getChapter }
            single { libraryPreferences }
            single { updatesPreferences }
            single { readerPreferences }
            single { application }
        },
    )
}

internal fun update(
    chapterId: Long,
    mangaId: Long = 1,
    dateFetch: Long = 0,
    bookmark: Boolean = false,
    sourceId: Long = 1,
): UpdatesWithRelations = UpdatesWithRelations(
    mangaId = mangaId,
    ogMangaTitle = "M$mangaId",
    chapterId = chapterId,
    chapterName = "C$chapterId",
    scanlator = null,
    chapterUrl = "/c/$chapterId",
    read = false,
    bookmark = bookmark,
    lastPageRead = 0,
    sourceId = sourceId,
    dateFetch = dateFetch,
    coverData = MangaCover(
        mangaId = mangaId,
        sourceId = sourceId,
        isMangaFavorite = true,
        ogUrl = null,
        lastModified = 0,
    ),
)

internal fun item(
    chapterId: Long,
    state: Download.State = Download.State.NOT_DOWNLOADED,
    mangaId: Long = 1,
    selected: Boolean = false,
): UpdatesItem = UpdatesItem(
    update = update(chapterId, mangaId),
    downloadStateProvider = { state },
    downloadProgressProvider = { 0 },
    selected = selected,
)

internal fun download(chapterId: Long, state: Download.State): Download = Download(
    source = mockk<HttpSource>(),
    manga = Manga.create().copy(id = 1),
    chapter = Chapter.create().copy(id = chapterId),
).apply { transition(state) }
