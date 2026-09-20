package mihon.feature.migration.list

import androidx.annotation.FloatRange
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.getNameForMangaInfo
import exh.util.ThrottleManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import mihon.domain.migration.usecases.MigrateMangaUseCase
import mihon.domain.source.interactor.UpdateMangaFromRemote
import mihon.feature.migration.list.models.MigratingManga
import mihon.feature.migration.list.models.MigratingManga.SearchResult
import mihon.feature.migration.list.search.SmartSourceSearchEngine
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal const val MAX_CONCURRENT_SOURCES = 5

internal class MigrationListScreenModel(
    mangaIds: Collection<Long>,
    extraSearchQuery: String?,
    private val preferences: SourcePreferences = Injekt.get(),
    internal val sourceManager: SourceManager = Injekt.get(),
    internal val getManga: GetManga = Injekt.get(),
    internal val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    internal val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    internal val migrateManga: MigrateMangaUseCase = Injekt.get(),
    internal val updateMangaFromRemote: UpdateMangaFromRemote = Injekt.get(),
) : StateScreenModel<MigrationListScreenModel.State>(State()) {

    internal val smartSearchEngine = SmartSourceSearchEngine(extraSearchQuery)

    // SY -->
    internal val throttleManager = ThrottleManager()
    // SY <--

    val items
        inline get() = state.value.items

    private val hideUnmatched = preferences.migrationHideUnmatched.get()
    private val hideWithoutUpdates = preferences.migrationHideWithoutUpdates.get()

    internal val navigateBackChannel = Channel<Unit>()
    val navigateBackEvent = navigateBackChannel.receiveAsFlow()

    internal var migrateJob: Job? = null

    init {
        screenModelScope.launchIO {
            val manga = mangaIds
                .map {
                    async {
                        getManga.await(it)?.let { manga ->
                            val chapterInfo = getChapterInfo(it)
                            MigratingManga(
                                manga = manga,
                                chapterCount = chapterInfo.chapterCount,
                                latestChapter = chapterInfo.latestChapter,
                                source = sourceManager.getOrStub(manga.source).getNameForMangaInfo(),
                                parentContext = screenModelScope.coroutineContext,
                            )
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
            mutableState.update { it.copy(items = manga) }
            runMigrations(manga)
        }
    }

    internal suspend fun Manga.toSuccessSearchResult(): SearchResult.Success {
        val chapterInfo = getChapterInfo(id)
        val source = sourceManager.getOrStub(source).getNameForMangaInfo()
        return SearchResult.Success(
            manga = this,
            chapterCount = chapterInfo.chapterCount,
            latestChapter = chapterInfo.latestChapter,
            source = source,
        )
    }

    private suspend fun runMigrations(mangas: List<MigratingManga>) {
        // SY -->
        throttleManager.resetThrottle()
        // SY <--
        val prioritizeByChapters = preferences.migrationPrioritizeByChapters.get()
        val deepSearchMode = preferences.migrationDeepSearchMode.get()

        val sources = preferences.migrationSources.get()
            .mapNotNull { sourceManager.get(it) }

        for (manga in mangas) {
            if (!currentCoroutineContext().isActive) break
            if (manga.isAwaitingSearch()) migrate(manga, sources, prioritizeByChapters, deepSearchMode)
        }
    }

    private fun MigratingManga.isAwaitingSearch(): Boolean =
        manga.id in state.value.mangaIds &&
            searchResult.value == SearchResult.Searching &&
            migrationScope.isActive

    // Searches the configured sources for [manga] and records the outcome on it.
    private suspend fun migrate(
        manga: MigratingManga,
        sources: List<Source>,
        prioritizeByChapters: Boolean,
        deepSearchMode: Boolean,
    ) {
        val result = try {
            manga.migrationScope.async {
                if (prioritizeByChapters) {
                    searchByMostChapters(manga, sources, deepSearchMode)
                } else {
                    searchFirstMatch(manga, sources, deepSearchMode)
                }
            }
                .await()
        } catch (_: CancellationException) {
            return
        }

        if (result != null && result.first.thumbnailUrl == null) {
            try {
                updateMangaFromRemote(result.first, fetchDetails = true, manualFetch = true).getOrThrow().manga
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }

        manga.searchResult.value = result?.first?.toSuccessSearchResult() ?: SearchResult.NotFound

        if (result == null && hideUnmatched) {
            removeManga(manga)
        }
        if (result != null &&
            hideWithoutUpdates &&
            (result.second.latestChapter ?: 0.0) <= (manga.latestChapter ?: 0.0)
        ) {
            removeManga(manga)
        }

        updateMigrationProgress()
    }

    internal suspend fun updateMigrationProgress() {
        mutableState.update { state ->
            state.copy(
                finishedCount = items.count { it.searchResult.value != SearchResult.Searching },
                migrationComplete = migrationComplete(),
            )
        }
        if (items.isEmpty()) {
            navigateBack()
        }
    }

    private fun migrationComplete() = items.all { it.searchResult.value != SearchResult.Searching } &&
        items.any { it.searchResult.value is SearchResult.Success }

    /** Applies [func] to the state; the extension files reach the protected flow through it. */
    internal fun updateState(func: (State) -> State) {
        mutableState.update(func)
    }

    override fun onDispose() {
        super.onDispose()
        items.forEach {
            it.cancelMigration()
        }
    }

    data class ChapterInfo(
        val latestChapter: Double?,
        val chapterCount: Int,
    )

    sealed interface Dialog {
        data class Migrate(val copy: Boolean, val totalCount: Int, val skippedCount: Int) : Dialog
        data class Progress(@FloatRange(0.0, 1.0) val progress: Float) : Dialog
        data object Exit : Dialog
    }

    data class State(
        val items: List<MigratingManga> = listOf(),
        val finishedCount: Int = 0,
        val migrationComplete: Boolean = false,
        val dialog: Dialog? = null,
    ) {
        val mangaIds: List<Long> = items.map { it.manga.id }
    }
}
