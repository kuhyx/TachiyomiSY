package mihon.feature.migration.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.flow.update
import mihon.feature.migration.config.MigrationConfigScreen.MigrationSource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class MigrationConfigScreenModel(
    val sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<MigrationConfigScreenModel.State>(State()) {

    private val sourcesComparator = { includedSources: List<Long> ->
        compareBy<MigrationSource>(
            { !it.isSelected },
            { includedSources.indexOf(it.id) },
            { with(it) { "$name ($shortLanguage)" } },
        )
    }

    init {
        screenModelScope.launchIO {
            initSources()
            mutableState.update { it.copy(isLoading = false) }
        }
    }

    private fun updateSources(action: (List<MigrationSource>) -> List<MigrationSource>) {
        mutableState.update { state ->
            val updatedSources = action(state.sources)
            val includedSources = updatedSources.mapNotNull { if (!it.isSelected) null else it.id }
            state.copy(sources = updatedSources.sortedWith(sourcesComparator(includedSources)))
        }
        saveSources()
    }

    private fun initSources() {
        val languages = sourcePreferences.enabledLanguages.get()
        val pinnedSources = sourcePreferences.pinnedSources.get().mapNotNull { it.toLongOrNull() }
        val includedSources = sourcePreferences.migrationSources.get()
        val disabledSources = sourcePreferences.disabledSources.get()
            .mapNotNull { it.toLongOrNull() }
        val sources = sourceManager.getAll()
            .asSequence()
            .filterIsInstance<HttpSource>()
            .filterNot { it.id == MERGED_SOURCE_ID }
            .filter { it.lang in languages }
            .map {
                val source = Source(
                    id = it.id,
                    lang = it.lang,
                    name = it.name,
                    supportsLatest = false,
                    isStub = false,
                )
                MigrationSource(
                    source = source,
                    isSelected = when {
                        includedSources.isNotEmpty() -> source.id in includedSources
                        pinnedSources.isNotEmpty() -> source.id in pinnedSources
                        else -> source.id !in disabledSources
                    },
                )
            }
            .toList()

        mutableState.update { state ->
            state.copy(sources = sources.sortedWith(sourcesComparator(includedSources)))
        }
    }

    fun toggleSelection(id: Long) {
        updateSources { sources ->
            sources.map { source ->
                source.copy(isSelected = if (source.source.id == id) !source.isSelected else source.isSelected)
            }
        }
    }

    fun toggleSelection(config: SelectionConfig) {
        val pinnedSources = sourcePreferences.pinnedSources.get().mapNotNull { it.toLongOrNull() }
        val disabledSources = sourcePreferences.disabledSources.get().mapNotNull { it.toLongOrNull() }
        val isSelected: (Long) -> Boolean = {
            when (config) {
                SelectionConfig.All -> true
                SelectionConfig.None -> false
                SelectionConfig.Pinned -> it in pinnedSources
                SelectionConfig.Enabled -> it !in disabledSources
            }
        }
        updateSources { sources ->
            sources.map { source ->
                source.copy(isSelected = isSelected(source.source.id))
            }
        }
    }

    fun orderSource(from: Int, to: Int) {
        updateSources {
            it.toMutableList()
                .apply {
                    add(to, removeAt(from))
                }
                .toList()
        }
    }

    fun saveSources() {
        state.value.sources
            .filter { source -> source.isSelected }
            .map { source -> source.source.id }
            .let { sources -> sourcePreferences.migrationSources.set(sources) }
    }

    data class State(
        val isLoading: Boolean = true,
        val sources: List<MigrationSource> = emptyList(),
    )

    enum class SelectionConfig {
        All,
        None,
        Pinned,
        Enabled,
    }
}
