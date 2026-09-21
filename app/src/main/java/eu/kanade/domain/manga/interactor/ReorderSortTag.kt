package eu.kanade.domain.manga.interactor

import tachiyomi.domain.library.service.LibraryPreferences

internal class ReorderSortTag(
    private val preferences: LibraryPreferences,
    private val getSortTag: GetSortTag,
) {

    fun await(tag: String, newPosition: Int): Result {
        val tags = getSortTag.await()
        val currentIndex = tags.indexOfFirst { it == tag }
        return when {
            currentIndex == -1 -> Result.InternalError
            currentIndex == newPosition -> Result.Unchanged
            else -> move(tags, from = currentIndex, to = newPosition)
        }
    }

    private fun move(tags: List<String>, from: Int, to: Int): Result {
        val reorderedTags = tags.toMutableList()
        val reorderedTag = reorderedTags.removeAt(from)
        reorderedTags.add(to, reorderedTag)

        preferences.sortTagsForLibrary.set(
            reorderedTags.mapIndexed { index, s ->
                CreateSortTag.encodeTag(index, s)
            }.toSet(),
        )

        return Result.Success
    }

    sealed class Result {
        data object Success : Result()
        data object Unchanged : Result()
        data object InternalError : Result()
    }
}
