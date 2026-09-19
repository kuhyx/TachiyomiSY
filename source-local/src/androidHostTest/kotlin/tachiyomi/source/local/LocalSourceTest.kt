package tachiyomi.source.local

import android.content.Context
import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.source.local.filter.OrderBy
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.Format
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import java.io.File
import kotlin.time.Duration.Companion.days

/** [LocalSource] end to end over a real folder, with `Json` and `XML` served through Injekt. */
@RunWith(RobolectricTestRunner::class)
internal class LocalSourceTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val files = ComicInfoFiles(context, testXml)
    private var previousScope: InjektScope? = null

    private val source by lazy {
        val fileSystem = fileSystemOver(folder.root)
        LocalSource(
            context = context,
            fileSystem = fileSystem,
            coverManager = LocalCoverManager(context, fileSystem),
            allowHiddenFiles = { false },
        )
    }

    @Before
    fun setUp() {
        previousScope = installInjekt()
        val old = folder.newFolder("Old")
        File(old, COMIC_INFO_FILE).writeBytes(comicInfoXml(emptyComicInfo().copy(series = ComicInfo.Series("Series"))))
        File(old, "cover.jpg").writeBytes(PNG_HEADER)
        File(old, "c1").mkdir()
        File(old, "c1/001.png").writeBytes(PNG_HEADER)
        old.setLastModified(System.currentTimeMillis() - 10.days.inWholeMilliseconds)
        folder.newFolder("Recent")
    }

    @After
    fun tearDown() {
        previousScope?.let { Injekt = it }
    }

    @Test
    fun popularListsEveryFolderByTitle() = runTest {
        val page = source.getPopularManga(1)
        page.mangas.map { it.title } shouldBe listOf("Old", "Recent")
        page.hasNextPage shouldBe false
    }

    @Test
    fun latestListsRecentFoldersOnly() = runTest {
        source.getLatestUpdates(1).mangas.map { it.title } shouldBe listOf("Recent")
    }

    @Test
    fun searchIsNotLimitedToRecent() = runTest {
        val latest = source.getSearchManga(1, "", FilterList(OrderBy.Latest(context)))
        latest.mangas.map { it.title } shouldBe listOf("Recent", "Old")
        source.getSearchManga(1, "old", FilterList()).mangas.map { it.title } shouldBe listOf("Old")
    }

    @Test
    fun filterListSortsByTitle() {
        source.getFilterList().single().shouldBeInstanceOf<OrderBy.Popular>()
    }

    @Test
    fun pageListIsUnsupported() = runTest {
        shouldThrow<UnsupportedOperationException> { source.getPageList(sampleChapter("Old/c1")) }
    }

    @Test
    fun formatResolvesTheChapterFile() {
        source.getFormat(sampleChapter("Old/c1")).shouldBeInstanceOf<Format.Directory>()
    }

    @Test
    fun updateMangaInfoWritesComicInfo() {
        source.updateMangaInfo(sampleManga("Recent", "Edited"))
        files.parse(File(folder.root, "Recent/$COMIC_INFO_FILE").inputStream()).series?.value shouldBe "Edited"
    }

    @Test
    fun updateFetchesNothingByDefault() = runTest {
        val manga = sampleManga("Old")
        val chapters = listOf(sampleChapter("Old/c1"))
        val update = source.getMangaUpdate(
            manga = manga,
            chapters = chapters,
            fetchDetails = false,
            fetchChapters = false,
        )
        update.manga shouldBeSameInstanceAs manga
        update.chapters shouldBeSameInstanceAs chapters
        manga.title shouldBe "Old"
    }

    @Test
    fun updateFetchesDetailsOnRequest() = runTest {
        val manga = sampleManga("Old")
        val chapters = listOf(sampleChapter("Old/c1"))
        val update = source.getMangaUpdate(
            manga = manga,
            chapters = chapters,
            fetchDetails = true,
            fetchChapters = false,
        )
        update.manga shouldBeSameInstanceAs manga
        update.chapters shouldBeSameInstanceAs chapters
        manga.title shouldBe "Series"
        manga.thumbnail_url shouldEndWith "/Old/cover.jpg"
    }

    @Test
    fun updateFetchesChaptersOnRequest() = runTest {
        val manga = sampleManga("Old")
        val update = source.getMangaUpdate(
            manga = manga,
            chapters = emptyList(),
            fetchDetails = false,
            fetchChapters = true,
        )
        update.manga shouldBeSameInstanceAs manga
        update.chapters.map { it.url } shouldBe listOf("Old/c1")
        manga.title shouldBe "Old"
        val both = source.getMangaUpdate(
            manga = manga,
            chapters = emptyList(),
            fetchDetails = true,
            fetchChapters = true,
        )
        both.chapters.map { it.name } shouldBe listOf("c1")
        manga.title shouldBe "Series"
        manga.thumbnail_url shouldEndWith "/Old/cover.jpg"
    }
}
