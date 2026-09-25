package eu.kanade.tachiyomi.data.backup.restore

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.tachiyomi.data.backup.BackupKoin
import eu.kanade.tachiyomi.data.backup.workerParams
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.workManager
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
internal class BackupRestoreJobTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val graph = BackupKoin()
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val uri = Uri.parse("file:///backup.tachibk")
    private val options = RestoreOptions(appSettings = false)

    @Before
    fun setUp() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        startKoin { modules(graph.module()) }
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        every { any<Context>().workManager } returns workManager
        mockkConstructor(BackupRestorer::class)
        coEvery { anyConstructed<BackupRestorer>().restore(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun job(vararg input: Pair<String, Any?>) = BackupRestoreJob(context, workerParams(workDataOf(*input)))

    private fun validJob() = job("location_uri" to uri.toString(), "options" to options.asBooleanArray())

    private fun completeText(key: String): String? = shadowOf(context.getSystemService(NotificationManager::class.java))
        .getNotification(Notifications.ID_RESTORE_COMPLETE)
        ?.extras
        ?.getCharSequence(key)
        ?.toString()

    @Test
    fun incompleteInputFails() = runTest {
        job().doWork() shouldBe Result.failure()
        job("location_uri" to uri.toString()).doWork() shouldBe Result.failure()
        job("options" to options.asBooleanArray()).doWork() shouldBe Result.failure()
    }

    @Test
    fun restoreRuns() = runTest {
        validJob().doWork() shouldBe Result.success()
        coVerify { anyConstructed<BackupRestorer>().restore(uri, options) }
    }

    @Test
    fun cancellingIsNotAFailure() = runTest {
        coEvery { anyConstructed<BackupRestorer>().restore(any(), any()) } throws CancellationException("stop")
        validJob().doWork() shouldBe Result.success()
        completeText(NotificationCompat.EXTRA_TEXT) shouldBe "Canceled restore"
    }

    @Test
    fun errorsFailTheRun() = runTest {
        coEvery { anyConstructed<BackupRestorer>().restore(any(), any()) } throws IllegalStateException("corrupt")
        job("location_uri" to uri.toString(), "options" to options.asBooleanArray(), "sync" to true)
            .doWork() shouldBe Result.failure()
        completeText(NotificationCompat.EXTRA_TITLE) shouldBe "Restoring backup failed"
        completeText(NotificationCompat.EXTRA_TEXT) shouldBe "corrupt"
    }

    @Test
    fun foregroundIsADataSync() = runTest {
        job().getForegroundInfo().foregroundServiceType shouldBe ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        val sdk = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.P)
        try {
            job().getForegroundInfo().foregroundServiceType shouldBe 0
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        }
    }

    @Test
    fun companionControlsTheWork() {
        val running = WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING, setOf("BackupRestore"))
        every { workManager.getWorkInfosByTag("BackupRestore") } returns immediateFuture(listOf(running))
        BackupRestoreJob.isRunning(context) shouldBe true
        val request = slot<OneTimeWorkRequest>()
        BackupRestoreJob.start(context, uri, options)
        verify { workManager.enqueueUniqueWork("BackupRestore", ExistingWorkPolicy.KEEP, capture(request)) }
        val input = request.captured.workSpec.input
        input.getString("location_uri") shouldBe uri.toString()
        input.getBoolean("sync", true) shouldBe false
        input.getBooleanArray("options")?.toList() shouldBe options.asBooleanArray().toList()
        BackupRestoreJob.stop(context)
        verify { workManager.cancelUniqueWork("BackupRestore") }
    }
}
