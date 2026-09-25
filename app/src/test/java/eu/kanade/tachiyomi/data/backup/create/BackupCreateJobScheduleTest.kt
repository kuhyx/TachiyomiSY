package eu.kanade.tachiyomi.data.backup.create

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.tachiyomi.data.backup.BackupKoin
import eu.kanade.tachiyomi.util.system.workManager
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class BackupCreateJobScheduleTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val graph = BackupKoin()
    private val workManager = mockk<WorkManager>(relaxed = true)

    @Before
    fun setUp() {
        startKoin { modules(graph.module()) }
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        every { any<Context>().workManager } returns workManager
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    @Test
    fun manualRunIsTracked() {
        val running = WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING, setOf("BackupCreator:manual"))
        every { workManager.getWorkInfosByTag("BackupCreator:manual") } returns immediateFuture(listOf(running))
        BackupCreateJob.isManualJobRunning(context) shouldBe true
    }

    @Test
    fun intervalSchedulesAutoBackups() {
        val request = slot<PeriodicWorkRequest>()
        BackupCreateJob.setupTask(context, prefInterval = 6)
        verify {
            workManager.enqueueUniquePeriodicWork("BackupCreator", ExistingPeriodicWorkPolicy.UPDATE, capture(request))
        }
        val spec = request.captured.workSpec
        spec.intervalDuration shouldBe TimeUnit.HOURS.toMillis(6)
        spec.constraints.requiresBatteryNotLow() shouldBe true
        spec.input.getBoolean("is_auto_backup", false) shouldBe true
    }

    @Test
    fun preferenceIntervalIsTheDefault() {
        BackupCreateJob.setupTask(context)
        verify { workManager.enqueueUniquePeriodicWork("BackupCreator", any(), any()) }
        graph.backupPreferences.backupInterval.set(0)
        BackupCreateJob.setupTask(context)
        verify { workManager.cancelUniqueWork("BackupCreator") }
    }

    @Test
    fun startNowQueuesAManualRun() {
        val request = slot<OneTimeWorkRequest>()
        val uri = Uri.parse("content://backups/b.tachibk")
        BackupCreateJob.startNow(context, uri, BackupOptions(history = false))
        verify { workManager.enqueueUniqueWork("BackupCreator:manual", ExistingWorkPolicy.KEEP, capture(request)) }
        val input = request.captured.workSpec.input
        input.getBoolean("is_auto_backup", true) shouldBe false
        input.getString("location_uri") shouldBe uri.toString()
        input.getBooleanArray("options")?.toList() shouldBe BackupOptions(history = false).asBooleanArray().toList()
    }
}
