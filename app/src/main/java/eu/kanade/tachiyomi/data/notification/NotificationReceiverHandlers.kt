@file:OptIn(DelicateCoroutinesApi::class)

package eu.kanade.tachiyomi.data.notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.kanade.tachiyomi.data.download.deleteChapters
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.chapter.model.toChapterUpdate
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// Called to start share intent to share image.
// @param context context of application
// @param uri path of file
internal fun NotificationReceiver.shareImage(context: Context, uri: Uri) {
    context.startActivity(uri.toShareIntent(context))
}

// Called to start share intent to share backup file.
// @param context context of application
// @param path path of file
internal fun NotificationReceiver.shareFile(context: Context, uri: Uri, fileMimeType: String) {
    context.startActivity(uri.toShareIntent(context, fileMimeType))
}

// Starts reader activity.
// @param context context of application
// @param mangaId id of manga
// @param chapterId id of chapter
internal fun NotificationReceiver.openChapter(context: Context, mangaId: Long, chapterId: Long) {
    val manga = runBlocking { getManga.await(mangaId) }
    val chapter = runBlocking { getChapter.await(chapterId) }
    if (manga != null && chapter != null) {
        val intent = ReaderActivity.newIntent(context, manga.id, chapter.id).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    } else {
        context.toast(MR.strings.chapter_error)
    }
}

// Method called when user wants to mark manga chapters as read.
// @param chapterUrls URLs of chapter to mark as read
// @param mangaId id of manga
internal fun NotificationReceiver.markAsRead(chapterUrls: Array<String>, mangaId: Long) {
    val downloadPreferences: DownloadPreferences = Injekt.get()
    val sourceManager: SourceManager = Injekt.get()

    launchIO {
        val toUpdate = chapterUrls.mapNotNull { getChapter.await(it, mangaId) }
            .map {
                val chapter = it.copy(read = true)
                if (downloadPreferences.removeAfterMarkedAsRead.get()) {
                    val manga = getManga.await(mangaId)
                    if (manga != null) {
                        val source = sourceManager.get(manga.source)
                        if (source != null) {
                            downloadManager.deleteChapters(listOf(it), manga, source)
                        }
                    }
                }
                chapter.toChapterUpdate()
            }
        updateChapter.awaitAll(toUpdate)
    }
}

// Method called when user wants to download chapters.
// @param chapterUrls URLs of chapter to download
// @param mangaId id of manga
internal fun NotificationReceiver.downloadChapters(chapterUrls: Array<String>, mangaId: Long) {
    launchIO {
        val manga = getManga.await(mangaId)
        if (manga != null) {
            val chapters = chapterUrls.mapNotNull { getChapter.await(it, mangaId) }
            downloadManager.downloadChapters(manga, chapters)
        }
    }
}
