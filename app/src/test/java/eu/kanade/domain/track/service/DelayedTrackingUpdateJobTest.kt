package eu.kanade.domain.track.service

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.track.interactor.TrackChapter
import eu.kanade.domain.track.model.domainTrack
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.util.system.workManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.track.interactor.GetTracks

internal class DelayedTrackingUpdateJobTest {

    private val context = mockk<Context>(relaxed = true)
    private val getTracks = mockk<GetTracks>()
    private val trackChapter = mockk<TrackChapter>()
    private val store = mockk<DelayedTrackingStore>(relaxed = true)
    private val params = mockk<WorkerParameters>(relaxed = true)
    private val logged = captureLogcat()

    @BeforeEach
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { getTracks }
                    single { trackChapter }
                    single { store }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        releaseLogcat()
        unmockkAll()
    }

    @Test
    fun givesUpAfterTooManyAttempts() = runTest {
        every { params.runAttemptCount } returns 4
        DelayedTrackingUpdateJob(context, params).doWork() shouldBe ListenableWorker.Result.failure()
    }

    @Test
    fun pushesQueuedDropsStale() = runTest {
        every { params.runAttemptCount } returns 0
        every { store.getItems() } returnsMany listOf(
            listOf(
                DelayedTrackingStore.DelayedTrackingItem(trackId = 1, lastChapterRead = 7f),
                DelayedTrackingStore.DelayedTrackingItem(trackId = 2, lastChapterRead = 3f),
            ),
            emptyList(),
        )
        coEvery { getTracks.awaitOne(1) } returns domainTrack(id = 1)
        coEvery { getTracks.awaitOne(2) } returns null
        coEvery { trackChapter.await(context, 9, 7.0, setupJobOnFailure = false) } returns Unit
        DelayedTrackingUpdateJob(context, params).doWork() shouldBe ListenableWorker.Result.success()
        verify(exactly = 1) { store.remove(2) }
        coVerify(exactly = 1) { trackChapter.await(context, 9, 7.0, setupJobOnFailure = false) }
        logged.single() shouldContain "Updating delayed track item: 9, last chapter read: 7.0"
    }

    @Test
    fun retriesWhileItemsRemain() = runTest {
        every { params.runAttemptCount } returns 1
        every { store.getItems() } returns listOf(
            DelayedTrackingStore.DelayedTrackingItem(trackId = 1, lastChapterRead = 7f),
        )
        coEvery { getTracks.awaitOne(1) } returns domainTrack(id = 1)
        coEvery { trackChapter.await(context, 9, 7.0, setupJobOnFailure = false) } returns Unit
        DelayedTrackingUpdateJob(context, params).doWork() shouldBe ListenableWorker.Result.retry()
    }

    @Test
    fun setupTaskEnqueuesUniqueWork() {
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        val workManager = mockk<WorkManager>(relaxed = true)
        every { context.workManager } returns workManager
        val request = slot<OneTimeWorkRequest>()
        DelayedTrackingUpdateJob.setupTask(context)
        verify(exactly = 1) {
            workManager.enqueueUniqueWork("DelayedTrackingUpdate", ExistingWorkPolicy.REPLACE, capture(request))
        }
        request.captured.tags shouldBe setOf(DelayedTrackingUpdateJob::class.java.name, "DelayedTrackingUpdate")
    }
}
