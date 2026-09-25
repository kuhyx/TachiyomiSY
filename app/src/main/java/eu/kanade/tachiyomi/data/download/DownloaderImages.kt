package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.storage.DiskUtil.NOMEDIA_FILE
import eu.kanade.tachiyomi.util.storage.saveTo
import exh.source.isEhBasedSource
import exh.util.DataSaver
import exh.util.DataSaver.Companion.getImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.single
import logcat.LogPriority
import okhttp3.Response
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.i18n.MR
import uy.kohesive.injekt.api.get
import java.io.File
import java.net.HttpURLConnection
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

// Gets the image from the filesystem if it exists or downloads it otherwise.
// @param page the page to download.
// @param download the download of the page.
// @param tmpDir the temporary directory of the download.
internal suspend fun Downloader.getOrDownloadImage(
    page: Page,
    download: Download,
    tmpDir: UniFile,
    dataSaver: DataSaver,
) {
    // If the image URL is empty, do nothing
    if (page.imageUrl == null) {
        return
    }

    val digitCount = (download.pages?.size ?: 0).toString().length.coerceAtLeast(MIN_FILENAME_DIGITS)
    val filename = "%0${digitCount}d".format(Locale.ENGLISH, page.number)

    // Try to find the image file
    val imageFile = tmpDir.listFiles()?.firstOrNull { file ->
        file.name?.let { isDownloadedPageImage(it, filename) } == true
    }

    try {
        // If the image is already downloaded, do nothing. Otherwise download from network
        val file = when {
            imageFile != null -> imageFile
            chapterCache.isImageInCache(page.imageUrl!!) ->
                copyImageFromCache(chapterCache.getImageFile(page.imageUrl!!), tmpDir, filename)

            else -> downloadImage(page, download.source, tmpDir, filename, dataSaver)
        }

        // When the page is ready, set page path, progress (just in case) and status
        splitTallImageIfNeeded(page, tmpDir)

        page.uri = file.uri
        page.progress = PROGRESS_DONE
        page.status = Page.State.Ready
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (expected: Throwable) {
        // Rethrown (or wrapped) whatever the cause.
        // Mark this page as error and allow to download the remaining
        page.progress = 0
        page.status = Page.State.Error(expected)
        notifier.onError(expected.message, download.chapter.name, download.manga.title, download.manga.id)
    }
}

// Downloads the image from network to a file in tmpDir.
// @param page the page to download.
// @param source the source of the page.
// @param tmpDir the temporary directory of the download.
// @param filename the filename of the image.
internal suspend fun Downloader.downloadImage(
    page: Page,
    source: HttpSource,
    tmpDir: UniFile,
    filename: String,
    dataSaver: DataSaver,
): UniFile {
    page.status = Page.State.DownloadImage
    page.progress = 0
    return flow {
        val file = tmpDir.findFile(inProgressFileName(filename))
            ?: tmpDir.createFile(inProgressFileName(filename))!!

        try {
            source.getImage(page, dataSaver = dataSaver).use {
                it.body.source().saveTo(
                    // If the server supports partial downloads (HTTP 206),
                    // append to the existing file.
                    // Otherwise, start from scratch and overwrite the file.
                    stream = file.openOutputStream(it.code == HttpURLConnection.HTTP_PARTIAL),
                )
                val extension = getImageExtension(it, file)
                file.renameTo("$filename.$extension")
            }
        } catch (e: HttpException) {
            if (e.code == HTTP_RANGE_NOT_SATISFIABLE) {
                file.delete()
            }
            throw e
        }
        emit(file)
    }
        // Retry 3 times, waiting 2, 4 and 8 seconds between attempts.
        .retryWhen { _, attempt ->
            if (attempt < DOWNLOAD_RETRIES) {
                delay((2L shl attempt.toInt()).seconds)
                if (source.isEhBasedSource()) {
                    page.imageUrl = source.getImageUrl(page)
                }
                true
            } else {
                false
            }
        }
        // The flow emits exactly once; single() lets it complete instead of aborting inside emit.
        .single()
}

// Copies the image from cache to file in tmpDir.
// @param cacheFile the file from cache.
// @param tmpDir the temporary directory of the download.
// @param filename the filename of the image.
internal fun Downloader.copyImageFromCache(cacheFile: File, tmpDir: UniFile, filename: String): UniFile {
    // Delete temp file if it exists
    tmpDir.findFile(inProgressFileName(filename))?.delete()
    val tmpFile = tmpDir.createFile(inProgressFileName(filename))!!
    cacheFile.inputStream().use { input ->
        tmpFile.openOutputStream().use { output ->
            input.copyTo(output)
        }
    }
    val extension = ImageUtil.findImageType(cacheFile.inputStream()) ?: return tmpFile
    tmpFile.renameTo("$filename.${extension.extension}")
    cacheFile.delete()
    return tmpFile
}

// Returns the extension of the downloaded image from the network response, or if it's null,
// analyze the file. If everything fails, assume it's a jpg.
// @param response the network response of the image.
// @param file the file where the image is already downloaded.
internal fun Downloader.getImageExtension(response: Response, file: UniFile): String {
    val mime = response.body.contentType()?.run { if (type == "image") "image/$subtype" else null }
    return ImageUtil.getExtensionFromMimeType(mime) { file.openInputStream() }
}

internal fun Downloader.splitTallImageIfNeeded(page: Page, tmpDir: UniFile) {
    if (!downloadPreferences.splitTallImages.get()) return

    try {
        val filenamePrefix = "%03d".format(Locale.ENGLISH, page.number)
        val imageFile = tmpDir.listFiles()?.firstOrNull { it.name.orEmpty().startsWith(filenamePrefix) }
            ?: error(context.stringResource(MR.strings.download_notifier_split_page_not_found, page.number))

        // If the original page was previously split, then skip (the file was picked by its name, so it has one)
        if (imageFile.name!!.startsWith("${filenamePrefix}__")) return

        ImageUtil.splitTallImage(
            tmpDir,
            imageFile,
            filenamePrefix,
        )
    } catch (expected: Exception) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected) { "Failed to split downloaded image" }
    }
}

// Checks if the download was successful.
// @param download the download to check.
// @param tmpDir the directory where the download is currently stored.
internal fun Downloader.isDownloadSuccessful(
    download: Download,
    tmpDir: UniFile,
): Boolean {
    // Page list hasn't been initialized
    val downloadPageCount = download.pages?.size ?: return false

    // Ensure that all pages have been downloaded
    if (download.downloadedImages != downloadPageCount) {
        return false
    }

    // Ensure that the chapter folder has all the pages
    val downloadedImagesCount = tmpDir.listFiles().orEmpty().count {
        val fileName = it.name.orEmpty()
        when {
            fileName in listOf(COMIC_INFO_FILE, NOMEDIA_FILE) -> false
            fileName.endsWith(".tmp") -> false
            // Only count the first split page and not the others
            fileName.contains("__") && !fileName.endsWith("__001.jpg") -> false
            else -> true
        }
    }
    return downloadedImagesCount == downloadPageCount
}

// Checks if the file name matches a downloaded page image.
// @param fileName Name of the file to check
// @param pagePrefix Expected page prefix (e.g., "001")
internal fun Downloader.isDownloadedPageImage(fileName: String, pagePrefix: String): Boolean =
    !fileName.endsWith(".tmp") && (
        fileName.startsWith("$pagePrefix.") ||
            fileName.startsWith("${pagePrefix}__001.")
        )
