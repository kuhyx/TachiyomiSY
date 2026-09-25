package eu.kanade.tachiyomi.ui.download

import eu.kanade.tachiyomi.data.download.cancelQueuedDownloads
import eu.kanade.tachiyomi.data.download.clearQueue
import eu.kanade.tachiyomi.data.download.pauseDownloads
import eu.kanade.tachiyomi.data.download.reorderQueue
import eu.kanade.tachiyomi.data.download.startDownloads
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DownloadQueueActionsTest {
    private val harness = DownloadHarness()
    private val first = download(1)
    private val other = download(3, source = httpSource(2), mangaId = 2)

    @Before
    fun setUp() {
        mainUnconfined()
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerQueueKt")
        every { harness.manager.startDownloads() } just runs
        every { harness.manager.pauseDownloads() } just runs
        every { harness.manager.clearQueue() } just runs
        every { harness.manager.reorderQueue(any()) } just runs
        every { harness.manager.cancelQueuedDownloads(any()) } just runs
    }

    @After
    fun tearDown() {
        mainReset()
        unmockkAll()
    }

    @Test
    fun queueControlsReachTheManager() {
        val model = harness.model()
        model.startDownloads()
        model.pauseDownloads()
        model.clearQueue()
        verify { harness.manager.startDownloads() }
        verify { harness.manager.pauseDownloads() }
        verify { harness.manager.clearQueue() }
    }

    @Test
    fun movesNeedAnAdapter() {
        val model = harness.model()
        val item = headers(first).single().subItems.single()
        model.moveWithinSeries(item, toTop = true)
        model.moveSeries(item, toTop = true)
        model.cancelSeries(item)
        verify(exactly = 1) { harness.manager.reorderQueue(emptyList()) }
        verify(exactly = 0) { harness.manager.cancelQueuedDownloads(any()) }
    }

    @Test
    fun emptySeriesCancelsNothing() {
        val model = harness.model()
        harness.attach(model, headers(other))
        model.cancelSeries(headers(first).single().subItems.single())
        verify(exactly = 0) { harness.manager.cancelQueuedDownloads(any()) }
    }
}
