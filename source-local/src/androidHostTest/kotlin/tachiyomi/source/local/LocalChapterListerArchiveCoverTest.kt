package tachiyomi.source.local

import android.content.Context
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import mihon.core.common.archive.ArchiveEntry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.source.local.image.LocalCoverManager
import java.io.File
import java.io.InputStream

/** The cover a [LocalChapterLister] takes from a chapter archive when the manga has none. */
@RunWith(RobolectricTestRunner::class)
internal class LocalChapterListerArchiveCoverTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val covers = mutableListOf<Pair<Int, Boolean>>()
    private val coverManager = mockk<LocalCoverManager> {
        every { update(any(), any(), any()) } answers {
            covers += secondArg<InputStream>().readBytes().size to thirdArg<Boolean>()
            null
        }
    }

    @Before
    fun setUp() {
        RecordingLogger.start()
        File(folder.newFolder("m"), "c1.cbz").createNewFile()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun listArchive(entries: Map<String, ByteArray>, listing: List<ArchiveEntry>, encrypted: Boolean = false) {
        stubArchiveReaders(fakeArchiveReader(entries, encrypted = encrypted, listing = listing))
        val fileSystem = fileSystemOver(folder.root)
        val lister = LocalChapterLister(
            context = context,
            fileSystem = fileSystem,
            coverManager = coverManager,
            comicInfoFiles = ComicInfoFiles(context, testXml),
            formats = LocalChapterFormats(context, fileSystem),
        )
        runTest { lister.list(sampleManga("m")).size shouldBe 1 }
    }

    private fun file(name: String): ArchiveEntry = ArchiveEntry(name = name, isFile = true, isEncrypted = false)

    private fun dir(name: String): ArchiveEntry = ArchiveEntry(name = name, isFile = false, isEncrypted = false)

    @Test
    fun archiveCoverIsTheFirstImage() {
        listArchive(
            entries = mapOf("b.png" to PNG_HEADER, "a.bin" to ByteArray(0)),
            listing = listOf(file("b.png"), dir("0dir"), file("a.bin")),
            encrypted = true,
        )
        covers shouldBe listOf(PNG_HEADER.size to true)
        RecordingLogger.messages shouldBe emptyList()
    }

    @Test
    fun archiveWithoutImagesHasNoCover() {
        listArchive(entries = mapOf("a.bin" to ByteArray(0)), listing = listOf(file("a.bin")))
        covers shouldBe emptyList()
        RecordingLogger.messages shouldBe emptyList()
    }

    @Test
    fun sniffedEntryNeedsAStream() {
        listArchive(entries = emptyMap(), listing = listOf(file("a.bin")))
        covers shouldBe emptyList()
        RecordingLogger.messages.single() shouldContain "Error updating cover for m"
    }

    @Test
    fun imageEntryNeedsAStream() {
        listArchive(entries = emptyMap(), listing = listOf(file("b.png")))
        covers shouldBe emptyList()
        RecordingLogger.messages.single() shouldContain "Error updating cover for m"
    }
}
