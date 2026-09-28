package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import mihon.core.common.archive.ZipWriter

private const val LIB_ARCHIVE = "mihon.core.common.archive.LibArchive"

private val unitCallsTakingOneId = listOf(
    "writeSetFormatZip",
    "writeZipSetCompressionStore",
    "entryFree",
    "writeFree",
)
private val unitCallsTakingIdAndBytes = listOf("setCharset", "writeSetOptions", "writeSetPassphrase")

/**
 * Stands in for the libarchive JNI binding `ZipWriter` is built on, which a JVM test cannot load: every write
 * succeeds. The binding is internal to core-common, so it is reached by name.
 * Undone by `unmockkAll()`.
 */
internal fun stubLibArchive() {
    val lib: Any = requireNotNull(Class.forName(LIB_ARCHIVE).getField("INSTANCE").get(null))
    mockkObject(lib)
    every { lib["writeNew"]() } returns 1L
    every { lib["entryNew2"](any<Long>()) } returns 2L
    unitCallsTakingOneId.forEach { name -> every { lib[name](any<Long>()) } returns Unit }
    unitCallsTakingIdAndBytes.forEach { name ->
        every { lib[name](any<Long>(), any<ByteArray>()) } returns Unit
    }
    every { lib["writeOpenFd"](any<Long>(), any<Int>()) } returns Unit
    // Adding a file stats it into a libarchive struct this classpath cannot name, so that call is stubbed whole.
    mockkConstructor(ZipWriter::class)
    every { anyConstructed<ZipWriter>().write(any<UniFile>()) } returns Unit
}
