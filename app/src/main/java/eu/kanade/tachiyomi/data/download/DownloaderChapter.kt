package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.domain.chapter.model.toSChapter
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.storage.DiskUtil
import exh.util.DataSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.MR
import java.io.IOException

// Arbitrary minimum required space to start a download: 200 MB
private const val MIN_DISK_SPACE = 200L * 1024 * 1024

/** The manga's download directory, or null (with the download already failed) when it is unavailable or full. */
internal fun Downloader.mangaDirWithSpace(download: Download): UniFile? {
    val mangaDir = provider.getMangaDir(/* SY --> */ download.manga.ogTitle /* SY <-- */, download.source)
        .getOrElse { e ->
            download.transition(Download.State.ERROR)
            notifier.onError(e.message, download.chapter.name, download.manga.title, download.manga.id)
            return null
        }
    val availSpace = DiskUtil.getAvailableStorageSpace(mangaDir)
    if (availSpace != -1L && availSpace < MIN_DISK_SPACE) {
        download.transition(Download.State.ERROR)
        notifier.onError(
            context.stringResource(MR.strings.download_insufficient_space),
            download.chapter.name,
            download.manga.title,
            download.manga.id,
        )
        return null
    }
    return mangaDir
}

/** Pulls the page list from the source and stores it on the download. */
internal suspend fun Downloader.fetchPageList(download: Download): List<Page> {
    val pages = download.source.getPageList(download.chapter.toSChapter())
    if (pages.isEmpty()) {
        throw IOException(context.stringResource(MR.strings.page_list_empty_error))
    }
    // Don't trust index from source
    val reIndexedPages = pages.mapIndexed { index, page -> Page(index, page.url, page.imageUrl, page.uri) }
    download.pages = reIndexedPages
    return reIndexedPages
}

/**
 * Downloads every page into [tmpDir], [DownloadPreferences.parallelPageLimit] at a time; images already on
 * disk are kept.
 */
internal suspend fun Downloader.downloadPages(download: Download, pageList: List<Page>, tmpDir: UniFile) {
    val dataSaver = if (sourcePreferences.dataSaverDownloader.get()) {
        DataSaver(download.source, sourcePreferences)
    } else {
        DataSaver.NoOp
    }
    pageList.asFlow().flatMapMerge(concurrency = downloadPreferences.parallelPageLimit.get()) { page ->
        flow {
            // Fetch image URL if necessary
            if (page.imageUrl.isNullOrEmpty()) {
                page.status = Page.State.LoadPage
                try {
                    page.imageUrl = download.source.getImageUrl(page)
                } catch (expected: Throwable) {
                    // Any failure ends here and the fallback below applies.
                    page.status = Page.State.Error(expected)
                }
            }
            withIOContext { getOrDownloadImage(page, download, tmpDir, dataSaver) }
            emit(page)
        }
            .flowOn(Dispatchers.IO)
    }
        .collect {
            // Do when page is downloaded.
            notifier.onProgressChange(download)
        }
}

/** Writes ComicInfo, moves the finished directory into place (or archives it) and registers it in the cache. */
internal suspend fun Downloader.finishChapter(
    download: Download,
    mangaDir: UniFile,
    chapterDirname: String,
    tmpDir: UniFile,
) {
    createComicInfoFile(tmpDir, download.manga, download.chapter, download.source)
    // Only rename the directory if it's downloaded
    if (downloadPreferences.saveChaptersAsCBZ.get()) {
        archiveChapter(mangaDir, chapterDirname, tmpDir)
    } else {
        tmpDir.renameTo(chapterDirname)
    }
    cache.addChapter(chapterDirname, mangaDir, download.manga)
    DiskUtil.createNoMediaFile(tmpDir, context)
    download.transition(Download.State.DOWNLOADED)
}
