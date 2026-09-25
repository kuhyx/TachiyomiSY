package eu.kanade.domain.extension.interactor

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetExtensionLanguagesTest {

    private val preferences = SourcePreferences(FlowPreferenceStore())
    private val extensionManager = mockk<ExtensionManager>()
    private val interactor = GetExtensionLanguages(preferences, extensionManager)

    @Test
    fun enabledLanguagesFirstThenName() = runTest {
        every { extensionManager.availableExtensionsFlow } returns MutableStateFlow(
            listOf(
                available("a", sources = listOf(1L to "fr", 2L to "en", 3L to "all")),
                available("b", sources = listOf(4L to "de", 5L to "en")),
            ),
        )
        preferences.enabledLanguages.set(setOf("de"))
        interactor.subscribe().first() shouldBe listOf("de", "all", "en", "fr")
    }
}
