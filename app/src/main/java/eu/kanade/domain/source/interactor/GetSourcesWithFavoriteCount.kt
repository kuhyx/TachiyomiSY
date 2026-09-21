package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.core.common.util.lang.compareToWithCollator
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.source.local.isLocal

internal class GetSourcesWithFavoriteCount(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<Pair<Source, Long>>> {
        return combine(
            preferences.migrationSortingDirection.changes(),
            preferences.migrationSortingMode.changes(),
            repository.getSourcesWithFavoriteCount(),
        ) { direction, mode, list ->
            list
                .filterNot { it.first.isLocal() }
                .sortedWith(sortFn(direction, mode))
        }
    }

    private fun sortFn(
        direction: SetMigrateSorting.Direction,
        sorting: SetMigrateSorting.Mode,
    ): Comparator<Pair<Source, Long>> {
        val byMode: Comparator<Pair<Source, Long>> = when (sorting) {
            SetMigrateSorting.Mode.ALPHABETICAL -> Comparator { a, b ->
                a.first.name.lowercase().compareToWithCollator(b.first.name.lowercase())
            }
            SetMigrateSorting.Mode.TOTAL -> compareBy { it.second }
        }
        // Stubs (uninstalled sources) sort first ascending, so last descending.
        val stubsFirst = compareByDescending<Pair<Source, Long>> { it.first.isStub }.then(byMode)
        return when (direction) {
            SetMigrateSorting.Direction.ASCENDING -> stubsFirst
            SetMigrateSorting.Direction.DESCENDING -> stubsFirst.reversed()
        }
    }
}
