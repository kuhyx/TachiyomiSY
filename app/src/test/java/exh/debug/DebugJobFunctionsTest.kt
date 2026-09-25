package exh.debug

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.google.common.util.concurrent.Futures
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
internal class DebugJobFunctionsTest {
    private val workId = UUID.randomUUID()

    @Before
    fun setUp() {
        // The mock lives in the companion because the debug object resolves the app once per JVM.
        clearMocks(workManager)
        jobScheduler.cancelAll()
        mockkObject(WorkManager)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        stopKoin()
        startKoin { modules(module { single<Application> { context } }) }
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun job(id: Int, workSpecId: String?, periodic: Boolean): JobInfo {
        val extras = PersistableBundle().apply {
            if (workSpecId != null) putString("EXTRA_WORK_SPEC_ID", workSpecId)
            putBoolean("EXTRA_IS_PERIODIC", periodic)
        }
        val builder = JobInfo.Builder(id, ComponentName(context, DebugJobFunctionsTest::class.java))
            .setExtras(extras)
            .setMinimumLatency(1)
        return builder.build()
    }

    @Test
    fun jobsAreDescribedFromWorkInfo() {
        val info = mockk<WorkInfo>()
        every { info.id } returns workId
        every { info.state } returns WorkInfo.State.ENQUEUED
        every { info.tags } returns setOf("a", "b")
        every { workManager.getWorkInfoById(workId) } returns Futures.immediateFuture(info)
        jobScheduler.schedule(job(1, workId.toString(), periodic = true))
        val listing = DebugJobFunctions.listScheduledJobs()
        listing shouldContain "id: $workId"
        listing shouldContain "isPeriodic: true"
        listing shouldContain "state: ENQUEUED"
        listing shouldContain "a,\n    b"
    }

    @Test
    fun jobsWithoutWorkInfoUseTheJob() {
        jobScheduler.schedule(job(7, null, periodic = false))
        val listing = DebugJobFunctions.listScheduledJobs()
        listing shouldContain "info: 7"
        listing shouldContain "isPersisted: false"
        jobScheduler.cancelAll()
        DebugJobFunctions.listScheduledJobs() shouldBe ""
    }

    @Test
    fun cancellingClearsJobsAndWork() {
        jobScheduler.schedule(job(3, null, periodic = false))
        DebugJobFunctions.cancelAllScheduledJobs()
        jobScheduler.allPendingJobs.size shouldBe 0
        val running = mockk<WorkInfo>(relaxed = true)
        every { running.id } returns workId
        every { workManager.getWorkInfos(any<WorkQuery>()) } returns Futures.immediateFuture(listOf(running))
        DebugJobFunctions.killSyncJobs()
        DebugJobFunctions.killLibraryJobs()
        verify(atLeast = 2) { workManager.cancelWorkById(workId) }
    }

    private companion object {
        val workManager = mockk<WorkManager>(relaxed = true)
        val context: Application = ApplicationProvider.getApplicationContext()
        val jobScheduler: JobScheduler = context.getSystemService(JobScheduler::class.java)
    }
}
