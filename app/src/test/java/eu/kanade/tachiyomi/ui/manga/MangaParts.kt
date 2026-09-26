package eu.kanade.tachiyomi.ui.manga

import android.os.Looper
import cafe.adriel.voyager.core.model.ScreenModelStore
import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.domain.manga.interactor.SetExcludedScanlators
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.interactor.RefreshTracks
import eu.kanade.domain.track.interactor.TrackChapter
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.online.readMember
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import org.koin.core.module.Module
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.chapter.interactor.SetMangaDefaultChapterFlags
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.manga.interactor.DeleteByMergeId
import tachiyomi.domain.manga.interactor.DeleteMangaById
import tachiyomi.domain.manga.interactor.DeleteMergeById
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.InsertMergedReference
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.manga.interactor.SetMangaChapterFlags
import tachiyomi.domain.manga.interactor.UpdateMergedSettings
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack

/** The interactors of the parts [MangaScreenModel] composes (downloads, library, chapters, merges). */
internal class MangaParts {
    val filterChaptersForDownload: FilterChaptersForDownload = mockk()
    val getDuplicateLibraryManga: GetDuplicateLibraryManga = mockk()
    val getCategories: GetCategories = mockk()
    val addTracks: AddTracks = mockk(relaxed = true)
    val setMangaCategories: SetMangaCategories = mockk(relaxed = true)
    val mangaRepository: MangaRepository = mockk()
    val setReadStatus: SetReadStatus = mockk(relaxed = true)
    val updateChapter: UpdateChapter = mockk(relaxed = true)
    val trackChapter: TrackChapter = mockk(relaxed = true)
    val getTracks: GetTracks = mockk()
    val insertTrack: InsertTrack = mockk(relaxed = true)
    val refreshTracks: RefreshTracks = mockk()
    val setMangaChapterFlags: SetMangaChapterFlags = mockk(relaxed = true)
    val setMangaDefaultChapterFlags: SetMangaDefaultChapterFlags = mockk(relaxed = true)
    val setExcludedScanlators: SetExcludedScanlators = mockk(relaxed = true)
    val setCustomMangaInfo: SetCustomMangaInfo = mockk(relaxed = true)
    val getManga: GetManga = mockk()
    val insertMergedReference: InsertMergedReference = mockk(relaxed = true)
    val updateMergedSettings: UpdateMergedSettings = mockk(relaxed = true)
    val networkToLocalManga: NetworkToLocalManga = mockk()
    val deleteMangaById: DeleteMangaById = mockk(relaxed = true)
    val deleteByMergeId: DeleteByMergeId = mockk(relaxed = true)
    val deleteMergeById: DeleteMergeById = mockk(relaxed = true)
    val coverCache: CoverCache = mockk(relaxed = true)

    init {
        coEvery { getTracks.subscribe(any<Long>()) } returns flowOf(emptyList())
        coEvery { getTracks.await(any<Long>()) } returns emptyList()
        coEvery { getCategories.await() } returns emptyList()
        coEvery { getCategories.await(any()) } returns emptyList()
        coEvery { refreshTracks.await(any()) } returns emptyList()
    }

    fun module(): Module = module {
        single { filterChaptersForDownload }
        single { getDuplicateLibraryManga }
        single { getCategories }
        single { addTracks }
        single { setMangaCategories }
        single { mangaRepository }
        single { setReadStatus }
        single { updateChapter }
        single { trackChapter }
        single { getTracks }
        single { insertTrack }
        single { refreshTracks }
        single { setMangaChapterFlags }
        single { setMangaDefaultChapterFlags }
        single { setExcludedScanlators }
        single { setCustomMangaInfo }
        single { getManga }
        single { insertMergedReference }
        single { updateMergedSettings }
        single { networkToLocalManga }
        single { deleteMangaById }
        single { deleteByMergeId }
        single { deleteMergeById }
        single { coverCache }
    }
}

/** No manga has custom info; what `Manga.title` and friends read through Injekt. */
internal object NoCustomInfo : CustomMangaRepository {
    override fun get(mangaId: Long): CustomMangaInfo? = null

    override fun set(mangaInfo: CustomMangaInfo) = Unit
}

/**
 * Cancels and forgets every coroutine scope Voyager cached for screen models built outside a
 * Navigator, so one test's model never keeps running into the next. Call it before `stopKoin()`:
 * cancelling does not stop a block already running on Dispatchers.IO, which would otherwise reach
 * Injekt after Koin is gone and fail a later test's `runTest`, so it lets the scopes finish first.
 * The entries are removed only afterwards: a draining block that reads `screenModelScope` would
 * otherwise get a fresh, uncancelled scope. The wait idles the main looper the scopes dispatch on
 * (`runBlocking { join() }` there would deadlock) and is bounded rather than asserted: a
 * non-cancellable block parked in `showSnackbar` has no SnackbarHost to resume it.
 */
internal fun clearVoyagerScopes() {
    val dependencies = checkNotNull(ScreenModelStore.readMember(ScreenModelStore::class, "dependencies"))
    val remove = dependencies::class.java.methods.first { it.name == "remove" && it.parameterCount == 1 }
    val entries = (dependencies as Map<*, *>).entries.toList()
    val scopes = entries.mapNotNull { (_, value) -> (value as? Pair<*, *>)?.first as? CoroutineScope }
    try {
        scopes.forEach { it.cancel() }
        idleMainUntilOrTimeout { scopes.all { it.coroutineContext.job.isCompleted } }
    } finally {
        entries.forEach { (key, _) -> remove.invoke(dependencies, key) }
    }
}

private fun idleMainUntilOrTimeout(done: () -> Boolean) {
    val deadline = System.currentTimeMillis() + SCOPE_DRAIN_TIMEOUT_MS
    val onMain = Looper.myLooper() == Looper.getMainLooper()
    while (!done() && System.currentTimeMillis() < deadline) {
        if (onMain) shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(10)
    }
}

private const val SCOPE_DRAIN_TIMEOUT_MS = 5_000L
