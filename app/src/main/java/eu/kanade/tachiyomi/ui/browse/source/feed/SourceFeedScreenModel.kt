package eu.kanade.tachiyomi.ui.browse.source.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.domain.source.interactor.GetExhSavedSearch
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.browse.SourceFeedUI
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import exh.source.mangaDexSourceIds
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.source.interactor.CountFeedSavedSearchBySourceId
import tachiyomi.domain.source.interactor.DeleteFeedSavedSearchById
import tachiyomi.domain.source.interactor.GetFeedSavedSearchBySourceId
import tachiyomi.domain.source.interactor.GetSavedSearchBySourceIdFeed
import tachiyomi.domain.source.interactor.InsertFeedSavedSearch
import tachiyomi.domain.source.model.EXHSavedSearch
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import xyz.nulldev.ts.api.http.serializer.FilterSerializer
import java.util.concurrent.Executors
import tachiyomi.domain.manga.model.Manga as DomainManga

private const val FEED_THREADS = 5
private const val MAX_FEEDS = 10

internal open class SourceFeedScreenModel(
    val sourceId: Long,
    uiPreferences: UiPreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val getFeedSavedSearchBySourceId: GetFeedSavedSearchBySourceId = Injekt.get(),
    private val getSavedSearchBySourceIdFeed: GetSavedSearchBySourceIdFeed = Injekt.get(),
    private val countFeedSavedSearchBySourceId: CountFeedSavedSearchBySourceId = Injekt.get(),
    private val insertFeedSavedSearch: InsertFeedSavedSearch = Injekt.get(),
    private val deleteFeedSavedSearchById: DeleteFeedSavedSearchById = Injekt.get(),
    private val getExhSavedSearch: GetExhSavedSearch = Injekt.get(),
) : StateScreenModel<SourceFeedState>(SourceFeedState()) {

    val source = sourceManager.getOrStub(sourceId)

    val sourceIsMangaDex = sourceId in mangaDexSourceIds

    private val coroutineDispatcher = Executors.newFixedThreadPool(FEED_THREADS).asCoroutineDispatcher()

    val startExpanded by uiPreferences.expandFilters.asState(screenModelScope)

    init {
        setFilters(source.getFilterList())

        screenModelScope.launchIO {
            val searches = loadSearches()
            mutableState.update { it.copy(savedSearches = searches) }
        }

        getFeedSavedSearchBySourceId.subscribe(source.id)
            .onEach {
                val items = getSourcesToGetFeed(it)
                mutableState.update { state ->
                    state.copy(
                        items = items,
                    )
                }
                getFeed(items)
            }
            .launchIn(screenModelScope)
    }

    internal val filterSerializer = FilterSerializer()

    /** Applies [func] to the state; the extension files reach the protected flow through it. */
    internal fun updateState(func: (SourceFeedState) -> SourceFeedState) {
        mutableState.update(func)
    }

    fun setFilters(filters: FilterList) {
        mutableState.update { it.copy(filters = filters) }
    }

    internal suspend fun hasTooManyFeeds(): Boolean = countFeedSavedSearchBySourceId.await(source.id) > MAX_FEEDS

    fun createFeed(savedSearchId: Long) {
        screenModelScope.launchNonCancellable {
            insertFeedSavedSearch.await(
                FeedSavedSearch(
                    id = -1,
                    source = source.id,
                    savedSearch = savedSearchId,
                    global = false,
                ),
            )
        }
    }

    fun deleteFeed(feed: FeedSavedSearch) {
        screenModelScope.launchNonCancellable {
            deleteFeedSavedSearchById.await(feed.id)
        }
    }

    private suspend fun getSourcesToGetFeed(feedSavedSearch: List<FeedSavedSearch>): List<SourceFeedUI> {
        val savedSearches = getSavedSearchBySourceIdFeed.await(source.id)
            .associateBy { it.id }

        val fixed = listOfNotNull(
            if (source.supportsLatest) SourceFeedUI.Latest(null) else null,
            SourceFeedUI.Browse(null),
        )
        return fixed + feedSavedSearch.map { SourceFeedUI.SourceSavedSearch(it, savedSearches[it.savedSearch]!!, null) }
    }

    // Initiates get manga per feed.
    private fun getFeed(feedSavedSearch: List<SourceFeedUI>) {
        screenModelScope.launch {
            feedSavedSearch.map { sourceFeed ->
                async {
                    val page = try {
                        withContext(coroutineDispatcher) {
                            when (sourceFeed) {
                                is SourceFeedUI.Browse -> source.getPopularManga(1)
                                is SourceFeedUI.Latest -> source.getLatestUpdates(1)
                                is SourceFeedUI.SourceSavedSearch -> source.getSearchManga(
                                    page = 1,
                                    query = sourceFeed.savedSearch.query.orEmpty(),
                                    filters = getFilterList(sourceFeed.savedSearch, source),
                                )
                            }
                        }.mangas
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val titles = withIOContext {
                        networkToLocalManga(page.map { it.toDomainManga(source.id) })
                    }

                    mutableState.update { state ->
                        state.copy(
                            items = state.items.map { item ->
                                if (item.id == sourceFeed.id) sourceFeed.withResults(titles) else item
                            },
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
                    if (manga == null) return@collectLatest
                    value = manga
                }
        }
    }
    private suspend fun loadSearches() =
        getExhSavedSearch.await(source.id, source::getFilterList)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, EXHSavedSearch::name))

    sealed class Dialog {
        data object Filter : Dialog()
        data class DeleteFeed(val feed: FeedSavedSearch) : Dialog()
        data class AddFeed(val feedId: Long, val name: String) : Dialog()
    }

    override fun onDispose() {
        super.onDispose()
        coroutineDispatcher.close()
    }
}

@Immutable
internal data class SourceFeedState(
    val searchQuery: String? = null,
    val items: List<SourceFeedUI> = emptyList(),
    val filters: FilterList = FilterList(),
    val savedSearches: List<EXHSavedSearch> = emptyList(),
    val dialog: SourceFeedScreenModel.Dialog? = null,
) {
    val isLoading
        get() = items.isEmpty()
}
