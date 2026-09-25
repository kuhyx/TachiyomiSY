package eu.kanade.tachiyomi.ui.reader.loader

import android.app.Application
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.reader.stubImageSniffing
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.ArchiveReaderMode
import eu.kanade.tachiyomi.ui.reader.setting.archiveReaderMode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.core.common.archive.ArchiveEntry
import mihon.core.common.archive.ArchiveInputStream
import mihon.core.common.archive.ArchiveReader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File

internal class ArchivePageLoaderTest {

    @TempDir
    lateinit var cacheDir: File

    private val prefs = ReaderPreferences(MapPreferenceStore())

    private val entries = mapOf(
        "dir/10.jpg" to byteArrayOf(10),
        "dir/2.jpg" to byteArrayOf(2),
        "notes.txt" to "text".toByteArray(),
    )

    @BeforeEach
    fun setUp() {
        stubImageSniffing()
        val app = mockk<Application>()
        every { app.externalCacheDir } answers { cacheDir }
        startKoin {
            modules(
                module {
                    single { app }
                    single { prefs }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun reader(wrongPassword: Boolean? = null, missing: String? = null): ArchiveReader {
        val listing = listOf(
            ArchiveEntry("dir", isFile = false, isEncrypted = false),
            ArchiveEntry("notes.txt", isFile = true, isEncrypted = false),
            ArchiveEntry("dir/10.jpg", isFile = true, isEncrypted = false),
            ArchiveEntry("dir/2.jpg", isFile = true, isEncrypted = false),
        )
        val reader = mockk<ArchiveReader>(relaxUnitFun = true)
        every { reader.archiveHashCode } returns 42
        every { reader.wrongPassword } returns wrongPassword
        every { reader.encrypted } returns false
        every { reader.getInputStream(any()) } answers {
            firstArg<String>().takeIf { it != missing }?.let { entries[it]?.inputStream() }
        }
        every { reader["openStream"](any<Boolean>()) } answers {
            val stream = mockk<ArchiveInputStream>(relaxUnitFun = true)
            every { stream.getNextEntry() } returnsMany listing + null
            stream
        }
        return reader
    }

    @Test
    fun wrongPasswordFails() {
        shouldThrow<IllegalStateException> { ArchivePageLoader(reader(wrongPassword = true)) }
            .message shouldBe "Incorrect archive password"
    }

    @Test
    fun loadsFromFileByDefault() = runTest {
        val archive = reader(wrongPassword = false)
        val loader = ArchivePageLoader(archive)
        loader.isLocal shouldBe true
        val pages = loader.getPages()
        pages.map { it.stream!!().read() } shouldBe listOf(2, 10)
        File(cacheDir, "reader_42").exists() shouldBe false
        loader.loadPage(pages[0])
        loader.recycle()
        verify { archive.close() }
        shouldThrow<IllegalStateException> { loader.loadPage(pages[0]) }
        loader.isLocal = false
        loader.isLocal shouldBe false
    }

    @Test
    fun loadsIntoMemory() = runTest {
        prefs.archiveReaderMode.set(ArchiveReaderMode.LOAD_INTO_MEMORY)
        val archive = reader()
        val pages = ArchivePageLoader(archive).getPages()
        pages[1].stream!!().read() shouldBe 10
        pages[1].stream!!().read() shouldBe 10
        pages[0].stream!!().read() shouldBe 2
        verify(exactly = 1) { archive.getInputStream("dir/10.jpg") }
    }

    @Test
    fun cachesToDisk() = runTest {
        prefs.archiveReaderMode.set(ArchiveReaderMode.CACHE_TO_DISK)
        val loader = ArchivePageLoader(reader(missing = "dir/10.jpg"))
        val tmp = File(cacheDir, "reader_42")
        tmp.list()!!.sorted() shouldBe listOf("10.jpg", "2.jpg")
        File(tmp, "10.jpg").length() shouldBe 0L
        val pages = loader.getPages()
        pages.size shouldBe 2
        pages[0].stream!!().read() shouldBe 2
        pages[1].stream!!().read() shouldBe -1
        loader.recycle()
        tmp.exists() shouldBe false
    }
}
