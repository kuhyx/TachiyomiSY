package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.Source
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.storage.service.StorageManager
import java.io.File

/**
 * A [DownloadProvider] over a real directory: [root] stands in for the storage manager's downloads
 * tree, so `createDirectory`/`findFile` hit the filesystem instead of a stub.
 */
internal class DownloadProviderHarness(root: File?, context: Context = mockk(relaxed = true)) {
    val store: MapPreferenceStore = MapPreferenceStore()
    val libraryPreferences: LibraryPreferences = LibraryPreferences(store)
    val downloadPreferences: DownloadPreferences = DownloadPreferences(store)
    val storageManager: StorageManager = mockk<StorageManager>().also {
        every { it.getDownloadsDirectory() } returns root?.let(UniFile::fromFile)
    }
    val provider: DownloadProvider = DownloadProvider(
        context = context,
        storageManager = storageManager,
        libraryPreferences = libraryPreferences,
        downloadPreferences = downloadPreferences,
    )
}

/** The temp downloads tree and the harness over it, shared by the provider's test classes. */
internal abstract class ProviderTestBase {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    protected val context: Context = mockk<Context>(relaxed = true).also {
        every { it.getString(any()) } returns "failed"
        every { it.getString(any(), *anyVararg()) } returns "failed"
    }

    protected lateinit var root: File
    protected lateinit var harness: DownloadProviderHarness
    protected val source: Source = namedSource("Source")

    @Before
    fun setUpProvider() {
        root = tmp.newFolder("downloads")
        harness = DownloadProviderHarness(root = root, context = context)
        harness.downloadPreferences.includeChapterUrlHash.set(false)
    }

    protected fun chapterDir(mangaTitle: String, name: String): File =
        File(root, "Source/$mangaTitle/$name").apply { mkdirs() }
}

/** A source whose `toString()` is the directory name the provider derives. */
internal fun namedSource(name: String, id: Long = 7L): Source = mockk<Source>().also {
    every { it.toString() } returns name
    every { it.id } returns id
}

internal fun testChapter(name: String, scanlator: String? = null, url: String = "/c"): Chapter =
    Chapter.create().copy(name = name, scanlator = scanlator, url = url)

internal fun testManga(title: String, id: Long = 1L): Manga = Manga.create().copy(id = id, ogTitle = title)
