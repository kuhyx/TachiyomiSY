package tachiyomi.source.local

import android.content.Context
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.source.local.filter.OrderBy
import tachiyomi.source.local.image.LocalCoverManager
import java.io.File
import kotlin.time.Duration.Companion.days

/** [LocalMangaBrowser.search] over real folders: visibility, query, recency and the title sort. */
@RunWith(RobolectricTestRunner::class)
internal class LocalMangaBrowserTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        val alpha = folder.newFolder("Alpha")
        File(alpha, "cover.png").writeBytes(PNG_HEADER)
        alpha.setLastModified(now - 10.days.inWholeMilliseconds)
        newManga("beta", modified = now)
        newManga("gamma", modified = now - 1.days.inWholeMilliseconds)
        newManga(".hidden", modified = now)
        folder.newFile("stray.txt")
    }

    private fun newManga(name: String, modified: Long): File =
        folder.newFolder(name).apply { setLastModified(modified) }

    private fun browser(allowHidden: Boolean = false): LocalMangaBrowser {
        val fileSystem = fileSystemOver(folder.root)
        return LocalMangaBrowser(fileSystem, LocalCoverManager(context, fileSystem)) { allowHidden }
    }

    private fun popular(ascending: Boolean = true): FilterList =
        FilterList(OrderBy.Popular(context).apply { state = Filter.Sort.Selection(0, ascending) })

    private fun titlesOf(page: MangasPage): List<String> = page.mangas.map { it.title }

    @Test
    fun listsVisibleFoldersByTitle() = runTest {
        val page = browser().search("", popular(), latestOnly = false)
        page.hasNextPage shouldBe false
        titlesOf(page) shouldBe listOf("Alpha", "beta", "gamma")
        page.mangas.map { it.url } shouldBe listOf("Alpha", "beta", "gamma")
        page.mangas[0].thumbnail_url shouldEndWith "/Alpha/cover.png"
        page.mangas[1].thumbnail_url.shouldBeNull()
    }

    @Test
    fun hiddenFoldersNeedPermission() = runTest {
        val page = browser(allowHidden = true).search("", popular(), latestOnly = false)
        titlesOf(page) shouldBe listOf(".hidden", "Alpha", "beta", "gamma")
    }

    @Test
    fun queryFiltersByTitle() = runTest {
        titlesOf(browser().search("ETA", popular(), latestOnly = false)) shouldBe listOf("beta")
        titlesOf(browser().search("zzz", popular(), latestOnly = false)) shouldBe emptyList()
    }

    @Test
    fun latestOnlyKeepsRecentFolders() = runTest {
        val page = browser().search("", FilterList(OrderBy.Latest(context)), latestOnly = true)
        titlesOf(page) shouldBe listOf("beta", "gamma")
    }

    @Test
    fun titleSortCanBeDescending() = runTest {
        val page = browser().search("", popular(ascending = false), latestOnly = false)
        titlesOf(page) shouldBe listOf("gamma", "beta", "Alpha")
    }

    @Test
    fun otherFiltersLeaveTheOrder() = runTest {
        val page = browser().search("", FilterList(Filter.Header("x")), latestOnly = false)
        titlesOf(page).toSet() shouldBe setOf("Alpha", "beta", "gamma")
    }
}
