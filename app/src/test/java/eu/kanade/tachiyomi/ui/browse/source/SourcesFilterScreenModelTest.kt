package eu.kanade.tachiyomi.ui.browse.source

import eu.kanade.domain.source.interactor.GetLanguagesWithSources
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.model.Source
import java.util.SortedMap

@RunWith(RobolectricTestRunner::class)
internal class SourcesFilterScreenModelTest {
    private val koin = BrowseKoin()
    private val getLanguages = mockk<GetLanguagesWithSources>()
    private val toggleSource = mockk<ToggleSource>(relaxed = true)
    private val toggleLanguage = mockk<ToggleLanguage>(relaxed = true)

    @Before
    fun setUp() = koin.start(
        module {
            single { getLanguages }
            single { toggleSource }
            single { toggleLanguage }
        },
    )

    @After
    fun tearDown() = koin.stop()

    @Test
    fun languagesAreListed() {
        val items: SortedMap<String, List<Source>> = sortedMapOf("en" to listOf(source(1L)))
        every { getLanguages.subscribe() } returns MutableStateFlow(items)
        koin.sourcePreferences.disabledSources.set(setOf("2"))
        val model = SourcesFilterScreenModel()
        eventually { model.state.value is SourcesFilterScreenModel.State.Success }
        val state = model.state.value as SourcesFilterScreenModel.State.Success
        state.items shouldBe items
        state.disabledSources shouldBe setOf("2")
        state.isEmpty shouldBe false
        state.copy(items = sortedMapOf()).isEmpty shouldBe true
    }

    @Test
    fun failuresBecomeErrors() {
        every { getLanguages.subscribe() } returns flow { error("db") }
        val model = SourcesFilterScreenModel()
        eventually { model.state.value is SourcesFilterScreenModel.State.Error }
        model.state.value.shouldBeInstanceOf<SourcesFilterScreenModel.State.Error>().throwable.message shouldBe "db"
    }

    @Test
    fun togglesReachTheInteractors() {
        every { getLanguages.subscribe() } returns MutableStateFlow(sortedMapOf())
        val model = SourcesFilterScreenModel()
        model.toggleSource(source(1L))
        model.toggleLanguage("en")
        model.toggleSources(enable = false, sources = listOf(source(1L), source(2L)))
        verify { toggleSource.await(source(1L)) }
        verify { toggleLanguage.await("en") }
        verify { toggleSource.await(listOf(1L, 2L), false) }
    }
}
