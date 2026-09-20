package eu.kanade.tachiyomi.ui.reader

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.api.get

internal fun ReaderActivity.openMangaScreen() {
    viewModel.manga?.id?.let { id ->
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = Constants.SHORTCUT_MANGA
                putExtra(Constants.MANGA_EXTRA, id)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
        )
    }
}

internal fun ReaderActivity.openChapterInWebView() {
    val manga = viewModel.manga ?: return
    val source = viewModel.getSource() ?: return
    assistUrl?.let {
        val intent = WebViewActivity.newIntent(this, it, source.id, manga.title)
        startActivity(intent)
    }
}

internal fun ReaderActivity.openChapterInBrowser() {
    assistUrl?.let {
        openInBrowser(it.toUri(), forceDefaultBrowser = false)
    }
}

internal fun ReaderActivity.shareChapter() {
    assistUrl?.let {
        val intent = it.toUri().toShareIntent(this, type = "text/plain")
        startActivity(intent)
    }
}

internal fun ReaderActivity.showReadingModeToast(mode: Int) {
    try {
        readingModeToast?.cancel()
        readingModeToast = toast(ReadingMode.fromPreference(mode).stringRes)
    } catch (_: ArrayIndexOutOfBoundsException) {
        logcat(LogPriority.ERROR) { "Unknown reading mode: $mode" }
    }
}

// Forces the user preferred [orientation] on the activity.
internal fun ReaderActivity.setOrientation(orientation: Int) {
    val newOrientation = ReaderOrientation.fromPreference(orientation)
    if (newOrientation.flag != requestedOrientation) {
        requestedOrientation = newOrientation.flag
    }
}
