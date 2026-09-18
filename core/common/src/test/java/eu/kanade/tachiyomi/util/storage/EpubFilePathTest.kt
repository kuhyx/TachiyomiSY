package eu.kanade.tachiyomi.util.storage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class EpubFilePathTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun collectsImagesInSpineOrder() {
        val entries = mapOf(
            "META-INF/container.xml" to containerXml("OEBPS/content.opf"),
            "OEBPS/content.opf" to PACKAGE,
            "OEBPS/text/page1.xhtml" to PAGE_ONE,
            "/text/page2.xhtml" to PAGE_TWO,
        )
        EpubFile(epubReader(entries)).getImagesFromPages() shouldContainExactly listOf(
            "OEBPS/images/a.png",
            "OEBPS/text/b.jpg",
            "text/c.png",
        )
    }

    @Test
    fun resolvesAgainstRootPackage() {
        val entries = mapOf(
            "META-INF/container.xml" to containerXml("content.opf"),
            "content.opf" to ROOT_PACKAGE,
            "page.xhtml" to "<html><body><img src=\"pic.png\"/></body></html>",
        )
        EpubFile(epubReader(entries)).getImagesFromPages() shouldContainExactly listOf("pic.png")
    }

    @Test
    fun handlesDosSeparators() {
        val entries = mapOf(
            "META-INF\\container.xml" to containerXml("OEBPS\\content.opf"),
            "OEBPS\\content.opf" to ROOT_PACKAGE,
            "OEBPS\\page.xhtml" to """<html><body><img src="img\pic.png"/></body></html>""",
        )
        EpubFile(epubReader(entries)).getImagesFromPages() shouldContainExactly listOf("OEBPS\\img\\pic.png")
    }

    @Test
    fun missingPageThrows() {
        val entries = mapOf(
            "META-INF/container.xml" to containerXml("content.opf"),
            "content.opf" to ROOT_PACKAGE,
        )
        shouldThrow<NullPointerException> { EpubFile(epubReader(entries)).getImagesFromPages() }
    }

    private companion object {
        val PACKAGE = """
            <package>
                <manifest>
                    <item id="p1" href="text/page1.xhtml" media-type="application/xhtml+xml"/>
                    <item id="p2" href="/text/page2.xhtml" media-type="application/xhtml+xml"/>
                    <item id="css" href="style.css" media-type="text/css"/>
                </manifest>
                <spine><itemref idref="p1"/><itemref idref="missing"/><itemref idref="p2"/></spine>
            </package>
        """.trimIndent()
        val ROOT_PACKAGE = """
            <package>
                <manifest><item id="p" href="page.xhtml" media-type="application/xhtml+xml"/></manifest>
                <spine><itemref idref="p"/></spine>
            </package>
        """.trimIndent()
        val PAGE_ONE = """
            <html><body>
                <img src="../images/a.png"/>
                <svg><image xlink:href="b.jpg"/></svg>
                <p>text</p>
            </body></html>
        """.trimIndent()
        const val PAGE_TWO = """<html><body><img src="c.png"/></body></html>"""
    }
}
