package eu.kanade.domain.source.interactor

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class GetSourceCategories(
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<String>> =
        preferences.sourcesTabCategories.changes().map { it.sortedWith(String.CASE_INSENSITIVE_ORDER) }
}
