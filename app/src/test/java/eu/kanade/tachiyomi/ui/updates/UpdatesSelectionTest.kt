package eu.kanade.tachiyomi.ui.updates

import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

internal class UpdatesSelectionTest {
    private val harness = UpdatesHarness()
    private lateinit var model: UpdatesScreenModel

    @BeforeEach
    fun setUp() {
        mainUnconfined()
        startKoin { modules(harness.koinModules()) }
        harness.updates.value = listOf(update(1), update(2), update(3))
        model = harness.model()
        model.state.await { it.items.size == 3 }
    }

    @AfterEach
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    private fun selected(): List<Long> = model.state.value.selected.map { it.update.chapterId }

    @Test
    fun tappingTogglesOneRow() {
        model.toggleSelection(item(2), selected = true)
        selected() shouldBe listOf(2L)
        model.selectedChapterIds shouldContainExactly setOf(2L)
        model.toggleSelection(item(2), selected = false)
        selected() shouldBe emptyList()
    }

    @Test
    fun longPressSelectsARange() {
        model.toggleSelection(item(1), selected = true, fromLongPress = true)
        model.toggleSelection(item(3), selected = true, fromLongPress = true)
        selected() shouldBe listOf(1L, 2L, 3L)
    }

    @Test
    fun allAndInvert() {
        model.toggleAllSelection(true)
        selected() shouldBe listOf(1L, 2L, 3L)
        model.selectedPositions.toList() shouldBe listOf(-1, -1)
        model.toggleSelection(item(2), selected = false)
        model.invertSelection()
        selected() shouldBe listOf(2L)
        model.selectedChapterIds shouldContainExactly setOf(2L)
        model.toggleAllSelection(false)
        selected() shouldBe emptyList()
    }
}
