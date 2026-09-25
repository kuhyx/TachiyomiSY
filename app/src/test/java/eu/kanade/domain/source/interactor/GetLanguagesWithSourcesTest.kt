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
import tachiyomi.domain.source.repository.SourceRepository

internal class GetLanguagesWithSourcesTest {

    private val preferences = SourcePreferences(FlowPreferenceStore())
    private val repository = mockk<SourceRepository>()
    private val interactor = GetLanguagesWithSources(repository, preferences)

    @Test
    fun groupsByLanguageEnabledFirst() = runTest {
        every { repository.getOnlineSources() } returns flowOf(
            listOf(
                source(1, name = "beta", lang = "fr"),
                source(2, name = "Alpha", lang = "fr"),
                source(3, name = "disabled", lang = "fr"),
                source(4, name = "multi", lang = "all"),
                source(5, name = "english", lang = "en"),
                source(MERGED_SOURCE_ID, name = "merged", lang = "en"),
            ),
        )
        preferences.enabledLanguages.set(setOf("fr"))
        preferences.disabledSources.set(setOf("2"))
        val result = interactor.subscribe().first()
        result.keys.toList() shouldBe listOf("fr", "all", "en")
        result.getValue("fr").map { it.name } shouldBe listOf("beta", "disabled", "Alpha")
        result.getValue("en").map { it.id } shouldBe listOf(5L)
    }
}
