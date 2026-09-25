package eu.kanade.tachiyomi.ui.history

import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import eu.kanade.tachiyomi.util.lang.toLocalDate
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import java.util.Date

internal class HistoryScreenModelTest {
    private val harness = HistoryHarness()
    private val chapter = Chapter.create().copy(id = 5L)

    @BeforeEach
    fun setUp() {
        mainUnconfined()
        startKoin { modules(harness.koinModules()) }
    }

    @AfterEach
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    @Test
    fun historyIsGroupedByDay() {
        val day1 = Date(DAY * 10)
        val day2 = Date(DAY * 12)
        val entries = listOf(history(1, day2), history(2, day2), history(3, null), history(4, day1))
        every { harness.getHistory.subscribe("") } returns MutableStateFlow(entries)
        val list = harness.model().state.await { it.list != null }.list
        list shouldBe listOf(
            HistoryUiModel.Header(day2.time.toLocalDate()),
            HistoryUiModel.Item(entries[0]),
            HistoryUiModel.Item(entries[1]),
            HistoryUiModel.Item(entries[2]),
            HistoryUiModel.Header(day1.time.toLocalDate()),
            HistoryUiModel.Item(entries[3]),
        )
    }

    @Test
    fun searchQueryFiltersHistory() {
        every { harness.getHistory.subscribe("q") } returns MutableStateFlow(listOf(history(1, null)))
        val model = harness.model()
        model.state.await { it.list == emptyList<HistoryUiModel>() }
        model.updateSearchQuery("q")
        model.state.await { it.list?.size == 1 }.searchQuery shouldBe "q"
    }

    @Test
    fun failingHistoryEmitsAnError() {
        every { harness.getHistory.subscribe("") } returns flow { error("db") }
        harness.model().events.await { true } shouldBe HistoryScreenModel.Event.InternalError
    }

    @Test
    fun nextChapterIsTheFirst() = runBlocking {
        coEvery { harness.getNextChapters.await(onlyUnread = false) } returns listOf(chapter)
        harness.model().getNextChapter() shouldBe chapter
        coEvery { harness.getNextChapters.await(onlyUnread = false) } returns emptyList()
        harness.model().getNextChapter().shouldBeNull()
    }

    @Test
    fun nextChapterForMangaIsOpened() {
        coEvery { harness.getNextChapters.await(1L, 2L, onlyUnread = false) } returns listOf(chapter)
        coEvery { harness.getNextChapters.await(1L, 3L, onlyUnread = false) } returns emptyList()
        val model = harness.model()
        model.getNextChapterForManga(1L, 2L)
        model.events.await { true } shouldBe HistoryScreenModel.Event.OpenChapter(chapter)
        model.getNextChapterForManga(1L, 3L)
        model.events.await { true } shouldBe HistoryScreenModel.Event.OpenChapter(null)
    }

    @Test
    fun dialogsAreSet() {
        val model = harness.model()
        model.setDialog(HistoryScreenModel.Dialog.DeleteAll)
        model.state.value.dialog shouldBe HistoryScreenModel.Dialog.DeleteAll
        val manga = Manga.create()
        model.showMigrateDialog(manga, manga.copy(id = 2))
        model.state.value.dialog shouldBe HistoryScreenModel.Dialog.Migrate(manga, manga.copy(id = 2))
        model.setDialog(null)
        model.state.value.dialog.shouldBeNull()
    }

    @Test
    fun defaultsComeFromInjekt() {
        HistoryScreenModel().state.await { it.list != null }.list shouldBe emptyList()
    }

    @Test
    fun modelMembers() {
        val delete = HistoryScreenModel.Dialog.Delete(history(1, null))
        delete.copy().hashCode() shouldBe delete.hashCode()
        HistoryScreenModel.Event.HistoryCleared.toString() shouldBe "HistoryCleared"
        HistoryScreenModel.State().copy(searchQuery = "a").searchQuery shouldBe "a"
    }
}

private const val DAY = 86_400_000L
