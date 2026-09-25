package eu.kanade.tachiyomi.ui.updates

import android.content.Context
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.startNow
import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import tachiyomi.core.common.preference.TriState

internal class UpdatesScreenModelTest {
    private val harness = UpdatesHarness()

    @BeforeEach
    fun setUp() {
        mainUnconfined()
        startKoin { modules(harness.koinModules()) }
    }

    @AfterEach
    fun tearDown() {
        mainReset()
        stopKoin()
        unmockkAll()
    }

    @Test
    fun updatesBecomeItems() {
        harness.queue.value = listOf(download(2, Download.State.DOWNLOADING))
        every { harness.downloadManager.isChapterDownloaded("C3", null, "/c/3", "M1", 1, false) } returns true
        harness.updates.value = listOf(update(1), update(2), update(3))
        val state = harness.model().state.await { !it.isLoading && it.items.size == 3 }
        state.items.map { it.downloadStateProvider() } shouldBe listOf(
            Download.State.NOT_DOWNLOADED,
            Download.State.DOWNLOADING,
            Download.State.DOWNLOADED,
        )
        state.items.map { it.downloadProgressProvider() } shouldBe listOf(0, 0, 0)
    }

    @Test
    fun downloadedFilterApplies() {
        every { harness.downloadManager.isChapterDownloaded("C3", null, "/c/3", "M1", 1, false) } returns true
        harness.updates.value = listOf(update(1), update(3))
        harness.updatesPreferences.filterDownloaded.set(TriState.ENABLED_IS)
        val model = harness.model()
        model.state.await { !it.isLoading && it.hasActiveFilters }.items.map { it.update.chapterId } shouldBe listOf(3L)
        harness.updatesPreferences.filterDownloaded.set(TriState.ENABLED_NOT)
        model.state.await { it.items.size == 1 && it.items[0].update.chapterId == 1L }
    }

    @Test
    fun sqlFiltersAreForwarded() {
        harness.updatesPreferences.filterUnread.set(TriState.ENABLED_IS)
        harness.updatesPreferences.filterStarted.set(TriState.ENABLED_NOT)
        val model = harness.model()
        model.state.await { it.hasActiveFilters }
        harness.updatesPreferences.filterUnread.set(TriState.DISABLED)
        harness.updatesPreferences.filterStarted.set(TriState.DISABLED)
        model.state.await { !it.hasActiveFilters }
        TriState.ENABLED_IS.toBooleanOrNull() shouldBe true
        TriState.ENABLED_NOT.toBooleanOrNull() shouldBe false
        TriState.DISABLED.toBooleanOrNull() shouldBe null
    }

    @Test
    fun statusFlowUpdatesItems() {
        harness.updates.value = listOf(update(1))
        val loaded = CompletableDeferred<Unit>()
        // The status arrives only once the item is listed; both observers run on the IO pool.
        harness.statuses = flow {
            loaded.await()
            // Re-sent a few times so a concurrent list refresh cannot hide it; then the flow fails.
            repeat(RESENDS) {
                emit(download(1, Download.State.QUEUE))
                delay(RESEND_MILLIS)
            }
            error("closed")
        }
        val model = harness.model()
        model.state.await { it.items.size == 1 }
        loaded.complete(Unit)
        model.state.await { s -> s.items.singleOrNull()?.downloadStateProvider() == Download.State.QUEUE }
    }

    @Test
    fun otherFiltersKeepDownloaded() {
        val model = harness.model()
        model.state.await { !it.isLoading }
        harness.updatesPreferences.filterBookmarked.set(TriState.ENABLED_IS)
        model.state.await { it.hasActiveFilters }
        UpdatesItem(update(1), { Download.State.QUEUE }, { 0 }).selected shouldBe false
    }

    @Test
    fun dialogsAreSet() {
        val model = harness.model()
        model.showFilterDialog()
        model.state.value.dialog shouldBe UpdatesScreenModel.Dialog.FilterSheet
        val items = listOf(item(1))
        model.showConfirmDeleteChapters(items)
        model.state.value.dialog shouldBe UpdatesScreenModel.Dialog.DeleteConfirmation(items)
        model.setDialog(null)
        model.state.value.dialog shouldBe null
    }

    @Test
    fun preferencesAreExposed() {
        harness.libraryPreferences.newUpdatesCount.set(4)
        harness.libraryPreferences.lastUpdatedTimestamp.set(9L)
        val model = harness.model()
        model.resetNewUpdatesCount()
        harness.libraryPreferences.newUpdatesCount.get() shouldBe 0
        model.lastUpdated shouldBe 9L
        model.preserveReadingPosition shouldBe false
    }

    @Test
    fun libraryUpdateIsTriggered() {
        mockkStatic("eu.kanade.tachiyomi.data.library.LibraryUpdateSchedulingKt")
        every { LibraryUpdateJob.startNow(any<Context>()) } returns true
        val model = UpdatesScreenModel()
        model.updateLibrary() shouldBe true
        model.events.await { true } shouldBe UpdatesScreenModel.Event.LibraryUpdateTriggered(started = true)
    }
}

private const val RESENDS = 20
private const val RESEND_MILLIS = 50L
