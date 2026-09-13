package mihon.gradle.transforms

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Prefix of the entries a multi-release jar adds for newer JDKs; Android's runtime never loads them. */
internal const val MULTI_RELEASE_PREFIX: String = "META-INF/versions/"

/** Whether [jar] carries any entry under [MULTI_RELEASE_PREFIX]. */
internal fun hasMultiReleaseEntries(jar: File): Boolean = ZipFile(jar).use { zip ->
    zip.entries().asSequence().any { it.name.startsWith(MULTI_RELEASE_PREFIX) }
}

/** Writes [source] to [target] entry by entry, dropping everything under [MULTI_RELEASE_PREFIX]. */
internal fun copyWithoutMultiRelease(source: File, target: File) {
    ZipFile(source).use { zip -> zip.copyEntriesTo(target) }
}

private fun ZipFile.copyEntriesTo(target: File) {
    ZipOutputStream(target.outputStream()).use { out ->
        entries().asSequence()
            .filterNot { it.name.startsWith(MULTI_RELEASE_PREFIX) }
            .forEach { entry -> out.copyEntry(this, entry) }
    }
}

private fun ZipOutputStream.copyEntry(zip: ZipFile, entry: ZipEntry) {
    putNextEntry(ZipEntry(entry.name).also { it.time = entry.time })
    if (!entry.isDirectory) {
        zip.getInputStream(entry).use { it.copyTo(this) }
    }
    closeEntry()
}
