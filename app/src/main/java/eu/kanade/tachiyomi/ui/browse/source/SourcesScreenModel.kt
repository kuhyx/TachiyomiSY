package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.domain.source.interactor.GetShowLatest
import eu.kanade.domain.source.interactor.GetSourceCategories
import eu.kanade.domain.source.interactor.SetSourceCategories
import eu.kanade.domain.source.interactor.ToggleExcludeFromDataSaver
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.source.service.SourcePreferences.DataSaver
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.browse.SourceUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.contains
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.TreeMap

internal class SourcesScreenModel(
    private val getEnabledSources: GetEnabledSources = Injekt.get(),
    private val toggleSource: ToggleSource = Injekt.get(),
    private val toggleSourcePin: ToggleSourcePin = Injekt.get(),
    // SY -->
    private val uiPreferences: UiPreferences = Injekt.get(),
    private val getSourceCategories: GetSourceCategories = Injekt.get(),
    private val getShowLatest: GetShowLatest = Injekt.get(),
    private val toggleExcludeFromDataSaver: ToggleExcludeFromDataSaver = Injekt.get(),
    private val setSourceCategories: SetSourceCategories = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    val smartSearchConfig: SourcesScreen.SmartSearchConfig?,
    // SY <--
) : StateScreenModel<SourcesScreenModel.State>(State()) {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    val useNewSourceNavigation by uiPreferences.useNewSourceNavigation.asState(screenModelScope)

    init {
        // SY -->
        combine(
            getEnabledSources.subscribe(),
            getSourceCategories.subscribe(),
            getShowLatest.subscribe(smartSearchConfig != null),
            flowOf(smartSearchConfig == null),
            ::collectLatestSources,
        )
            .catch {
                logcat(LogPriority.ERROR, it)
                _events.send(Event.FailedFetchingSources)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(screenModelScope)

        sourcePreferences.dataSaver.changes()
            .onEach {
                mutableState.update {
                    it.copy(
                        dataSaverEnabled = sourcePreferences.dataSaver.get() != DataSaver.NONE,
                    )
                }
            }
            .launchIn(screenModelScope)
        // SY <--
    }

    private fun collectLatestSources(
        sources: List<Source>,
        categories: List<String>,
        showLatest: Boolean,
        showPin: Boolean,
    ) {
        mutableState.update { state ->
            val map = TreeMap<String, MutableList<Source>>(SECTION_ORDER)
            val byLang = sources.groupByTo(map) {
                when {
                    // SY -->
                    it.category != null -> "$CATEGORY_KEY_PREFIX${it.category}"
                    // SY <--
                    it.isUsedLast -> LAST_USED_KEY
                    Pin.Actual in it.pin -> PINNED_KEY
                    else -> it.lang
                }
            }

            state.copy(
                isLoading = false,
                items = byLang
                    .flatMap {
                        val header = SourceUiModel.Header(
                            it.key.removePrefix(CATEGORY_KEY_PREFIX),
                            it.value.firstOrNull()?.category != null,
                        )
                        listOf(header) + it.value.map { source -> SourceUiModel.Item(source) }
                    },
                // SY -->
                categories = categories
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it }),
                showPin = showPin,
                showLatest = showLatest,
                // SY <--
            )
        }
    }

    fun toggleSource(source: Source) {
        toggleSource.await(source)
    }

    fun togglePin(source: Source) {
        toggleSourcePin.await(source)
    }

    // SY -->
    fun toggleExcludeFromDataSaver(source: Source) {
        toggleExcludeFromDataSaver.await(source)
    }

    fun setSourceCategories(source: Source, categories: List<String>) {
        setSourceCategories.await(source, categories)
    }

    fun showSourceCategoriesDialog(source: Source) {
        mutableState.update { it.copy(dialog = Dialog.SourceCategories(source)) }
    }
    // SY <--

    fun showSourceDialog(source: Source) {
        mutableState.update { it.copy(dialog = Dialog.SourceLongClick(source)) }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    sealed interface Event {
        data object FailedFetchingSources : Event
    }

    sealed class Dialog {
        data class SourceLongClick(val source: Source) : Dialog()
        data class SourceCategories(val source: Source) : Dialog()
    }

    @Immutable
    data class State(
        val dialog: Dialog? = null,
        val isLoading: Boolean = true,
        val items: List<SourceUiModel> = emptyList(),
        // SY -->
        val categories: List<String> = emptyList(),
        val showPin: Boolean = true,
        val showLatest: Boolean = false,
        val dataSaverEnabled: Boolean = false,
        // SY <--
    ) {
        val isEmpty = items.isEmpty()
    }

    companion object {
        const val PINNED_KEY = "pinned"
        const val LAST_USED_KEY = "last_used"

        // SY -->
        const val CATEGORY_KEY_PREFIX = "category-"
        // SY <--
    }
}

// Section order: last used, pinned, categories, then languages alphabetically with the unnamed one last.
private enum class Section {
    LAST_USED,
    PINNED,
    CATEGORY,
    LANGUAGE,
    NO_LANGUAGE,
}

private fun sectionOf(key: String): Section = when {
    key == SourcesScreenModel.LAST_USED_KEY -> Section.LAST_USED
    key == SourcesScreenModel.PINNED_KEY -> Section.PINNED
    // SY -->
    key.startsWith(SourcesScreenModel.CATEGORY_KEY_PREFIX) -> Section.CATEGORY
    // SY <--
    key == "" -> Section.NO_LANGUAGE
    else -> Section.LANGUAGE
}

private val SECTION_ORDER: Comparator<String> = compareBy<String>(::sectionOf).thenBy { it }
