package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.saver.Image
import eu.kanade.tachiyomi.data.saver.Location
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Dialog
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.util.editCover
import eu.kanade.tachiyomi.util.storage.cacheImageDir
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Shares the image of the selected page and notifies the UI with the path of the file to share.
 * The image must be first copied to the internal partition because there are many possible
 * formats it can come from, like a zipped chapter, in which case it's not possible to directly
 * get a path to the file and it has to be decompressed somewhere first. Only the last shared
 * image will be kept so it won't be taking lots of internal disk space.
 */
internal fun ReaderImageActions.shareImage(copyToClipboard: Boolean, useExtraPage: Boolean) {
    val page = selectedPage(useExtraPage)?.takeIf { it.status == Page.State.Ready } ?: return
    val manga = model.manga ?: return
    val stream = page.stream ?: return

    val context = Injekt.get<Application>()
    val destDir = context.cacheImageDir

    val filename = generateFilename(manga, page)

    // Failures inside the launch go to the scope's handler: a launch itself never throws.
    model.viewModelScope.launchNonCancellable {
        destDir.deleteRecursively()
        val uri = imageSaver.save(
            image = Image.Page(
                inputStream = stream,
                name = filename,
                location = Location.Cache,
            ),
        )
        model.eventChannel.send(if (copyToClipboard) Event.CopyImage(uri) else Event.ShareImage(uri, page))
    }
}

// SY -->
internal fun ReaderImageActions.shareImages(copyToClipboard: Boolean) {
    val (firstPage, secondPage) = model.state.value.dialog as? Dialog.PageActions ?: return
    val viewer = model.state.value.viewer as? PagerViewer ?: return
    if (firstPage.status != Page.State.Ready) return
    if (secondPage?.status != Page.State.Ready) return
    val manga = model.manga ?: return
    val isLTR = (viewer !is R2LPagerViewer) xor viewer.config.invertDoublePages
    val bg = viewer.config.pageCanvasColor

    val context = Injekt.get<Application>()
    val destDir = context.cacheImageDir

    model.viewModelScope.launchNonCancellable {
        destDir.deleteRecursively()
        val uri = saveImages(
            page1 = firstPage,
            page2 = secondPage,
            isLTR = isLTR,
            bg = bg,
            location = Location.Cache,
            manga = manga,
        )
        val event = if (copyToClipboard) Event.CopyImage(uri) else Event.ShareImage(uri, firstPage, secondPage)
        model.eventChannel.send(event)
    }
}

/**
 * Sets the image of the selected page as cover and notifies the UI of the result.
 */
internal fun ReaderImageActions.setAsCover(useExtraPage: Boolean) {
    val page = selectedPage(useExtraPage)?.takeIf { it.status == Page.State.Ready } ?: return
    val manga = model.manga ?: return
    val stream = page.stream ?: return

    model.viewModelScope.launchNonCancellable {
        val result = try {
            manga.editCover(Injekt.get(), stream())
            if (manga.isLocal() || manga.favorite) {
                SetAsCoverResult.Success
            } else {
                SetAsCoverResult.AddToLibraryFirst
            }
        } catch (_: Exception) {
            // Any failure ends here and the fallback below applies.
            SetAsCoverResult.Error
        }
        model.eventChannel.send(Event.SetCoverResult(result))
    }
}
