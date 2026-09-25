package eu.kanade.tachiyomi.data.backup.create

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.common.util.concurrent.Futures.immediateFuture
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.backup.BackupKoin
import eu.kanade.tachiyomi.data.backup.workerParams
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.workManager
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
internal class BackupCreateJobTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val graph = BackupKoin()
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val target by lazy { Uri.fromFile(folder.newFile("out.tachibk")) }

    @Before
    fun setUp() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        startKoin { modules(graph.module()) }
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        every { any<Context>().workManager } returns workManager
        restoreState(WorkInfo.State.SUCCEEDED)
        mockkConstructor(BackupCreator::class)
        coEvery { anyConstructed<BackupCreator>().backup(any(), any()) } answers { target.toString() }
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun restoreState(state: WorkInfo.State) {
        val info = WorkInfo(UUID.randomUUID(), state, setOf("BackupRestore"))
        every { workManager.getWorkInfosByTag("BackupRestore") } returns immediateFuture(listOf(info))
    }

    private fun job(vararg input: Pair<String, Any?>) = BackupCreateJob(context, workerParams(workDataOf(*input)))

    private fun completeTitle(): String? = shadowOf(context.getSystemService(NotificationManager::class.java))
        .getNotification(Notifications.ID_BACKUP_COMPLETE)
        ?.extras
        ?.getCharSequence(NotificationCompat.EXTRA_TITLE)
        ?.toString()

    @Test
    fun autoBackupWaitsForARestore() = runTest {
        restoreState(WorkInfo.State.RUNNING)
        job().doWork() shouldBe Result.retry()
    }

    @Test
    fun autoBackupUsesItsDirectory() = runTest {
        val dir = UniFile.fromFile(folder.newFolder("auto"))
        every { graph.storageManager.getAutomaticBackupsDirectory() } returns dir
        job().doWork() shouldBe Result.success()
        coVerify { anyConstructed<BackupCreator>().backup(checkNotNull(dir).uri, BackupOptions()) }
        completeTitle().shouldBeNull()
    }

    @Test
    fun noLocationFails() = runTest {
        every { graph.storageManager.getAutomaticBackupsDirectory() } returns null
        job("is_auto_backup" to true).doWork() shouldBe Result.failure()
    }

    @Test
    fun manualBackupNotifies() = runTest {
        val options = BackupOptions(chapters = false, savedSearches = false)
        job(
            "is_auto_backup" to false,
            "location_uri" to target.toString(),
            "options" to options.asBooleanArray(),
        ).doWork() shouldBe Result.success()
        coVerify { anyConstructed<BackupCreator>().backup(target, options) }
        completeTitle() shouldBe "Backup created"
    }

    @Test
    fun manualFailureNotifies() = runTest {
        coEvery { anyConstructed<BackupCreator>().backup(any(), any()) } throws IllegalStateException("full")
        job("is_auto_backup" to false, "location_uri" to target.toString()).doWork() shouldBe Result.failure()
        completeTitle() shouldBe "Backup failed"
    }

    @Test
    fun autoFailureIsSilent() = runTest {
        coEvery { anyConstructed<BackupCreator>().backup(any(), any()) } throws IllegalStateException("full")
        job("location_uri" to target.toString()).doWork() shouldBe Result.failure()
        completeTitle().shouldBeNull()
    }

    @Test
    fun foregroundIsADataSync() = runTest {
        val info = job().getForegroundInfo()
        info.notificationId shouldBe Notifications.ID_BACKUP_PROGRESS
        info.foregroundServiceType shouldBe ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        val sdk = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.P)
        try {
            job().getForegroundInfo().foregroundServiceType shouldBe 0
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        }
    }
}
