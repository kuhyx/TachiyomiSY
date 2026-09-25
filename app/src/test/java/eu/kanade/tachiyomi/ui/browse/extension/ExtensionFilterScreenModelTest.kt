package eu.kanade.tachiyomi.ui.browse.extension

import eu.kanade.domain.extension.interactor.GetExtensionLanguages
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExtensionFilterScreenModelTest {
    private val koin = BrowseKoin()
    private val getLanguages = mockk<GetExtensionLanguages>()
    private val toggle = mockk<ToggleLanguage>(relaxed = true)

    @Before
    fun setUp() = koin.start(
        module {
            single { getLanguages }
            single { toggle }
        },
    )

    @After
    fun tearDown() = koin.stop()

    @Test
    fun languagesAreListed() {
        every { getLanguages.subscribe() } returns MutableStateFlow(listOf("en", "fr"))
        koin.sourcePreferences.enabledLanguages.set(setOf("en"))
        val model = ExtensionFilterScreenModel()
        eventually { model.state.value is ExtensionFilterState.Success }
        val state = model.state.value as ExtensionFilterState.Success
        state.languages shouldBe listOf("en", "fr")
        state.enabledLanguages shouldBe setOf("en")
        state.isEmpty shouldBe false
        ExtensionFilterState.Success(emptyList()).isEmpty shouldBe true
        model.toggle("fr")
        verify { toggle.await("fr") }
    }

    @Test
    fun failuresAreReported() {
        every { getLanguages.subscribe() } returns flow { error("offline") }
        val model = ExtensionFilterScreenModel()
        val event = runBlocking { withTimeout(10_000L) { model.events.first() } }
        event shouldBe ExtensionFilterEvent.FailedFetchingLanguages
        model.state.value shouldBe ExtensionFilterState.Loading
    }
}
