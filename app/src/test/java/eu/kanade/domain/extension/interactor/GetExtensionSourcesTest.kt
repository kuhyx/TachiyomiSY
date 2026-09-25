package eu.kanade.domain.extension.interactor

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetExtensionSourcesTest {

    private val preferences = SourcePreferences(FlowPreferenceStore())
    private val interactor = GetExtensionSources(preferences)

    private fun source(id: Long, name: String): Source {
        val source = mockk<Source>()
        every { source.id } returns id
        every { source.name } returns name
        return source
    }

    @Test
    fun singleSourceIsNeverLabelled() = runTest {
        val extension = installed("one").copy(sources = listOf(source(1, "One")))
        preferences.disabledSources.set(setOf("1"))
        val items = interactor.subscribe(extension).first()
        items shouldBe listOf(
            ExtensionSourceItem(source = extension.sources[0], enabled = false, labelAsName = false),
        )
    }

    @Test
    fun multiSourceIsLabelledByName() = runTest {
        val extension = installed("multi").copy(sources = listOf(source(1, "One"), source(2, "Two")))
        val items = interactor.subscribe(extension).first()
        items.map { it.enabled } shouldBe listOf(true, true)
        items.map { it.labelAsName } shouldBe listOf(true, true)
    }

    @Test
    fun multiLanguageSingleSourceIsNot() = runTest {
        val extension = installed("langs").copy(sources = listOf(source(1, "Same"), source(2, "Same")))
        interactor.subscribe(extension).first().all { !it.labelAsName } shouldBe true
    }
}
