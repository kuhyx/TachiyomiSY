package eu.kanade.domain.source.interactor

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.source.service.SourcePreferences
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Pins
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.minus
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.source.local.LocalSource

internal class GetEnabledSourcesTest {

    private val preferences = SourcePreferences(FlowPreferenceStore())
    private val repository = mockk<SourceRepository>()
    private val interactor = GetEnabledSources(repository, preferences)

    private val sources = listOf(
        source(2, name = "beta"),
        source(1, name = "Alpha"),
        source(3, name = "gamma", lang = "fr"),
        source(4, name = "hidden"),
        source(MERGED_SOURCE_ID, name = "merged"),
        source(LocalSource.ID, name = "Local", lang = "other"),
    )

    @Test
    fun filtersAndSortsByName() = runTest {
        every { repository.getSources() } returns flowOf(sources)
        preferences.enabledLanguages.set(setOf("en"))
        preferences.disabledSources.set(setOf("4"))
        interactor.subscribe().first().map { it.name } shouldBe listOf("Alpha", "beta", "Local")
    }

    @Test
    fun pinnedLastUsedCategoriesFanOut() = runTest {
        every { repository.getSources() } returns flowOf(sources.take(2))
        preferences.enabledLanguages.set(setOf("en"))
        preferences.pinnedSources.set(setOf("1"))
        preferences.lastUsedSource.set(2)
        preferences.dataSaverExcludedSources.set(setOf("2"))
        preferences.sourcesTabSourcesInCategories.set(setOf("1|Fav", "2|Fav", "2|Other"))
        val result = interactor.subscribe().first()
        result.map { Triple(it.id, it.category, it.isUsedLast) } shouldBe listOf(
            Triple(1L, null, false),
            Triple(1L, "Fav", false),
            Triple(2L, null, false),
            Triple(2L, null, true),
            Triple(2L, "Fav", false),
            Triple(2L, "Other", false),
        )
        result[0].pin shouldBe Pins.pinned
        result[1].pin shouldBe Pins.pinned - Pin.Actual
        result[0].categories shouldBe setOf("Fav")
        result[2].pin shouldBe Pins.unpinned
        result[2].isExcludedFromDataSaver shouldBe true
        result[3].pin shouldBe Pins.unpinned - Pin.Actual
    }

    @Test
    fun categoryFilterHidesPlainRows() = runTest {
        every { repository.getSources() } returns flowOf(sources.take(3))
        preferences.enabledLanguages.set(setOf("en", "fr"))
        preferences.pinnedSources.set(setOf("2"))
        preferences.sourcesTabSourcesInCategories.set(setOf("1|Fav", "2|Fav"))
        preferences.sourcesTabCategoriesFilter.set(true)
        val result: List<Source> = interactor.subscribe().first()
        result.map { it.id to it.category } shouldBe listOf(
            1L to "Fav",
            2L to null,
            2L to "Fav",
            3L to null,
        )
    }
}
