package eu.kanade.tachiyomi.ui.updates

import eu.kanade.tachiyomi.ui.base.customInfoModule
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import tachiyomi.core.common.preference.TriState

internal class UpdatesStateTest {
    @BeforeEach
    fun setUp() {
        startKoin { modules(customInfoModule()) }
    }

    @AfterEach
    fun tearDown() = stopKoin()

    @Test
    fun uiModelSeparatesDays() {
        val day = 86_400_000L
        val items = listOf(
            item(1).copy(update = update(1, dateFetch = day * 12)),
            item(2).copy(update = update(2, dateFetch = day * 12)),
            item(3).copy(update = update(3, dateFetch = day * 10)),
        )
        val models = UpdatesScreenModel.State(items = items).getUiModel()
        models.size shouldBe 5
        UpdatesScreenModel.State(items = emptyList()).getUiModel() shouldBe emptyList()
    }

    @Test
    fun stateTracksSelection() {
        val state = UpdatesScreenModel.State(items = listOf(item(1, selected = true), item(2)))
        state.selected.map { it.update.chapterId } shouldBe listOf(1L)
        state.selectionMode shouldBe true
        UpdatesScreenModel.State().selectionMode shouldBe false
        item(1).isEhBasedUpdate() shouldBe false
        item(1).copy(update = update(1, sourceId = EH_SOURCE_ID)).isEhBasedUpdate() shouldBe true
        item(1).copy(update = update(1, sourceId = EXH_SOURCE_ID)).isEhBasedUpdate() shouldBe true
    }

    @Test
    fun modelMembers() {
        UpdatesScreenModel.Event.InternalError.toString() shouldBe "InternalError"
        val prefs = UpdatesScreenModel.ItemPreferences(
            filterDownloaded = TriState.DISABLED,
            filterUnread = TriState.DISABLED,
            filterStarted = TriState.DISABLED,
            filterBookmarked = TriState.DISABLED,
            filterExcludedScanlators = false,
        )
        prefs.copy().hashCode() shouldBe prefs.hashCode()
    }
}
