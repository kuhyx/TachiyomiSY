package eu.kanade.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.library.service.LibraryPreferences

internal class GetSortTag(private val preferences: LibraryPreferences) {

    fun subscribe(): Flow<List<String>> {
        return preferences.sortTagsForLibrary.changes()
            .map(::mapSortTags)
    }

    fun await() = getSortTags(preferences).let(::mapSortTags)

    companion object {
        fun getSortTags(preferences: LibraryPreferences) = preferences.sortTagsForLibrary.get()

        fun mapSortTags(tags: Set<String>) = tags.mapNotNull { tag ->
            val index = tag.indexOf('|')
            val order = if (index != -1) tag.substring(0, index).toIntOrNull() else null
            order?.let { it to tag.substring(index + 1) }
        }
            .sortedBy { it.first }
            .map { it.second }
    }
}
