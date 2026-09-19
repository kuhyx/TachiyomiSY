package tachiyomi.source.local

import android.content.Context
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.source.local.filter.OrderBy
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.Format
import tachiyomi.source.local.io.LocalSourceFileSystem
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.source.model.Source as DomainSource

/**
 * The built-in source backed by the local-source directory: one manga per folder, one chapter
 * per sub-folder, archive or EPUB, details from `ComicInfo.xml`.
 *
 * @param context resolves the source's display strings.
 * @param fileSystem the local-source directory.
 * @param coverManager finds and writes the per-manga cover file.
 * @param allowHiddenFiles whether dot-folders are listed as manga.
 */
public actual class LocalSource(
    private val context: Context,
    private val fileSystem: LocalSourceFileSystem,
    coverManager: LocalCoverManager,
    allowHiddenFiles: () -> Boolean,
) : Source, UnmeteredSource {

    private val json: Json by injectLazy()
    private val xml: XML by injectLazy()

    private val popularFilters = FilterList(OrderBy.Popular(context))
    private val latestFilters = FilterList(OrderBy.Latest(context))

    private val browser = LocalMangaBrowser(fileSystem, coverManager, allowHiddenFiles)
    private val comicInfoFiles by lazy { ComicInfoFiles(context, xml) }
    private val details by lazy { LocalMangaDetails(context, fileSystem, coverManager, json, comicInfoFiles) }
    private val formats = LocalChapterFormats(context, fileSystem)
    private val chapters by lazy { LocalChapterLister(context, fileSystem, coverManager, comicInfoFiles, formats) }
    private val infoWriter by lazy { LocalMangaInfoWriter(context, fileSystem, comicInfoFiles) }

    override val name: String = context.stringResource(MR.strings.local_source)

    override val id: Long = ID

    override val lang: String = "other"

    override val supportsLatest: Boolean = true

    override fun toString(): String = name

    // Browse related
    override suspend fun getPopularManga(page: Int): MangasPage = getSearchManga(page, "", popularFilters)

    override suspend fun getLatestUpdates(page: Int): MangasPage = getSearchManga(page, "", latestFilters)

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        browser.search(query, filters, latestOnly = filters === latestFilters)

    /** Writes [manga]'s edited details back into its directory's `ComicInfo.xml`. */
    public fun updateMangaInfo(manga: SManga) {
        infoWriter.write(manga)
    }

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = supervisorScope {
        val asyncManga = async { if (fetchDetails) details.fetch(manga) else manga }
        val asyncChapters = async { if (fetchChapters) this@LocalSource.chapters.list(manga) else chapters }
        SMangaUpdate(asyncManga.await(), asyncChapters.await())
    }

    // Filters
    override fun getFilterList(): FilterList = FilterList(OrderBy.Popular(context))

    // Unused stuff
    override suspend fun getPageList(chapter: SChapter): List<Page> = throw UnsupportedOperationException("Unused")

    /** The directory, archive or EPUB [chapter] is stored as; see [LocalChapterFormats.resolve]. */
    public fun getFormat(chapter: SChapter): Format = formats.resolve(chapter)

    /** The constants the app needs to recognise the local source and its files. */
    public companion object {
        /** The fixed source id of the local source. */
        public const val ID: Long = 0L

        /** The user guide for setting up the local source directory. */
        public const val HELP_URL: String = "https://mihon.app/docs/guides/local-source/"

        /** The encrypted archive that holds `ComicInfo.xml` for a manga whose chapters are encrypted. */
        public const val COMIC_INFO_ARCHIVE: String = "ComicInfo.cbm"
    }
}

/** Whether this manga belongs to the local source. */
public fun Manga.isLocal(): Boolean = source == LocalSource.ID

/** Whether this source is the local source. */
public fun Source.isLocal(): Boolean = id == LocalSource.ID

/** Whether this source is the local source. */
public fun DomainSource.isLocal(): Boolean = id == LocalSource.ID
