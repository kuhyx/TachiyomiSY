package eu.kanade.domain.manga.interactor

import eu.kanade.domain.FlowPreferenceStore
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.service.LibraryPreferences

/** The sort-tag interactors share one preference, so they are exercised together. */
internal class SortTagsTest {

    private val preferences = LibraryPreferences(FlowPreferenceStore())
    private val getSortTag = GetSortTag(preferences)

    @Test
    fun getDecodesAndOrdersTags() = runTest {
        preferences.sortTagsForLibrary.set(setOf("2|c", "0|a", "1|b", "x|broken", "plain", "|empty"))
        getSortTag.await() shouldBe listOf("a", "b", "c")
        getSortTag.subscribe().first() shouldBe listOf("a", "b", "c")
        GetSortTag.getSortTags(preferences).size shouldBe 6
    }

    @Test
    fun createRejectsDuplicates() {
        val create = CreateSortTag(preferences, getSortTag)
        create.await(" a ") shouldBe CreateSortTag.Result.Success
        create.await("a") shouldBe CreateSortTag.Result.TagExists
        create.await("b") shouldBe CreateSortTag.Result.Success
        preferences.sortTagsForLibrary.get() shouldBe setOf("0|a", "1|b")
        CreateSortTag.encodeTag(3, " z ") shouldBe "3|z"
    }

    @Test
    fun deleteReindexesTheRest() {
        preferences.sortTagsForLibrary.set(setOf("0|a", "1|b", "2|c"))
        DeleteSortTag(preferences, getSortTag).await("b")
        preferences.sortTagsForLibrary.get() shouldBe setOf("0|a", "1|c")
    }

    @Test
    fun reorderMovesATag() {
        val reorder = ReorderSortTag(preferences, getSortTag)
        preferences.sortTagsForLibrary.set(setOf("0|a", "1|b", "2|c"))
        reorder.await("missing", 0) shouldBe ReorderSortTag.Result.InternalError
        reorder.await("b", 1) shouldBe ReorderSortTag.Result.Unchanged
        reorder.await("c", 0) shouldBe ReorderSortTag.Result.Success
        preferences.sortTagsForLibrary.get() shouldBe setOf("0|c", "1|a", "2|b")
    }
}
