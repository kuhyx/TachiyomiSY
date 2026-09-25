package eu.kanade.tachiyomi.data.updater

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.saver.plainFileUris
import eu.kanade.tachiyomi.network.NetworkHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class AppUpdateDownloadJobTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val network: NetworkHelper = mockk()
    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
        plainFileUris()
        context.allowNotifications()
        every { network.client } returns OkHttpClient()
        startKoin { modules(module { single { network } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
        server.close()
        unmockkAll()
    }

    private fun job(url: String?, title: String? = null): AppUpdateDownloadJob {
        val params = mockk<WorkerParameters>(relaxed = true)
        every { params.foregroundUpdater } returns ForegroundUpdater { _, _, _ -> immediateFuture(null) }
        every { params.inputData } returns workDataOf(
            AppUpdateDownloadJob.EXTRA_DOWNLOAD_URL to url,
            AppUpdateDownloadJob.EXTRA_DOWNLOAD_TITLE to title,
        )
        return AppUpdateDownloadJob(context, params)
    }

    private fun apkFile(): File = File(context.externalCacheDir, "update.apk")

    @Test
    fun missingUrlFails() = runTest {
        job(url = null).doWork() shouldBe ListenableWorker.Result.failure()
        job(url = "").doWork() shouldBe ListenableWorker.Result.failure()
    }

    @Test
    fun downloadPromptsInstall() = runTest {
        server.enqueue(MockResponse(body = "x".repeat(200_000)))
        job(url = server.url("/app.apk").toString(), title = "v2").doWork() shouldBe ListenableWorker.Result.success()
        apkFile().length() shouldBe 200_000L
        context.activeNotificationIds() shouldBe listOf(Notifications.ID_APP_UPDATE_PROMPT)
    }

    /** A chunked body has no length, so the listener's progress never grows past the saved value. */
    @Test
    fun chunkedDownloadStillSaves() = runTest {
        server.enqueue(MockResponse.Builder().chunkedBody(Buffer().writeUtf8("apk"), maxChunkSize = 1).build())
        job(url = server.url("/app.apk").toString()).doWork() shouldBe ListenableWorker.Result.success()
        apkFile().readText() shouldBe "apk"
    }

    /** The progress notification (the worker's foreground one) is removed by WorkManager, not the job. */
    @Test
    fun httpErrorShowsRetry() = runTest {
        server.enqueue(MockResponse(code = 404))
        job(url = server.url("/app.apk").toString()).doWork()
        context.activeNotificationIds() shouldBe listOf(Notifications.ID_APP_UPDATER, Notifications.ID_APP_UPDATE_ERROR)
    }

    @Test
    fun cancelledStreamOnlyDismisses() = runTest {
        every { network.client } throws StreamResetException(ErrorCode.CANCEL)
        job(url = "https://example.org/app.apk").doWork()
        context.activeNotificationIds() shouldBe emptyList()
        every { network.client } throws StreamResetException(ErrorCode.PROTOCOL_ERROR)
        job(url = "https://example.org/app.apk").doWork()
        context.activeNotificationIds() shouldBe listOf(Notifications.ID_APP_UPDATER, Notifications.ID_APP_UPDATE_ERROR)
    }

    @Test
    fun cancellationOnlyDismisses() = runTest {
        every { network.client } throws CancellationException("stopped")
        job(url = "https://example.org/app.apk").doWork() shouldBe ListenableWorker.Result.success()
        context.activeNotificationIds() shouldBe emptyList()
    }

    @Test
    fun foregroundTypeFollowsTheSdk() = runTest {
        job(url = "u").getForegroundInfo().foregroundServiceType shouldBe
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        val sdk = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.P)
        try {
            job(url = "u").getForegroundInfo().foregroundServiceType shouldBe 0
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        }
    }

    @Test
    fun startAndStopTheWork() {
        val workManager = mockk<WorkManager>(relaxed = true)
        val requests = mutableListOf<OneTimeWorkRequest>()
        mockkObject(WorkManager)
        try {
            every { WorkManager.getInstance(any<Context>()) } returns workManager
            AppUpdateDownloadJob.start(context = context, url = "https://example.org/a.apk", title = "v3")
            AppUpdateDownloadJob.start(context = context, url = "https://example.org/a.apk")
            verify { workManager.enqueueUniqueWork(any(), any(), capture(requests)) }
            val titles = requests.map { it.workSpec.input.getString(AppUpdateDownloadJob.EXTRA_DOWNLOAD_TITLE) }
            titles shouldBe listOf("v3", null)
            AppUpdateDownloadJob.stop(context)
            verify { workManager.cancelUniqueWork("AppUpdateDownload") }
        } finally {
            unmockkObject(WorkManager)
        }
    }
}
