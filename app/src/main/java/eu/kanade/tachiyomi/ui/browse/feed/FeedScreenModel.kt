package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.util.fastAny
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.FeedItemUI
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.source.interactor.CountFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.DeleteFeedSavedSearchById
import tachiyomi.domain.source.interactor.GetFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.GetSavedSearchBySourceId
import tachiyomi.domain.source.interactor.GetSavedSearchGlobalFeed
import tachiyomi.domain.source.interactor.InsertFeedSavedSearch
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import xyz.nulldev.ts.api.http.serializer.FilterSerializer
import java.util.concurrent.Executors
import tachiyomi.domain.manga.model.Manga as DomainManga

// Presenter of [feedTab].
private const val MAX_FEEDS = 10

internal open class FeedScreenModel(
    val sourceManager: SourceManager = Injekt.get(),
    val sourcePreferences: SourcePreferences = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val getFeedSavedSearchGlobal: GetFeedSavedSearchGlobal = Injekt.get(),
    private val getSavedSearchGlobalFeed: GetSavedSearchGlobalFeed = Injekt.get(),
    private val countFeedSavedSearchGlobal: CountFeedSavedSearchGlobal = Injekt.get(),
    internal val getSavedSearchBySourceId: GetSavedSearchBySourceId = Injekt.get(),
    internal val insertFeedSavedSearch: InsertFeedSavedSearch = Injekt.get(),
    internal val deleteFeedSavedSearchById: DeleteFeedSavedSearchById = Injekt.get(),
) : StateScreenModel<FeedScreenState>(FeedScreenState()) {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    private val coroutineDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    var pushed: Boolean = false

    init {
        getFeedSavedSearchGlobal.subscribe()
            .distinctUntilChanged()
            .onEach {
                sourceManager.isInitialized.first { it }
                val items = getSourcesToGetFeed(it).map { (feed, savedSearch) ->
                    createCatalogueSearchItem(
                        feed = feed,
                        savedSearch = savedSearch,
                        source = sourceManager.get(feed.source),
                        results = null,
                    )
                }
                mutableState.update { state ->
                    state.copy(
                        items = items,
                    )
                }
                getFeed(items)
            }
            .catch { _events.send(Event.FailedFetchingSources) }
            .launchIn(screenModelScope)
    }

    private val filterSerializer = FilterSerializer()

    /** Emits [event] to the tab; the extension files reach the private channel through it. */
    internal suspend fun emit(event: Event) = _events.send(event)

    /** Applies [func] to the state; the extension files reach the protected flow through it. */
    internal fun updateState(func: (FeedScreenState) -> FeedScreenState) {
        mutableState.update(func)
    }

    fun init() {
        pushed = false
        screenModelScope.launchIO {
            val newItems = state.value.items?.map { it.copy(results = null) }
            if (newItems != null) {
                mutableState.update { state ->
                    state.copy(
                        items = newItems,
                    )
                }
                getFeed(newItems)
            }
        }
    }

    internal suspend fun hasTooManyFeeds(): Boolean = countFeedSavedSearchGlobal.await() > MAX_FEEDS

    private suspend fun getSourcesToGetFeed(
        feedSavedSearch: List<FeedSavedSearch>,
    ): List<Pair<FeedSavedSearch, SavedSearch?>> {
        val savedSearches = getSavedSearchGlobalFeed.await()
            .associateBy { it.id }
        return feedSavedSearch
            .map { it to savedSearches[it.savedSearch] }
    }

    // Creates a catalogue search item.
    private fun createCatalogueSearchItem(
        feed: FeedSavedSearch,
        savedSearch: SavedSearch?,
        source: Source?,
        results: List<DomainManga>?,
    ): FeedItemUI {
        return FeedItemUI(
            feed,
            savedSearch,
            source,
            savedSearch?.name ?: (source?.name ?: feed.source.toString()),
            if (savedSearch != null) {
                source?.name ?: feed.source.toString()
            } else {
                LocaleHelper.getLocalizedDisplayName(source?.lang)
            },
            results,
        )
    }

    // Initiates get manga per feed.
    private fun getFeed(feedSavedSearch: List<FeedItemUI>) {
        screenModelScope.launch {
            feedSavedSearch.map { itemUI ->
                async {
                    val page = withContext(coroutineDispatcher) { itemUI.fetchFirstPage(::getFilterList) }

                    val result = withIOContext {
                        itemUI.copy(
                            results = networkToLocalManga(page.map { it.toDomainManga(itemUI.source!!.id) }),
                        )
                    }

                    mutableState.update { state ->
                        state.copy(
                            items = state.items?.map { if (it.feed.id == result.feed.id) result else it },
                        )
                    }
                }
            }.awaitAll()
        }
    }

    private fun getFilterList(savedSearch: SavedSearch, source: Source): FilterList {
        val filters = savedSearch.filtersJson ?: return FilterList()
        return runCatching {
            val originalFilters = source.getFilterList()
            filterSerializer.deserialize(
                filters = originalFilters,
                json = Json.decodeFromString(filters),
            )
            originalFilters
        }.getOrElse { FilterList() }
    }

    @Composable
    fun getManga(initialManga: DomainManga): State<DomainManga> {
        return produceState(initialValue = initialManga) {
            getManga.subscribe(initialManga.url, initialManga.source)
                .collectLatest { manga ->
                    if (manga != null) {
                        value = manga
                    }
                }
        }
    }
    override fun onDispose() {
        super.onDispose()
        coroutineDispatcher.close()
    }

    sealed class Dialog {
        data class AddFeed(val options: List<Source>) : Dialog()
        data class AddFeedSearch(val source: Source, val options: List<SavedSearch?>) : Dialog()
        data class DeleteFeed(val feed: FeedSavedSearch) : Dialog()
    }

    sealed class Event {
        data object FailedFetchingSources : Event()
        data object TooManyFeeds : Event()
    }
}

internal data class FeedScreenState(
    val dialog: FeedScreenModel.Dialog? = null,
    val items: List<FeedItemUI>? = null,
) {
    val isLoading
        get() = items == null

    val isEmpty
        get() = items.isNullOrEmpty()

    val isLoadingItems
        get() = items?.fastAny { it.results == null } != false
}

// The feed's first page: latest updates, or the saved search; nothing when the source is gone or the request fails.
internal suspend fun FeedItemUI.fetchFirstPage(filterList: (SavedSearch, Source) -> FilterList): List<SManga> {
    val source = source ?: return emptyList()
    return try {
        val page = if (savedSearch == null) {
            source.getLatestUpdates(1)
        } else {
            source.getSearchManga(1, savedSearch.query.orEmpty(), filterList(savedSearch, source))
        }
        page.mangas
    } catch (_: Exception) {
        // Any failure ends here and the fallback below applies.
        emptyList()
    }
}
