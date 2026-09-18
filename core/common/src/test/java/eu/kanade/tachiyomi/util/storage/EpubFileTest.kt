package eu.kanade.tachiyomi.util.storage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import mihon.core.common.archive.ArchiveReader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

internal class EpubFileTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun usesSlashWhenContainerIsPosix() {
        val epub = EpubFile(epubReader(mapOf("META-INF/container.xml" to containerXml("OEBPS/content.opf"))))
        epub.getPackageHref() shouldBe "OEBPS/content.opf"
    }

    @Test
    fun usesBackslashForDosContainer() {
        val epub = EpubFile(epubReader(mapOf("META-INF\\container.xml" to containerXml("OEBPS\\content.opf"))))
        epub.getPackageHref() shouldBe "OEBPS\\content.opf"
    }

    @Test
    fun defaultsHrefWithoutContainer() {
        EpubFile(epubReader(emptyMap())).getPackageHref() shouldBe "OEBPS/content.opf"
    }

    @Test
    fun defaultsHrefWithoutRootfile() {
        val container = "<container><rootfiles></rootfiles></container>"
        val epub = EpubFile(epubReader(mapOf("META-INF/container.xml" to container)))
        epub.getPackageHref() shouldBe "OEBPS/content.opf"
    }

    @Test
    fun parsesPackageDocument() {
        val epub = EpubFile(epubReader(mapOf("book.opf" to "<package><manifest/></package>")))
        epub.getPackageDocument("book.opf").select("manifest").size shouldBe 1
    }

    @Test
    fun missingPackageDocumentThrows() {
        val epub = EpubFile(epubReader(emptyMap()))
        shouldThrow<NullPointerException> { epub.getPackageDocument("missing.opf") }
    }

    @Test
    fun streamsDelegateToReader() {
        val epub = EpubFile(epubReader(mapOf("a.txt" to "hello")))
        epub.getInputStream("a.txt")!!.readBytes() shouldBe "hello".toByteArray()
        epub.getInputStream("b.txt").shouldBeNull()
    }

    @Test
    fun closeDelegatesToReader() {
        val reader = epubReader(emptyMap())
        EpubFile(reader).close()
        verify(exactly = 1) { reader.close() }
    }
}

/** A reader serving [entries] by name from memory; `close` is a no-op. */
internal fun epubReader(entries: Map<String, String>): ArchiveReader {
    val reader = mockk<ArchiveReader>()
    every { reader.getInputStream(any()) } answers {
        entries[firstArg<String>()]?.let { ByteArrayInputStream(it.toByteArray()) }
    }
    justRun { reader.close() }
    return reader
}

internal fun containerXml(fullPath: String): String =
    """<?xml version="1.0"?><container><rootfiles><rootfile full-path="$fullPath"/></rootfiles></container>"""
