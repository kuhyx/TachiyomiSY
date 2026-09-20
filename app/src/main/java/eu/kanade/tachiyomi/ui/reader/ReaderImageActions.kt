package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.saver.Image
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.saver.Location
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Dialog
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SaveImageResult
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.util.editCover
import eu.kanade.tachiyomi.util.lang.byteSize
import eu.kanade.tachiyomi.util.lang.takeBytes
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.DiskUtil.MAX_FILE_NAME_BYTES
import eu.kanade.tachiyomi.util.storage.cacheImageDir
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.decoder.ImageDecoder
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * What the reader does with a page image: save it (or a spread) to Pictures, share or copy it,
 * set it as the cover. Composed by [ReaderViewModel], whose state and event channel it uses.
 */
internal class ReaderImageActions(
    private val model: ReaderViewModel,
    private val readerPreferences: ReaderPreferences,
    private val imageSaver: ImageSaver = Injekt.get(),
) {
    // Generate a filename for the given [manga] and [page].
    private fun generateFilename(
        manga: Manga,
        page: ReaderPage,
    ): String {
        val chapter = page.chapter.chapter
        val filenameSuffix = " - ${page.number}"
        return DiskUtil.buildValidFilename(
            "${manga.title} - ${chapter.name}",
            DiskUtil.MAX_FILE_NAME_BYTES - filenameSuffix.byteSize(),
        ) + filenameSuffix
    }

    fun saveImage(useExtraPage: Boolean) {
        // SY -->
        val page = if (useExtraPage) {
            (model.state.value.dialog as? Dialog.PageActions)?.extraPage
        } else {
            (model.state.value.dialog as? Dialog.PageActions)?.page
        }
        // SY <--
        if (page?.status != Page.State.Ready) return
        val manga = model.manga ?: return

        val context = Injekt.get<Application>()
        val notifier = SaveImageNotifier(context)
        notifier.onClear()

        val filename = generateFilename(manga, page)

        // Pictures directory.
        val relativePath = if (readerPreferences.folderPerManga.get()) {
            DiskUtil.buildValidFilename(
                manga.title,
            )
        } else {
            ""
        }

        // Copy file in background.
        model.viewModelScope.launchNonCancellable {
            try {
                val uri = imageSaver.save(
                    image = Image.Page(
                        inputStream = page.stream!!,
                        name = filename,
                        location = Location.Pictures.create(relativePath),
                    ),
                )
                withUIContext {
                    notifier.onComplete(uri)
                    model.eventChannel.send(Event.SavedImage(SaveImageResult.Success(uri)))
                }
            } catch (expected: Throwable) {
                // Any failure ends here and the fallback below applies.
                notifier.onError(expected.message)
                model.eventChannel.send(Event.SavedImage(SaveImageResult.Error(expected)))
            }
        }
    }

    // SY -->
    fun saveImages() {
        val (firstPage, secondPage) = model.state.value.dialog as? Dialog.PageActions ?: return
        val viewer = model.state.value.viewer as? PagerViewer ?: return
        val isLTR = (viewer !is R2LPagerViewer) xor viewer.config.invertDoublePages
        val bg = viewer.config.pageCanvasColor

        if (firstPage.status != Page.State.Ready) return
        if (secondPage?.status != Page.State.Ready) return

        val manga = model.manga ?: return

        val context = Injekt.get<Application>()
        val notifier = SaveImageNotifier(context)
        notifier.onClear()

        // Copy file in background.
        model.viewModelScope.launchNonCancellable {
            try {
                val uri = saveImages(
                    page1 = firstPage,
                    page2 = secondPage,
                    isLTR = isLTR,
                    bg = bg,
                    location = Location.Pictures.create(DiskUtil.buildValidFilename(manga.title)),
                    manga = manga,
                )
                model.eventChannel.send(Event.SavedImage(SaveImageResult.Success(uri)))
            } catch (expected: Throwable) {
                // Any failure ends here and the fallback below applies.
                notifier.onError(expected.message)
                model.eventChannel.send(Event.SavedImage(SaveImageResult.Error(expected)))
            }
        }
    }

    private fun saveImages(
        page1: ReaderPage,
        page2: ReaderPage,
        isLTR: Boolean,
        @ColorInt bg: Int,
        location: Location,
        manga: Manga,
    ): Uri {
        val stream1 = page1.stream!!
        ImageUtil.findImageType(stream1) ?: throw IllegalArgumentException("Not an image")
        val stream2 = page2.stream!!
        ImageUtil.findImageType(stream2) ?: throw IllegalArgumentException("Not an image")
        val imageBitmap = ImageDecoder.newInstance(stream1())?.decode()!!
        val imageBitmap2 = ImageDecoder.newInstance(stream2())?.decode()!!

        val chapter = page1.chapter.chapter

        // Build destination file.
        val filenameSuffix = " - ${page1.number}-${page2.number}.jpg"
        val filename = DiskUtil.buildValidFilename(
            "${manga.title} - ${chapter.name}".takeBytes(MAX_FILE_NAME_BYTES - filenameSuffix.byteSize()),
        ) + filenameSuffix

        return imageSaver.save(
            image = Image.Page(
                inputStream = { ImageUtil.mergeBitmaps(imageBitmap, imageBitmap2, isLTR, 0, bg).inputStream() },
                name = filename,
                location = location,
            ),
        )
    }
    // SY <--

    /**
     * Shares the image of the selected page and notifies the UI with the path of the file to share.
     * The image must be first copied to the internal partition because there are many possible
     * formats it can come from, like a zipped chapter, in which case it's not possible to directly
     * get a path to the file and it has to be decompressed somewhere first. Only the last shared
     * image will be kept so it won't be taking lots of internal disk space.
     */
    fun shareImage(copyToClipboard: Boolean, useExtraPage: Boolean) {
        // SY -->
        val page = if (useExtraPage) {
            (model.state.value.dialog as? Dialog.PageActions)?.extraPage
        } else {
            (model.state.value.dialog as? Dialog.PageActions)?.page
        }
        // SY <--
        if (page?.status != Page.State.Ready) return
        val manga = model.manga ?: return

        val context = Injekt.get<Application>()
        val destDir = context.cacheImageDir

        val filename = generateFilename(manga, page)

        try {
            model.viewModelScope.launchNonCancellable {
                destDir.deleteRecursively()
                val uri = imageSaver.save(
                    image = Image.Page(
                        inputStream = page.stream!!,
                        name = filename,
                        location = Location.Cache,
                    ),
                )
                model.eventChannel.send(if (copyToClipboard) Event.CopyImage(uri) else Event.ShareImage(uri, page))
            }
        } catch (expected: Throwable) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
        }
    }

    // SY -->
    fun shareImages(copyToClipboard: Boolean) {
        val (firstPage, secondPage) = model.state.value.dialog as? Dialog.PageActions ?: return
        val viewer = model.state.value.viewer as? PagerViewer ?: return
        val isLTR = (viewer !is R2LPagerViewer) xor viewer.config.invertDoublePages
        val bg = viewer.config.pageCanvasColor

        if (firstPage.status != Page.State.Ready) return
        if (secondPage?.status != Page.State.Ready) return
        val manga = model.manga ?: return

        val context = Injekt.get<Application>()
        val destDir = context.cacheImageDir

        try {
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
        } catch (expected: Throwable) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
        }
    }
    // SY <--

    /**
     * Sets the image of the selected page as cover and notifies the UI of the result.
     */
    fun setAsCover(useExtraPage: Boolean) {
        // SY -->
        val page = if (useExtraPage) {
            (model.state.value.dialog as? Dialog.PageActions)?.extraPage
        } else {
            (model.state.value.dialog as? Dialog.PageActions)?.page
        }
        // SY <--
        if (page?.status != Page.State.Ready) return
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
}
