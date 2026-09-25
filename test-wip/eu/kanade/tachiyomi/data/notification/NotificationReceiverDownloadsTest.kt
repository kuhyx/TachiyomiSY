package eu.kanade.tachiyomi.data.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.data.updater.AppUpdateDownloadJob
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

private fun chapter(url: String): Chapter = Chapter.create().copy(url = url)

@RunWith(RobolectricTestRunner::class)
internal class NotificationReceiverDownloadsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manga: Manga = Manga.create().copy(id = 77L)

    @Test
    fun resumeDownloadsAction() {
        val intent = NotificationReceiver.resumeDownloadsBroadcast(context).intent()
        intent.action shouldBe NotificationReceiver.ACTION_RESUME_DOWNLOADS
        intent.component!!.className shouldBe RECEIVER
    }

    @Test
    fun pauseDownloadsAction() {
        NotificationReceiver.pauseDownloadsPendingBroadcast(context).intent().action shouldBe
            NotificationReceiver.ACTION_PAUSE_DOWNLOADS
    }

    @Test
    fun clearDownloadsAction() {
        NotificationReceiver.clearDownloadsPendingBroadcast(context).intent().action shouldBe
            NotificationReceiver.ACTION_CLEAR_DOWNLOADS
    }

    @Test
    fun downloadChaptersCarriesUrls() {
        val intent = NotificationReceiver.downloadChaptersBroadcast(
            context = context,
            manga = manga,
            chapters = arrayOf(chapter("/a"), chapter("/b")),
            groupId = 5,
        ).intent()
        intent.action shouldBe NotificationReceiver.ACTION_DOWNLOAD_CHAPTER
        intent.getStringArrayExtra(NotificationReceiver.EXTRA_CHAPTER_URL)!!.toList() shouldContainExactly
            listOf("/a", "/b")
    }

    @Test
    fun downloadChaptersCarriesIds() {
        val intent = NotificationReceiver.downloadChaptersBroadcast(
            context = context,
            manga = manga,
            chapters = emptyArray(),
            groupId = 5,
        ).intent()
        intent.getLongExtra(NotificationReceiver.EXTRA_MANGA_ID, -1) shouldBe 77L
        intent.getIntExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, -1) shouldBe 77L.hashCode()
        intent.getIntExtra(NotificationReceiver.EXTRA_GROUP_ID, -1) shouldBe 5
    }

    @Test
    fun appUpdateWithoutTitle() {
        val intent = NotificationReceiver.downloadAppUpdateBroadcast(
            context = context,
            url = "https://example.org/app.apk",
        ).intent()
        intent.action shouldBe NotificationReceiver.ACTION_START_APP_UPDATE
        intent.getStringExtra(AppUpdateDownloadJob.EXTRA_DOWNLOAD_URL) shouldBe "https://example.org/app.apk"
        intent.getStringExtra(AppUpdateDownloadJob.EXTRA_DOWNLOAD_TITLE).shouldBeNull()
    }

    @Test
    fun appUpdateWithTitle() {
        val intent = NotificationReceiver.downloadAppUpdateBroadcast(
            context = context,
            url = "https://example.org/app.apk",
            title = "v1.2.3",
        ).intent()
        intent.getStringExtra(AppUpdateDownloadJob.EXTRA_DOWNLOAD_TITLE) shouldBe "v1.2.3"
    }

    @Test
    fun cancelAppUpdateAction() {
        NotificationReceiver.cancelAppUpdateBroadcast(context).intent().action shouldBe
            NotificationReceiver.ACTION_CANCEL_APP_UPDATE_DOWNLOAD
    }
}
