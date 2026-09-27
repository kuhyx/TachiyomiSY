package mihon.feature.migration.config

import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mihon.feature.migration.config.MigrationConfigScreenModel.SelectionConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.Source

internal class MigrationConfigScreenModelTest {
    private val harness = MigrationConfigHarness()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        harness.start()
        harness.sources = {
            listOf(
                httpSource(3L, name = "Gamma"),
                httpSource(1L, name = "Alpha"),
                httpSource(2L, name = "Beta", lang = "ja"),
                httpSource(4L, name = "French", lang = "fr"),
                httpSource(MERGED_SOURCE_ID, name = "Merged"),
                plainSource(5L),
            )
        }
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
        Dispatchers.resetMain()
    }

    @Test
    fun enabledSourcesAreTheDefault() {
        harness.preferences.disabledSources.set(setOf("2", "junk"))
        val state = harness.model().awaitLoaded()
        state.sources.map { it.id } shouldContainExactly listOf(1L, 3L, 2L)
        state.selectedIds() shouldContainExactly listOf(1L, 3L)
        state.sources.map { it.shortLanguage } shouldContainExactly listOf("en", "en", "ja")
        state.sources.map { it.name } shouldContainExactly listOf("Alpha", "Gamma", "Beta")
    }

    @Test
    fun pinnedSourcesComeNext() {
        harness.preferences.pinnedSources.set(setOf("2", "junk"))
        val state = harness.model().awaitLoaded()
        state.selectedIds() shouldContainExactly listOf(2L)
    }

    @Test
    fun savedSourcesWinInTheirOrder() {
        harness.preferences.pinnedSources.set(setOf("2"))
        harness.preferences.migrationSources.set(listOf(3L, 1L))
        val state = harness.model().awaitLoaded()
        state.selectedIds() shouldContainExactly listOf(3L, 1L)
        state.sources.last().id shouldBe 2L
    }

    @Test
    fun togglingAnIdFlipsOnlyIt() {
        val model = harness.model()
        model.awaitLoaded()
        model.toggleSelection(2L)
        model.state.value.selectedIds() shouldContainExactly listOf(1L, 3L)
        model.toggleSelection(2L)
        model.state.value.selectedIds() shouldContainExactly listOf(1L, 3L, 2L)
        model.toggleSelection(1L)
        model.state.value.selectedIds() shouldContainExactly listOf(3L, 2L)
        harness.preferences.migrationSources.get() shouldContainExactly listOf(3L, 2L)
    }

    @Test
    fun selectionPresetsApply() {
        harness.preferences.pinnedSources.set(setOf("3", "junk"))
        harness.preferences.disabledSources.set(setOf("1", "junk"))
        val model = harness.model()
        model.awaitLoaded()
        model.toggleSelection(SelectionConfig.None)
        model.state.value.selectedIds() shouldBe emptyList()
        model.toggleSelection(SelectionConfig.All)
        model.state.value.selectedIds().toSet() shouldBe setOf(1L, 2L, 3L)
        model.toggleSelection(SelectionConfig.Pinned)
        model.state.value.selectedIds() shouldContainExactly listOf(3L)
        model.toggleSelection(SelectionConfig.Enabled)
        model.state.value.selectedIds().toSet() shouldBe setOf(2L, 3L)
    }

    @Test
    fun orderingMovesASource() {
        harness.preferences.migrationSources.set(listOf(1L, 2L, 3L))
        val model = harness.model()
        model.awaitLoaded()
        model.orderSource(from = 0, to = 2)
        model.state.value.selectedIds() shouldContainExactly listOf(2L, 3L, 1L)
        harness.preferences.migrationSources.get() shouldContainExactly listOf(2L, 3L, 1L)
    }

    @Test
    fun savingStoresTheSelection() {
        harness.preferences.migrationSources.set(listOf(3L))
        val model = harness.model()
        model.awaitLoaded()
        harness.preferences.migrationSources.set(emptyList())
        model.saveSources()
        harness.preferences.migrationSources.get() shouldContainExactly listOf(3L)
        model.sourcePreferences shouldBe harness.preferences
    }

    @Test
    fun defaultsComeFromInjekt() {
        val model = MigrationConfigScreenModel()
        model.awaitLoaded().sources.size shouldBe 3
        model.sourcePreferences shouldBe harness.preferences
    }

    @Test
    fun stateAndPresetsAreValues() {
        val state = MigrationConfigScreenModel.State()
        state.isLoading shouldBe true
        state.copy(isLoading = false) shouldBe MigrationConfigScreenModel.State(isLoading = false)
        state.hashCode() shouldBe MigrationConfigScreenModel.State().hashCode()
        state.toString().startsWith("State(") shouldBe true
        SelectionConfig.entries.map { it.name } shouldContainExactly listOf("All", "None", "Pinned", "Enabled")
        SelectionConfig.valueOf("All") shouldBe SelectionConfig.All
    }

    @Test
    fun migrationSourceIsAValue() {
        val domain = Source(id = 9L, lang = "zh-CN", name = "Nine", supportsLatest = false, isStub = false)
        val source = MigrationConfigScreen.MigrationSource(domain, isSelected = true)
        source.shortLanguage shouldBe "zh-hans"
        source.copy(isSelected = false).isSelected shouldBe false
        source.hashCode() shouldBe source.copy().hashCode()
        source shouldBe source.copy()
        source.toString().contains("Nine") shouldBe true
    }
}
