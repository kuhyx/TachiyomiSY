package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.produceState
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.Executors

private const val SEARCH_THREADS = 5

internal abstract class SearchScreenModel(
    initialState: State = State(),
    sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    internal val getManga: GetManga = Injekt.get(),
    internal val preferences: SourcePreferences = Injekt.get(),
) : StateScreenModel<SearchScreenModel.State>(initialState) {

    private val coroutineDispatcher = Executors.newFixedThreadPool(SEARCH_THREADS).asCoroutineDispatcher()
    private var searchJob: Job? = null

    private val enabledLanguages = sourcePreferences.enabledLanguages.get()
    private val disabledSources = sourcePreferences.disabledSources.get()
    protected val pinnedSources = sourcePreferences.pinnedSources.get()

    private var lastQuery: String? = null
    private var lastSourceFilter: SourceFilter? = null

    protected var extensionFilter: String? = null

    open val sortComparator = { map: Map<Source, SearchItemResult> ->
        compareBy<Source>(
            { (map[it] as? SearchItemResult.Success)?.isEmpty ?: true },
            { it.id.toString() !in pinnedSources },
            { "${it.name.lowercase()} (${it.lang})" },
        )
    }

    init {
        screenModelScope.launch {
            preferences.globalSearchFilterState.changes().collectLatest { state ->
                mutableState.update { it.copy(onlyShowHasResults = state) }
            }
        }
    }

    /** Applies [func] to the state; the extension files reach the protected flow through it. */
    internal fun updateState(func: (State) -> State) {
        mutableState.update(func)
    }

    @Composable
    fun getManga(initialManga: Manga): androidx.compose.runtime.State<Manga> {
        return produceState(initialValue = initialManga) {
            getManga.subscribe(initialManga.url, initialManga.source)
                .filterNotNull()
                .collectLatest { manga ->
                    value = manga
                }
        }
    }

    open fun getEnabledSources(): List<Source> {
        return sourceManager.getVisibleSources()
            .filter { it.lang in enabledLanguages && it.id.toString() !in disabledSources }
            .sortedWith(
                compareBy(
                    { it.id.toString() !in pinnedSources },
                    { "${it.name.lowercase()} (${it.lang})" },
                ),
            )
    }

    private fun getSelectedSources(): List<Source> {
        val enabledSources = getEnabledSources()

        val filter = extensionFilter
        if (filter.isNullOrEmpty()) {
            return enabledSources
        }

        // SY -->
        val filteredSourceIds = extensionManager.installedExtensionsFlow.value
            .filter { it.pkgName == filter }
            .flatMap { it.sources }
            .map { it.id }
        return enabledSources.filter { it.id in filteredSourceIds }
        // SY <--
    }

    fun search() {
        val query = state.value.searchQuery
        val sourceFilter = state.value.sourceFilter

        if (query.isNullOrBlank()) return

        val sameQuery = this.lastQuery == query
        if (sameQuery && this.lastSourceFilter == sourceFilter) return

        this.lastQuery = query
        this.lastSourceFilter = sourceFilter

        searchJob?.cancel()

        val sources = getSelectedSources()

        // Reuse previous results if possible
        val existingResults = if (sameQuery) state.value.items else emptyMap()
        updateItems(sources.associateWith { existingResults[it] ?: SearchItemResult.Loading })

        searchJob = ioCoroutineScope.launch {
            sources
                .filter { state.value.items[it] is SearchItemResult.Loading }
                .map { source -> async { searchSource(source, query) } }
                .awaitAll()
        }
    }

    // The result lands only while this search is still the current one.
    private suspend fun CoroutineScope.searchSource(source: Source, query: String) {
        val result = try {
            val page = withContext(coroutineDispatcher) {
                source.getSearchManga(1, query, source.getFilterList())
            }
            val titles = page.mangas
                .map { it.toDomainManga(source.id) }
                .distinctBy { it.url }
                .let { networkToLocalManga(it) }
            SearchItemResult.Success(titles)
        } catch (expected: Exception) {
            // Any failure ends here and the fallback below applies.
            SearchItemResult.Error(expected)
        }
        if (isActive) {
            updateItem(source, result)
        }
    }

    private fun updateItems(items: Map<Source, SearchItemResult>) {
        mutableState.update {
            it.copy(
                items = items
                    .toSortedMap(sortComparator(items)),
            )
        }
    }

    private fun updateItem(source: Source, result: SearchItemResult) {
        updateItems(state.value.items + (source to result))
    }

    @Immutable
    data class State(
        val from: Manga? = null,
        val searchQuery: String? = null,
        val sourceFilter: SourceFilter = SourceFilter.PinnedOnly,
        val onlyShowHasResults: Boolean = false,
        val items: Map<Source, SearchItemResult> = mapOf(),
        val dialog: Dialog? = null,
    ) {
        val progress: Int = items.count { it.value !is SearchItemResult.Loading }
        val total: Int = items.size
        val filteredItems = items.filter { (_, result) -> result.isVisible(onlyShowHasResults) }
    }

    sealed interface Dialog {
        data class Migrate(val target: Manga, val current: Manga) : Dialog
    }
}

internal enum class SourceFilter {
    All,
    PinnedOnly,
}

internal sealed interface SearchItemResult {
    data object Loading : SearchItemResult

    data class Error(
        val throwable: Throwable,
    ) : SearchItemResult

    data class Success(
        val result: List<Manga>,
    ) : SearchItemResult {
        val isEmpty: Boolean
            get() = result.isEmpty()
    }

    fun isVisible(onlyShowHasResults: Boolean): Boolean = !onlyShowHasResults || (this is Success && !this.isEmpty)
}
