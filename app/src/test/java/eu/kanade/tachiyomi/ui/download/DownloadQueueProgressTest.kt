package eu.kanade.tachiyomi.ui.download

import android.os.Looper
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
internal class DownloadQueueProgressTest {
    private val harness = DownloadHarness()
    private val first = download(1)
    private lateinit var clock: TestCoroutineScheduler
    private lateinit var model: DownloadQueueScreenModel

    @Before
    fun setUp() {
        clock = mainUnconfined()
        model = harness.model()
    }

    private fun elapse() {
        clock.advanceTimeBy(STEP_MILLIS)
        clock.runCurrent()
        // The progress bar animates to its value on the main looper.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    }

    @After
    fun tearDown() = mainReset()

    private fun pagesText(): String =
        model.getHolder(first)!!.itemView.findViewById<TextView>(R.id.download_progress_text).text.toString()

    @Test
    fun holdersAreFoundByChapter() {
        model.getHolder(first).shouldBeNull()
        harness.attach(model, headers(first))
        model.getHolder(first).shouldNotBeNull()
        model.getHolder(download(9)).shouldBeNull()
    }

    @Test
    fun downloadingTracksProgress() {
        harness.attach(model, headers(first))
        first.transition(Download.State.DOWNLOADING)
        model.onStatusChange(first)
        model.progressJobs[first].shouldNotBeNull()
        model.onStatusChange(first)
        elapse()
        first.pages = listOf(Page(0), Page(1))
        elapse()
        first.pages!![0].progress = 50
        elapse()
        // Only the job's collector pushes page progress into the bound row.
        val bar = model.getHolder(first)!!.itemView.findViewById<LinearProgressIndicator>(R.id.download_progress)
        bar.progress shouldBe 50
        model.onUpdateDownloadedPages(first)
        pagesText() shouldBe "0/2"
    }

    @Test
    fun finishedDownloadsStopTracking() {
        harness.attach(model, headers(first))
        first.pages = listOf(Page(0))
        first.transition(Download.State.DOWNLOADING)
        model.onStatusChange(first)
        first.transition(Download.State.DOWNLOADED)
        model.onStatusChange(first)
        model.progressJobs[first].shouldBeNull()
        pagesText() shouldBe "0/1"
    }

    @Test
    fun errorsAndOtherStates() {
        model.launchProgressJob(first)
        first.transition(Download.State.ERROR)
        model.onStatusChange(first)
        model.progressJobs[first].shouldBeNull()
        first.transition(Download.State.QUEUE)
        model.onStatusChange(first)
        model.onUpdateProgress(first)
        model.onUpdateDownloadedPages(first)
        model.cancelProgressJob(first)
        model.progressJobs.isEmpty() shouldBe true
    }
}

private const val STEP_MILLIS = 100L
