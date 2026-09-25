package eu.kanade.tachiyomi.ui.browse.source

import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.domain.source.interactor.GetShowLatest
import eu.kanade.domain.source.interactor.GetSourceCategories
import eu.kanade.domain.source.interactor.SetSourceCategories
import eu.kanade.domain.source.interactor.ToggleExcludeFromDataSaver
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.SourceUiModel
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
import tachiyomi.domain.source.model.Pins
import tachiyomi.domain.source.model.Source

/** A catalogue source row with [id] in [lang]. */
internal fun source(id: Long, lang: String = "en", name: String = "Source $id"): Source =
    Source(id = id, lang = lang, name = name, supportsLatest = true, isStub = false)

@RunWith(RobolectricTestRunner::class)
internal class SourcesScreenModelTest {
    private val koin = BrowseKoin()
    private val sources = MutableStateFlow(emptyList<Source>())
    private val getEnabledSources = mockk<GetEnabledSources> { every { subscribe() } returns sources }
    private val categories = MutableStateFlow(listOf("b", "A"))
    private val toggleSource = mockk<ToggleSource>(relaxed = true)
    private val togglePin = mockk<ToggleSourcePin>(relaxed = true)
    private val toggleDataSaver = mockk<ToggleExcludeFromDataSaver>(relaxed = true)
    private val setCategories = mockk<SetSourceCategories>(relaxed = true)

    @Before
    fun setUp() = koin.start(
        module {
            single { getEnabledSources }
            single<GetSourceCategories> { mockk { every { subscribe() } returns categories } }
            single<GetShowLatest> { mockk { every { subscribe(any()) } answers { MutableStateFlow(firstArg()) } } }
            single { toggleSource }
            single { togglePin }
            single { toggleDataSaver }
            single { setCategories }
        },
    )

    @After
    fun tearDown() = koin.stop()

    private fun model(config: SourcesScreen.SmartSearchConfig? = null) =
        SourcesScreenModel(smartSearchConfig = config).also { model -> eventually { !model.state.value.isLoading } }

    @Test
    fun sectionsAreOrdered() {
        sources.value = listOf(
            source(1L, lang = "fr"),
            source(2L, lang = ""),
            source(3L).copy(isUsedLast = true),
            source(4L).copy(pin = Pins.pinned),
            source(5L).copy(category = "Fav"),
            source(6L, lang = "en"),
        )
        val state = model().state.value
        val headers = state.items.filterIsInstance<SourceUiModel.Header>()
        headers.map { it.language } shouldBe listOf("last_used", "pinned", "Fav", "en", "fr", "")
        headers.map { it.isCategory } shouldBe listOf(false, false, true, false, false, false)
        state.categories shouldBe listOf("A", "b")
        state.showPin shouldBe true
        state.showLatest shouldBe false
        state.isEmpty shouldBe false
    }

    @Test
    fun smartSearchHidesPins() {
        val state = model(SourcesScreen.SmartSearchConfig("Needle", 1L)).state.value
        state.showPin shouldBe false
        state.showLatest shouldBe true
        state.isEmpty shouldBe true
    }

    @Test
    fun failuresAreReported() {
        every { getEnabledSources.subscribe() } returns flow { error("db") }
        val model = SourcesScreenModel(smartSearchConfig = null)
        val event = runBlocking { withTimeout(10_000L) { model.events.first() } }
        event shouldBe SourcesScreenModel.Event.FailedFetchingSources
    }

    @Test
    fun actionsReachTheInteractors() {
        val model = model()
        val row = source(1L)
        model.toggleSource(row)
        model.togglePin(row)
        model.toggleExcludeFromDataSaver(row)
        model.setSourceCategories(row, listOf("A"))
        verify { toggleSource.await(row) }
        verify { togglePin.await(row) }
        verify { toggleDataSaver.await(row) }
        verify { setCategories.await(row, listOf("A")) }
    }

    @Test
    fun dialogsOpenAndClose() {
        val model = model()
        model.showSourceDialog(source(1L))
        model.state.value.dialog shouldBe SourcesScreenModel.Dialog.SourceLongClick(source(1L))
        model.showSourceCategoriesDialog(source(1L))
        model.state.value.dialog shouldBe SourcesScreenModel.Dialog.SourceCategories(source(1L))
        model.closeDialog()
        model.state.value.dialog shouldBe null
    }

    @Test
    fun dataSaverFlagFollowsThePreference() {
        val model = model()
        model.useNewSourceNavigation shouldBe koin.uiPreferences.useNewSourceNavigation.get()
        koin.sourcePreferences.dataSaver.set(SourcePreferences.DataSaver.BANDWIDTH_HERO)
        eventually { model.state.value.dataSaverEnabled }
        koin.sourcePreferences.dataSaver.set(SourcePreferences.DataSaver.NONE)
        eventually { !model.state.value.dataSaverEnabled }
    }
}
