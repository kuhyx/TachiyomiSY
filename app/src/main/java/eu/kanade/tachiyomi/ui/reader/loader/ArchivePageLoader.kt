package eu.kanade.tachiyomi.ui.reader.loader

import android.app.Application
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.ArchiveReaderMode
import eu.kanade.tachiyomi.ui.reader.setting.archiveReaderMode
import eu.kanade.tachiyomi.util.lang.compareNaturalIgnoreCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.core.common.archive.ArchiveReader
import tachiyomi.core.common.util.system.ImageUtil
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.InputStream

/**
 * Loader used to load a chapter from an archive file.
 */
internal class ArchivePageLoader(private val reader: ArchiveReader) : PageLoader() {
    // SY -->
    private val mutex = Mutex()
    private val context: Application by injectLazy()
    private val readerPreferences: ReaderPreferences by injectLazy()
    private val tmpDir = File(context.externalCacheDir, "reader_${reader.archiveHashCode}").apply {
        deleteRecursively()
    }

    init {
        reader.wrongPassword?.let { wrongPassword ->
            if (wrongPassword) {
                error("Incorrect archive password")
            }
        }
        if (readerPreferences.archiveReaderMode.get() == ArchiveReaderMode.CACHE_TO_DISK) {
            tmpDir.mkdirs()
            reader.useEntries { entries ->
                entries
                    .filter { it.isFile && ImageUtil.isImage(it.name) { reader.getInputStream(it.name)!! } }
                    .sortedWith { f1, f2 -> f1.name.compareNaturalIgnoreCase(f2.name) }
                    .forEach { entry ->
                        File(tmpDir, entry.name.substringAfterLast("/"))
                            .apply { createNewFile() }
                            .outputStream()
                            .use { output ->
                                reader.getInputStream(entry.name)?.use { input ->
                                    input.copyTo(output)
                                }
                            }
                    }
            }
        }
    }
    // SY <--

    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderPage> = reader.useEntries { entries ->
        // SY -->
        if (readerPreferences.archiveReaderMode.get() == ArchiveReaderMode.CACHE_TO_DISK) {
            return DirectoryPageLoader(UniFile.fromFile(tmpDir)!!).getPages()
        }
        // SY <--
        entries
            .filter { it.isFile && ImageUtil.isImage(it.name) { reader.getInputStream(it.name)!! } }
            .sortedWith { f1, f2 -> f1.name.compareNaturalIgnoreCase(f2.name) }
            .mapIndexed { i, entry ->
                // SY -->
                val imageBytesDeferred: Deferred<ByteArray>? =
                    if (readerPreferences.archiveReaderMode.get() == ArchiveReaderMode.LOAD_INTO_MEMORY) {
                        CoroutineScope(Dispatchers.IO).async {
                            mutex.withLock {
                                // readBytes copies through its own buffer; buffered() would only add a branch.
                                reader.getInputStream(entry.name)!!.use { stream -> stream.readBytes() }
                            }
                        }
                    } else {
                        null
                    }
                // SY <--
                ReaderPage(i).apply {
                    // SY -->
                    stream = if (imageBytesDeferred != null) {
                        memoryStream(imageBytesDeferred)
                    } else {
                        { reader.getInputStream(entry.name)!! }
                    }
                    // SY <--
                    status = Page.State.Ready
                }
            }
            .toList()
    }

    // SY --> A stream over the bytes [deferred] loads, awaited once on first use.
    private fun memoryStream(deferred: Deferred<ByteArray>): () -> InputStream {
        val imageBytes by lazy { runBlocking { deferred.await() } }
        return { imageBytes.copyOf().inputStream() }
    }
    // SY <--

    override suspend fun loadPage(page: ReaderPage) {
        check(!isRecycled)
    }

    override fun recycle() {
        super.recycle()
        reader.close()
        // SY -->
        tmpDir.deleteRecursively()
        // SY <--
    }
}
