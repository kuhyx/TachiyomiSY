package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkInfo
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI

@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateJobAutoTest {

    private val harness = LibraryJobHarness()
    private val sdk = Build.VERSION.SDK_INT
    private var wifi = false

    @Before
    fun setUp() {
        harness.start()
        mockkStatic("eu.kanade.tachiyomi.util.system.NetworkExtensionsKt")
        every { any<Context>().isConnectedToWifi() } answers { wifi }
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        harness.stop()
    }

    private fun sdk(level: Int) = ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", level)

    private suspend fun autoRun() = harness.job(Data.EMPTY, LibraryUpdateJob.WORK_NAME_AUTO).doWork()

    @Test
    fun autoRunYieldsToManual() = runTest {
        harness.running(tag = LibraryUpdateJob.WORK_NAME_MANUAL, state = WorkInfo.State.RUNNING)
        autoRun() shouldBe Result.retry()
    }

    @Test
    fun autoRunProceedsWhenIdle() = runTest {
        autoRun() shouldBe Result.success()
    }

    @Test
    fun oldAndroidWaitsForWifi() = runTest {
        sdk(Build.VERSION_CODES.O)
        harness.libraryPreferences.autoUpdateDeviceRestrictions.set(setOf(DEVICE_ONLY_ON_WIFI))
        autoRun() shouldBe Result.retry()
        wifi = true
        autoRun() shouldBe Result.success()
    }

    @Test
    fun oldAndroidWithoutWifiRule() = runTest {
        sdk(Build.VERSION_CODES.O)
        harness.libraryPreferences.autoUpdateDeviceRestrictions.set(emptySet())
        autoRun() shouldBe Result.success()
    }

    @Test
    fun foregroundIsADataSync() = runTest {
        val info = harness.job().getForegroundInfo()
        info.notificationId shouldBe Notifications.ID_LIBRARY_PROGRESS
        info.foregroundServiceType shouldBe ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    }

    @Test
    fun oldAndroidHasNoServiceType() = runTest {
        sdk(Build.VERSION_CODES.P)
        harness.job().getForegroundInfo().foregroundServiceType shouldBe 0
    }
}
