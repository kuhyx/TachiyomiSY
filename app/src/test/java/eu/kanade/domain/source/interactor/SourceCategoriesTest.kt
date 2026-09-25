package eu.kanade.domain.source.interactor

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.source.service.SourcePreferences
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** The source-category interactors share one preference pair, so they are exercised together. */
internal class SourceCategoriesTest {

    private val preferences = SourcePreferences(FlowPreferenceStore())

    @Test
    fun createRejectsPipes() {
        val create = CreateSourceCategory(preferences)
        create.await("a|b") shouldBe CreateSourceCategory.Result.InvalidName
        create.await("Fav") shouldBe CreateSourceCategory.Result.Success
        preferences.sourcesTabCategories.get() shouldBe setOf("Fav")
    }

    @Test
    fun deleteRemovesCategoryAndRows() {
        preferences.sourcesTabCategories.set(setOf("Fav", "Other"))
        preferences.sourcesTabSourcesInCategories.set(setOf("1|Fav", "2|Other", "3|Fav"))
        DeleteSourceCategory(preferences).await("Fav")
        preferences.sourcesTabCategories.get() shouldBe setOf("Other")
        preferences.sourcesTabSourcesInCategories.get() shouldBe setOf("2|Other")
    }

    @Test
    fun getSortsCaseInsensitively() = runTest {
        preferences.sourcesTabCategories.set(setOf("beta", "Alpha", "gamma"))
        GetSourceCategories(preferences).subscribe().first() shouldBe listOf("Alpha", "beta", "gamma")
    }

    @Test
    fun setReplacesMemberships() {
        preferences.sourcesTabSourcesInCategories.set(setOf("1|Fav", "2|Fav"))
        SetSourceCategories(preferences).await(source(1), listOf("Other", "New"))
        preferences.sourcesTabSourcesInCategories.get() shouldBe setOf("2|Fav", "1|Other", "1|New")
    }

    @Test
    fun renameKeepsInvalidNamesOut() {
        preferences.sourcesTabCategories.set(setOf("Fav"))
        val rename = RenameSourceCategory(preferences, CreateSourceCategory(preferences))
        rename.await("Fav", "a|b") shouldBe CreateSourceCategory.Result.InvalidName
        preferences.sourcesTabCategories.get() shouldBe setOf("Fav")
    }

    @Test
    fun renameMovesTheSources() {
        preferences.sourcesTabCategories.set(setOf("Fav", "Other"))
        preferences.sourcesTabSourcesInCategories.set(setOf("1|Fav", "2|Other", "broken"))
        val rename = RenameSourceCategory(preferences, CreateSourceCategory(preferences))
        rename.await("Fav", "Best") shouldBe CreateSourceCategory.Result.Success
        preferences.sourcesTabCategories.get() shouldBe setOf("Other", "Best")
        preferences.sourcesTabSourcesInCategories.get() shouldBe setOf("1|Best", "2|Other", "broken")
    }
}
