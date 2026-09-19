package tachiyomi.source.local

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.source.local.filter.OrderBy
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem

/** [LocalMangaBrowser.search] over mocked folders: nameless entries, duplicates and the date sort. */
@RunWith(RobolectricTestRunner::class)
internal class LocalMangaBrowserSortTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val coverManager = mockk<LocalCoverManager> { every { find(any()) } returns null }

    private fun entry(entryName: String?, modified: Long, isFolder: Boolean = true): UniFile = mockk {
        every { name } returns entryName
        every { isDirectory } returns isFolder
        every { lastModified() } returns modified
    }

    private fun browserOver(vararg entries: UniFile): LocalMangaBrowser {
        val fileSystem = mockk<LocalSourceFileSystem> { every { getFilesInBaseDirectory() } returns entries.toList() }
        return LocalMangaBrowser(fileSystem, coverManager) { false }
    }

    private fun popular(ascending: Boolean): FilterList =
        FilterList(OrderBy.Popular(context).apply { state = Filter.Sort.Selection(0, ascending) })

    private fun latest(ascending: Boolean): FilterList =
        FilterList(OrderBy.Latest(context).apply { state = Filter.Sort.Selection(1, ascending) })

    private suspend fun LocalMangaBrowser.titles(filters: FilterList, query: String = ""): List<String> =
        search(query, filters, latestOnly = false).mangas.map { it.title }

    @Test
    fun namelessFoldersSortAsEmpty() = runTest {
        val browser = browserOver(entry("B", 2), entry(null, 1), entry(null, 3), entry("a", 4))
        browser.titles(popular(ascending = true)) shouldBe listOf("", "a", "B")
        browser.titles(popular(ascending = false)) shouldBe listOf("B", "a", "")
        verify { coverManager.find("") }
    }

    @Test
    fun namelessFoldersNeverMatch() = runTest {
        val browser = browserOver(entry(null, 1), entry("Manga", 2))
        browser.titles(popular(ascending = true), query = "man") shouldBe listOf("Manga")
    }

    @Test
    fun nonFoldersAreSkipped() = runTest {
        val browser = browserOver(entry("file", 1, isFolder = false), entry("dir", 2))
        browser.titles(popular(ascending = true)) shouldBe listOf("dir")
    }

    @Test
    fun dateSortDescendsByModification() = runTest {
        val browser = browserOver(entry("b", 2), entry("a", 1), entry("c", 3))
        browser.titles(latest(ascending = false)) shouldBe listOf("c", "b", "a")
    }

    @Test
    fun dateSortAscendingIsOldestFirst() = runTest {
        val browser = browserOver(entry("b", 2), entry("a", 1), entry("c", 3))
        browser.titles(latest(ascending = true)) shouldBe listOf("a", "b", "c")
    }

    @Test
    fun sortWithoutASelectionFails() = runTest {
        val browser = browserOver(entry("a", 1))
        val popular = FilterList(OrderBy.Popular(context).apply { state = null })
        shouldThrow<IllegalStateException> { browser.titles(popular) }
        val latest = FilterList(OrderBy.Latest(context).apply { state = null })
        shouldThrow<IllegalStateException> { browser.titles(latest) }
    }
}
