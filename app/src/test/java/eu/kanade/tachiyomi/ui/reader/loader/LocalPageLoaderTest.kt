package eu.kanade.tachiyomi.ui.reader.loader

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.stubImageSniffing
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.core.common.archive.ArchiveReader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val OPF = """<package><manifest>
<item id="p1" href="p1.xhtml" media-type="application/xhtml+xml"/>
<item id="css" href="s.css" media-type="text/css"/>
</manifest><spine><itemref idref="p1"/><itemref idref="css"/></spine></package>"""

internal class LocalPageLoaderTest {

    @BeforeEach
    fun setUp() {
        stubImageSniffing()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun uniFile(name: String?, isDirectory: Boolean = false, bytes: ByteArray = byteArrayOf()): UniFile {
        val file = mockk<UniFile>()
        every { file.name } returns name
        every { file.isDirectory } returns isDirectory
        every { file.openInputStream() } answers { bytes.inputStream() }
        return file
    }

    @Test
    fun directoryKeepsSortedImages() = runTest {
        val dir = mockk<UniFile>()
        every { dir.listFiles() } returns arrayOf(
            uniFile("10.jpg", bytes = byteArrayOf(10)),
            uniFile("sub", isDirectory = true),
            uniFile("notes.txt", bytes = "text".toByteArray()),
            uniFile(null),
            uniFile("2.PNG", bytes = byteArrayOf(2)),
            uniFile("1.png", bytes = byteArrayOf(1)),
        )
        val loader = DirectoryPageLoader(dir)
        loader.file shouldBe dir
        loader.isLocal shouldBe true
        val pages = loader.getPages()
        pages.map { it.index } shouldBe listOf(0, 1, 2)
        pages.map { it.stream!!().read() } shouldBe listOf(1, 2, 10)
        pages.forEach { it.status shouldBe Page.State.Ready }
        loader.isLocal = false
        loader.isLocal shouldBe false
    }

    @Test
    fun directoryWithoutListing() = runTest {
        val dir = mockk<UniFile>()
        every { dir.listFiles() } returns null
        DirectoryPageLoader(dir).getPages().shouldBeEmpty()
    }

    @Test
    fun epubPagesStreamEntries() = runTest {
        val entries = mapOf(
            "OEBPS/content.opf" to OPF,
            "OEBPS/p1.xhtml" to """<html><body><img src="a.jpg"/><img src="b.jpg"/></body></html>""",
            "OEBPS/b.jpg" to "\u0007",
        )
        val reader = mockk<ArchiveReader>(relaxUnitFun = true)
        every { reader.getInputStream(any()) } answers { entries[firstArg<String>()]?.byteInputStream() }
        val loader = EpubPageLoader(reader)
        loader.isLocal shouldBe true
        val pages = loader.getPages()
        pages.map { it.index } shouldBe listOf(0, 1)
        pages[1].stream!!().read() shouldBe 7
        pages[0].status shouldBe Page.State.Ready
        loader.loadPage(pages[0])
        loader.recycle()
        loader.isRecycled shouldBe true
        verify { reader.close() }
        shouldThrow<IllegalStateException> { loader.loadPage(pages[0]) }
        loader.isLocal = false
        loader.isLocal shouldBe false
    }

    @Test
    fun baseLoaderDefaultsAreNoOps() = runTest {
        val loader = object : PageLoader() {
            override var isLocal: Boolean = false

            override suspend fun getPages() = emptyList<ReaderPage>()
        }
        val page = ReaderPage(0)
        loader.loadPage(page)
        loader.retryPage(page)
        loader.isRecycled shouldBe false
        loader.recycle()
        loader.isRecycled shouldBe true
        loader.getPages().shouldBeEmpty()
    }

    @Test
    fun priorityThenInsertionOrder() {
        val page = ReaderPage(0)
        val adjacent = PriorityPage(page, PriorityPage.ADJACENT)
        val first = PriorityPage(page, PriorityPage.DEFAULT)
        val second = PriorityPage(page, PriorityPage.DEFAULT)
        val retry = PriorityPage(page, PriorityPage.RETRY)
        listOf(adjacent, second, first, retry).sorted() shouldBe listOf(retry, first, second, adjacent)
        first.page shouldBe page
        retry.priority shouldBe PriorityPage.RETRY
    }
}
