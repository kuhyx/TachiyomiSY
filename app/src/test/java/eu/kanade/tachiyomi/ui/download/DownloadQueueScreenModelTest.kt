package eu.kanade.tachiyomi.ui.download

import android.view.MenuItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.cancelQueuedDownloads
import eu.kanade.tachiyomi.data.download.reorderQueue
import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Job
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DownloadQueueScreenModelTest {
    private val harness = DownloadHarness()
    private val first = download(1)
    private val second = download(2, dateUpload = 0)
    private val other = download(3, source = httpSource(2), mangaId = 2)

    @Before
    fun setUp() {
        mainUnconfined()
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerQueueKt")
        every { harness.manager.reorderQueue(any()) } just runs
        every { harness.manager.cancelQueuedDownloads(any()) } just runs
    }

    @After
    fun tearDown() {
        mainReset()
        unmockkAll()
        stopKoin()
    }

    private fun menu(id: Int): MenuItem = mockk { every { itemId } returns id }

    @Test
    fun queueIsGroupedBySource() {
        harness.queue.value = listOf(first, second, other)
        val state = harness.model().state.await { it.size == 2 }
        state.map { Triple(it.id, it.name, it.size) } shouldBe listOf(Triple(1L, "S1", 2), Triple(2L, "S2", 1))
        state[0].subItems.map { it.download } shouldBe listOf(first, second)
    }

    @Test
    fun releasingReordersTheQueue() {
        val model = harness.model()
        model.listener.onItemReleased(0)
        verify(exactly = 0) { harness.manager.reorderQueue(any()) }
        harness.attach(model, headers(first, second, other))
        model.listener.onItemReleased(0)
        verify { harness.manager.reorderQueue(listOf(first, second, other)) }
    }

    @Test
    fun menuNeedsADownloadRow() {
        val model = harness.model()
        model.listener.onMenuItemClick(1, menu(R.id.cancel_download))
        harness.attach(model, headers(first))
        model.listener.onMenuItemClick(0, menu(R.id.cancel_download))
        model.listener.onMenuItemClick(1, menu(R.id.menu))
        verify(exactly = 0) { harness.manager.cancelQueuedDownloads(any()) }
        model.listener.onMenuItemClick(1, menu(R.id.cancel_download))
        verify { harness.manager.cancelQueuedDownloads(listOf(first)) }
    }

    @Test
    fun menuMovesWithinAndAcrossSeries() {
        val model = harness.model()
        harness.attach(model, headers(first, second, other))
        model.listener.onMenuItemClick(2, menu(R.id.move_to_top))
        verify { harness.manager.reorderQueue(listOf(second, first, other)) }
        model.listener.onMenuItemClick(1, menu(R.id.move_to_bottom))
        model.listener.onMenuItemClick(4, menu(R.id.move_to_top_series))
        verify { harness.manager.reorderQueue(listOf(other, first, second)) }
        model.listener.onMenuItemClick(4, menu(R.id.move_to_bottom_series))
        verify { harness.manager.reorderQueue(listOf(first, second, other)) }
    }

    @Test
    fun cancellingASeries() {
        val model = harness.model()
        harness.attach(model, headers(first, second, other))
        model.listener.onMenuItemClick(1, menu(R.id.cancel_series))
        verify { harness.manager.cancelQueuedDownloads(listOf(first, second)) }
    }

    @Test
    fun sortingReordersEachSeries() {
        val model = harness.model()
        model.reorderQueue({ it.download.chapter.dateUpload })
        harness.attach(model, headers(first, second))
        model.reorderQueue({ it.download.chapter.dateUpload })
        verify { harness.manager.reorderQueue(listOf(second, first)) }
        model.reorderQueue({ it.download.chapter.dateUpload }, reverse = true)
        verify { harness.manager.reorderQueue(listOf(first, second)) }
    }

    @Test
    fun disposingCancelsTheJobs() {
        val model = harness.model()
        harness.attach(model, headers(first))
        val job = Job()
        model.progressJobs[first] = job
        model.onDispose()
        job.isCancelled shouldBe true
        model.progressJobs shouldBe emptyMap()
        model.adapter.shouldBeNull()
    }

    @Test
    fun managerFlowsAreExposed() {
        val model = harness.model()
        model.getDownloadStatusFlow()
        model.getDownloadProgressFlow()
        harness.running.value = true
        model.isDownloaderRunning.await { it } shouldBe true
        verify { harness.manager.statusFlow() }
        verify { harness.manager.progressFlow() }
    }

    @Test
    fun defaultManagerComesFromInjekt() {
        startKoin { modules(module { single { harness.manager } }) }
        DownloadQueueScreenModel().downloadManager shouldBe harness.manager
    }
}
