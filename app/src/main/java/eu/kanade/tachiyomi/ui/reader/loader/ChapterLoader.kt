package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import mihon.core.common.archive.archiveReader
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.io.Format
import java.io.IOException

/**
 * Loader used to retrieve the [PageLoader] for a given chapter.
 */
internal class ChapterLoader(
    services: Services,
    private val manga: Manga,
    private val source: Source,
    // SY -->
    merged: MergedData,
    // SY <--
) {
    private val context = services.context
    private val downloadManager = services.downloadManager
    private val downloadProvider = services.downloadProvider
    private val sourceManager = services.sourceManager
    private val readerPrefs = services.readerPrefs
    private val mergedReferences = merged.references
    private val mergedManga = merged.manga

    /** The app-wide services a loader needs, the same for every chapter. */
    data class Services(
        val context: Context,
        val downloadManager: DownloadManager,
        val downloadProvider: DownloadProvider,
        val sourceManager: SourceManager,
        val readerPrefs: ReaderPreferences,
    )

    /** The merged-manga references and their manga, so a merged chapter can find its real source. */
    data class MergedData(
        val references: List<MergedMangaReference>,
        val manga: Map<Long, Manga>,
    )

    /**
     * Assigns the chapter's page loader and loads the its pages. Returns immediately if the chapter
     * is already loaded.
     */
    suspend fun loadChapter(chapter: ReaderChapter /* SY --> */, page: Int? = null/* SY <-- */) {
        if (chapterIsReady(chapter)) {
            return
        }

        chapter.state = ReaderChapter.State.Loading
        withIOContext {
            logcat { "Loading pages for ${chapter.chapter.name}" }
            try {
                val loader = getPageLoader(chapter)
                chapter.pageLoader = loader

                val pages = loader.getPages()
                    .onEach { it.chapter = chapter }

                if (pages.isEmpty()) {
                    throw IOException(context.stringResource(MR.strings.page_list_empty_error))
                }

                // If the chapter is partially read, set the starting page to the last the user read
                // otherwise use the requested page.
                if (!chapter.chapter.read /* --> EH */ ||
                    readerPrefs
                        .preserveReadingPosition
                        .get() ||
                    page != null // <-- EH
                ) {
                    chapter.requestedPage = /* SY --> */ page ?: /* SY <-- */ chapter.chapter.lastPageRead
                }

                chapter.state = ReaderChapter.State.Loaded(pages)
            } catch (expected: Throwable) {
                // Rethrown (or wrapped) whatever the cause.
                chapter.state = ReaderChapter.State.Error(expected)
                throw expected
            }
        }
    }

    // Checks [chapter] to be loaded based on present pages and loader in addition to state.
    private fun chapterIsReady(chapter: ReaderChapter): Boolean =
        chapter.state is ReaderChapter.State.Loaded && chapter.pageLoader != null

    // Returns the page loader to use for this [chapter].
    private fun getPageLoader(chapter: ReaderChapter): PageLoader {
        // SY -->
        if (source is MergedSource) return getMergedPageLoader(chapter)
        // SY <--
        val dbChapter = chapter.chapter
        val isDownloaded = downloadManager.isChapterDownloaded(
            dbChapter.name,
            dbChapter.scanlator,
            dbChapter.url,
            /* SY --> */ manga.ogTitle, /* SY <-- */
            manga.source,
            skipCache = true,
        )
        return when {
            isDownloaded -> DownloadPageLoader(chapter, manga, source, downloadManager, downloadProvider)
            source is LocalSource -> localPageLoader(source, chapter)
            source is HttpSource -> HttpPageLoader(chapter, source)
            source is StubSource -> error(context.stringResource(MR.strings.source_not_installed, source.toString()))
            else -> error(context.stringResource(MR.strings.loader_not_implemented_error))
        }
    }

    // SY -->

    // A merged chapter loads through the source and manga it was merged from, not the merged entry.
    private fun getMergedPageLoader(chapter: ReaderChapter): PageLoader {
        val mangaReference = mergedReferences.firstOrNull { it.mangaId == chapter.chapter.mangaId }
            ?: error("Merge reference null")
        val source = sourceManager.get(mangaReference.mangaSourceId)
            ?: error("Source ${mangaReference.mangaSourceId} was null")
        val manga = mergedManga[chapter.chapter.mangaId] ?: error("Manga for merged chapter was null")
        val isMergedMangaDownloaded = downloadManager.isChapterDownloaded(
            chapterName = chapter.chapter.name,
            chapterScanlator = chapter.chapter.scanlator,
            chapterUrl = chapter.chapter.url,
            mangaTitle = manga.ogTitle,
            sourceId = manga.source,
            skipCache = true,
        )
        return when {
            isMergedMangaDownloaded -> DownloadPageLoader(chapter, manga, source, downloadManager, downloadProvider)
            source is HttpSource -> HttpPageLoader(chapter, source)
            source is LocalSource -> localPageLoader(source, chapter)
            else -> error(context.stringResource(MR.strings.loader_not_implemented_error))
        }
    }
    // SY <--

    private fun localPageLoader(source: LocalSource, chapter: ReaderChapter): PageLoader {
        return when (val format = source.getFormat(chapter.chapter)) {
            is Format.Directory -> DirectoryPageLoader(format.file)
            is Format.Archive -> ArchivePageLoader(format.file.archiveReader(context))
            is Format.Epub -> EpubPageLoader(format.file.archiveReader(context))
        }
    }
}
