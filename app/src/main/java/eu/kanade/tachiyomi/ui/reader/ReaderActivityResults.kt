package eu.kanade.tachiyomi.ui.reader

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.AddToLibraryFirst
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Error
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Success
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.api.get

/**
 * Called from the presenter when a page is ready to be shared. It shows Android's default
 * sharing tool.
 */
internal fun ReaderActivity.onShareImageResult(
    uri: Uri,
    page: ReaderPage /* SY --> */,
    secondPage: ReaderPage? = null /* SY <-- */,
) {
    val manga = viewModel.manga ?: return
    val chapter = page.chapter.chapter

    // SY -->
    val text = if (secondPage != null) {
        stringResource(
            SYMR.strings.share_pages_info, manga.title, chapter.name,
            if (resources.configuration.layoutDirection ==
                View.LAYOUT_DIRECTION_LTR
            ) {
                "${page.number}-${page.number + 1}"
            } else {
                "${page.number + 1}-${page.number}"
            },
        )
    } else {
        stringResource(MR.strings.share_page_info, manga.title, chapter.name, page.number)
    }
    // SY <--

    val intent = uri.toShareIntent(
        context = applicationContext,
        message = /* SY --> */ text, // SY <--
    )
    startActivity(intent)
}

internal fun ReaderActivity.onCopyImageResult(uri: Uri) {
    // Every Android build has a clipboard service.
    val clipboardManager: ClipboardManager = applicationContext.getSystemService(ClipboardManager::class.java)
    val clipData = ClipData.newUri(applicationContext.contentResolver, "", uri)
    clipboardManager.setPrimaryClip(clipData)
}

// Called from the presenter when a page is saved or fails. It shows a message or logs the
// event depending on the [result].
internal fun ReaderActivity.onSaveImageResult(result: ReaderViewModel.SaveImageResult) {
    when (result) {
        is ReaderViewModel.SaveImageResult.Success -> {
            toast(MR.strings.picture_saved)
        }

        is ReaderViewModel.SaveImageResult.Error -> {
            logcat(LogPriority.ERROR, result.error)
        }
    }
}

// Called from the presenter when a page is set as cover or fails. It shows a different message
// depending on the [result].
internal fun ReaderActivity.onSetAsCoverResult(result: ReaderViewModel.SetAsCoverResult) {
    toast(
        when (result) {
            Success -> MR.strings.cover_updated
            AddToLibraryFirst -> MR.strings.notification_first_add_to_library
            Error -> MR.strings.notification_cover_update_failed
        },
    )
}
