package eu.kanade.tachiyomi.data.download

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class DownloadNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val security = SecurityPreferences(MapPreferenceStore())
    private val notifier by lazy { DownloadNotifier(context) }
    private val download = Download(
        source = mockk<HttpSource>(),
        manga = Manga.create().copy(id = 3L, ogTitle = "Title"),
        chapter = Chapter.create().copy(name = "Title - Chapter 1"),
    ).apply { pages = listOf(Page(0).apply { status = Page.State.Ready }, Page(1)) }

    private fun shown(id: Int): Notification? =
        Shadows.shadowOf(context.getSystemService(NotificationManager::class.java)).getNotification(id)

    private fun Notification.title(): String? = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

    private fun Notification.text(): String? = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

    @Before
    fun setUp() {
        Shadows.shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        startKoin { modules(module { single { security } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun progressNamesTheChapter() {
        notifier.onProgressChange(download)
        val progress = shown(Notifications.ID_DOWNLOAD_CHAPTER_PROGRESS)!!
        progress.title() shouldBe "Title - Chapter 1"
        progress.actions.size shouldBe 2
        notifier.onProgressChange(download)
        shown(Notifications.ID_DOWNLOAD_CHAPTER_PROGRESS)!!.actions.size shouldBe 2
    }

    @Test
    fun hiddenContentShowsOnlyProgress() {
        security.hideNotificationContent.set(true)
        notifier.onProgressChange(download)
        shown(Notifications.ID_DOWNLOAD_CHAPTER_PROGRESS)!!.text() shouldBe null
    }

    @Test
    fun pauseOffersResumeAndCancel() {
        notifier.onProgressChange(download)
        notifier.onPaused()
        shown(Notifications.ID_DOWNLOAD_CHAPTER_PROGRESS)!!.actions.size shouldBe 2
        notifier.onComplete()
        shown(Notifications.ID_DOWNLOAD_CHAPTER_PROGRESS) shouldBe null
    }

    @Test
    fun warningWithEverything() {
        val intent = PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        notifier.onWarning(reason = "slow down", timeout = 1_000L, contentIntent = intent, mangaId = 3L)
        val warning = shown(Notifications.ID_DOWNLOAD_CHAPTER_ERROR)!!
        warning.actions.size shouldBe 1
        warning.contentIntent shouldBe intent
        warning.timeoutAfter shouldBe 1_000L
    }

    @Test
    fun warningWithDefaults() {
        notifier.onWarning("slow down")
        shown(Notifications.ID_DOWNLOAD_CHAPTER_ERROR)!!.actions shouldBe null
    }

    @Test
    fun errorWithEverything() {
        notifier.onError(error = "boom", chapter = "Ch 1", mangaTitle = "Title", mangaId = 3L)
        val error = shown(Notifications.ID_DOWNLOAD_CHAPTER_ERROR)!!
        error.title() shouldBe "Title: Ch 1"
        error.text() shouldBe "boom"
        error.actions.size shouldBe 1
    }

    @Test
    fun errorWithDefaults() {
        notifier.onError()
        val error = shown(Notifications.ID_DOWNLOAD_CHAPTER_ERROR)!!
        error.title() shouldBe "Downloader"
        error.text() shouldBe "Could not download chapter due to unexpected error"
        notifier.dismissProgress()
    }
}
