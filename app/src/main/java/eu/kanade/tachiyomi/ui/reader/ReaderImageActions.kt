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
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.util.lang.byteSize
import eu.kanade.tachiyomi.util.lang.takeBytes
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.DiskUtil.MAX_FILE_NAME_BYTES
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.decoder.ImageDecoder
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * What the reader does with a page image: save it (or a spread) to Pictures, share or copy it,
 * set it as the cover. Composed by [ReaderViewModel], whose state and event channel it uses.
 */
internal class ReaderImageActions(
    internal val model: ReaderViewModel,
    private val readerPreferences: ReaderPreferences,
    internal val imageSaver: ImageSaver = Injekt.get(),
) {
    // Generate a filename for the given [manga] and [page].
    // SY --> The page the open page-actions dialog is about, or the second page of its spread.
    internal fun selectedPage(useExtraPage: Boolean): ReaderPage? {
        val dialog = model.state.value.dialog as? Dialog.PageActions
        return if (useExtraPage) dialog?.extraPage else dialog?.page
    }
    // SY <--

    internal fun generateFilename(
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
        val page = selectedPage(useExtraPage)
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
        if (firstPage.status != Page.State.Ready) return
        if (secondPage?.status != Page.State.Ready) return
        val manga = model.manga ?: return
        val isLTR = (viewer !is R2LPagerViewer) xor viewer.config.invertDoublePages
        val bg = viewer.config.pageCanvasColor

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

    internal fun saveImages(
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

    // SY <--
}
