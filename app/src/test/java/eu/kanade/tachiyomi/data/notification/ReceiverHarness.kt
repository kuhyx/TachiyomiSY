package eu.kanade.tachiyomi.data.notification

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.source.service.SourceManager

/** A receiver action whose constant is private to [NotificationReceiver]. */
internal fun receiverAction(name: String): String = "${BuildConfig.APPLICATION_ID}.NotificationReceiver.$name"

/** A receiver extra whose constant is private to [NotificationReceiver]. */
internal fun receiverExtra(name: String): String = receiverAction(name)

/**
 * Everything [NotificationReceiver] pulls from Injekt, with WorkManager replaced by a relaxed mock
 * so the job `start`/`stop` helpers the actions call have somewhere to go.
 */
internal abstract class ReceiverTestBase {

    protected val context: Context = ApplicationProvider.getApplicationContext()
    protected val getManga: GetManga = mockk()
    protected val getChapter: GetChapter = mockk()
    protected val updateChapter: UpdateChapter = mockk(relaxed = true)
    protected val sourceManager: SourceManager = mockk()
    protected val downloadManager: DownloadManager = mockk(relaxed = true)
    protected val downloadPreferences: DownloadPreferences = DownloadPreferences(MapPreferenceStore())
    protected val workManager: WorkManager = mockk(relaxed = true)
    protected val receiver: NotificationReceiver = NotificationReceiver()

    @Before
    fun startReceiverKoin() {
        mockkObject(WorkManager)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        every { workManager.getWorkInfos(any()) } returns immediateFuture(emptyList())
        every { workManager.getWorkInfosForUniqueWork(any()) } returns immediateFuture(emptyList())
        startKoin {
            modules(
                module {
                    single { getManga }
                    single { getChapter }
                    single { updateChapter }
                    single { sourceManager }
                    single { downloadManager }
                    single { downloadPreferences }
                },
            )
        }
    }

    @After
    fun stopReceiverKoin() {
        stopKoin()
        unmockkAll()
    }

    protected fun receive(action: String, extras: Intent.() -> Unit = {}) {
        receiver.onReceive(context, Intent(action).apply(extras))
    }
}

/** Posts a notification, optionally in [group], through the real (shadowed) manager. */
internal fun postNotification(context: Context, id: Int, group: String? = null) {
    val notification = androidx.core.app.NotificationCompat.Builder(context, Notifications.CHANNEL_COMMON)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setGroup(group)
        .build()
    context.getSystemService(android.app.NotificationManager::class.java).notify(id, notification)
}

internal fun activeIds(context: Context): List<Int> =
    context.getSystemService(android.app.NotificationManager::class.java).activeNotifications.map { it.id }.sorted()
