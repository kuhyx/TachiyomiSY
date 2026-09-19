package tachiyomi.source.local

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.serialization.json.Json
import mihon.core.common.archive.ArchiveEntry
import mihon.core.common.archive.ArchiveInputStream
import mihon.core.common.archive.ArchiveReader
import mihon.core.common.archive.archiveReader
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.core.metadata.comicinfo.getComicInfo
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.source.local.io.LocalSourceFileSystem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope
import java.io.File
import java.io.InputStream
import java.lang.reflect.Type

/** The PNG signature; enough content for a file that must count as an image. */
internal val PNG_HEADER: ByteArray = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

/** The JSON instance the app registers in Injekt. */
internal val testJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** The XML instance the app registers in Injekt, so fixtures round-trip like production files. */
internal val testXml: XML = XML.v1 {
    policy {
        ignoreUnknownChildren()
        autoPolymorphic = true
    }
    xmlDeclMode = XmlDeclMode.Charset
    xmlVersion = XmlVersion.XML10
    setIndent(2)
}

/** This file as a [UniFile]. */
internal fun File.uni(): UniFile = requireNotNull(UniFile.fromFile(this))

/** A file system whose local-source directory is [dir], or unset when [dir] is null. */
internal fun fileSystemOver(dir: File?): LocalSourceFileSystem {
    val storage = mockk<StorageManager> { every { getLocalSourceDirectory() } returns dir?.uni() }
    return LocalSourceFileSystem(storage)
}

/** A manga stored in the folder called [mangaUrl]. */
internal fun sampleManga(mangaUrl: String, mangaTitle: String = mangaUrl): SManga = SManga.create().apply {
    url = mangaUrl
    title = mangaTitle
}

/** A chapter stored at [chapterUrl] (`<manga folder>/<chapter file>`). */
internal fun sampleChapter(chapterUrl: String, chapterName: String = chapterUrl): SChapter = SChapter.create().apply {
    url = chapterUrl
    name = chapterName
}

/** A `ComicInfo` with no elements at all. */
internal fun emptyComicInfo(): ComicInfo = SManga.create().getComicInfo().copy(series = null, publishingStatus = null)

/** [info] serialised the way the app writes `ComicInfo.xml`. */
internal fun comicInfoXml(info: ComicInfo): ByteArray =
    testXml.encodeToString(ComicInfo.serializer(), info).toByteArray()

/**
 * A reader over an in-memory archive: [entries] maps entry names to contents and [listing] is
 * what `useEntries` walks, every file of [entries] by default. Streams are fresh on every call.
 */
internal fun fakeArchiveReader(
    entries: Map<String, ByteArray>,
    encrypted: Boolean = false,
    listing: List<ArchiveEntry> = entries.keys.map { fileEntry(it) },
): ArchiveReader {
    val stream = mockk<ArchiveInputStream>(relaxUnitFun = true)
    every { stream.getNextEntry() } returnsMany listing + null
    val reader = mockk<ArchiveReader>(relaxUnitFun = true)
    every { reader.encrypted } returns encrypted
    every { reader.getInputStream(any()) } answers { entries[firstArg<String>()]?.inputStream() }
    every { reader["openStream"](any<Boolean>()) } returns stream
    return reader
}

/** An archive entry that is a plain, unencrypted file. */
internal fun fileEntry(name: String): ArchiveEntry = ArchiveEntry(name = name, isFile = true, isEncrypted = false)

/** Makes every `UniFile.archiveReader(context)` call of the current test return [reader]. */
internal fun stubArchiveReaders(reader: ArchiveReader) {
    mockkStatic("mihon.core.common.archive.ArchiveReaderKt")
    every { any<UniFile>().archiveReader(any()) } returns reader
}

/** Serves [json] and [xml] through the global Injekt scope and returns the scope to put back. */
internal fun installInjekt(json: Json = testJson, xml: XML = testXml): InjektScope {
    val registrar = mockk<InjektRegistrar> {
        every { getInstance<Any>(any<Type>()) } answers { serve(firstArg(), json, xml) }
    }
    val previous = Injekt
    Injekt = InjektScope(registrar)
    return previous
}

private fun serve(type: Type, json: Json, xml: XML): Any = when (type) {
    Json::class.java -> json
    XML::class.java -> xml
    else -> error("Injekt type not served by the local-source tests: $type")
}

/** An empty stream that remembers whether it was closed. */
internal class ClosableProbe : InputStream() {
    /** True once [close] ran. */
    var isClosed: Boolean = false
        private set

    override fun read(): Int = -1

    override fun close() {
        isClosed = true
    }
}
