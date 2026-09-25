package eu.kanade.tachiyomi.data.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.Constants
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class NotificationReceiverEntriesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manga: Manga = Manga.create().copy(id = 12L)

    @Test
    fun openChapterTargetsTheReader() {
        val chapter = Chapter.create().copy(id = 34L)
        val intent = NotificationReceiver.openChapterPendingActivity(
            context = context,
            manga = manga,
            chapter = chapter,
        ).intent()
        intent.component!!.className shouldBe "eu.kanade.tachiyomi.ui.reader.ReaderActivity"
    }

    @Test
    fun openEntryByGroupTargetsMain() {
        val intent = NotificationReceiver.openChapterPendingActivity(
            context = context,
            manga = manga,
            groupId = 3,
        ).intent()
        intent.action shouldBe Constants.SHORTCUT_MANGA
        intent.getLongExtra(Constants.MANGA_EXTRA, -1) shouldBe 12L
        intent.getIntExtra("notificationId", -1) shouldBe 12L.hashCode()
        intent.getIntExtra("groupId", -1) shouldBe 3
    }

    @Test
    fun markAsReadCarriesUrlsAndIds() {
        val chapters = arrayOf(Chapter.create().copy(url = "/one"), Chapter.create().copy(url = "/two"))
        val intent = NotificationReceiver.markAsReadPendingBroadcast(
            context = context,
            manga = manga,
            chapters = chapters,
            groupId = 8,
        ).intent()
        intent.action shouldBe NotificationReceiver.ACTION_MARK_AS_READ
        intent.getStringArrayExtra(NotificationReceiver.EXTRA_CHAPTER_URL)!!.toList() shouldContainExactly
            listOf("/one", "/two")
        intent.getLongExtra(NotificationReceiver.EXTRA_MANGA_ID, -1) shouldBe 12L
        intent.getIntExtra(NotificationReceiver.EXTRA_GROUP_ID, -1) shouldBe 8
    }

    @Test
    fun openEntryByIdTargetsMain() {
        val intent = NotificationReceiver.openEntryPendingActivity(context = context, mangaId = 99L).intent()
        intent.action shouldBe Constants.SHORTCUT_MANGA
        intent.component!!.className shouldBe "eu.kanade.tachiyomi.ui.main.MainActivity"
        intent.getLongExtra(Constants.MANGA_EXTRA, -1) shouldBe 99L
        intent.getIntExtra("notificationId", -1) shouldBe 99L.hashCode()
    }
}
