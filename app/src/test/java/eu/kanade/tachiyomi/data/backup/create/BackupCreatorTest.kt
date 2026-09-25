package eu.kanade.tachiyomi.data.backup.create

import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.backup.BackupDecoder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class BackupCreatorTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val harness = BackupCreatorHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() {
        unmockkAll()
        harness.stop()
    }

    @Test
    fun manualBackupWritesTheFile() = runTest {
        val file = folder.newFile("manual.tachibk")
        harness.creator(isAutoBackup = false).backup(Uri.fromFile(file), BackupOptions()) shouldBe
            Uri.fromFile(file).toString()
        BackupDecoder(harness.context).decode(Uri.fromFile(file)).backupManga.single().url shouldBe "/fav"
        coVerify { harness.mangaCreator(listOf(harness.favorite, harness.read, harness.merged), BackupOptions()) }
        harness.backupPreferences.lastAutoBackupTimestamp.get() shouldBe 0L
    }

    @Test
    fun unreadEntriesCanBeLeftOut() = runTest {
        val options = BackupOptions(readEntries = false)
        harness.creator(isAutoBackup = false).backup(Uri.fromFile(folder.newFile()), options)
        coVerify { harness.mangaCreator(listOf(harness.favorite, harness.merged), options) }
        coVerify(exactly = 0) { harness.mangaRepository.getReadMangaNotInLibrary() }
    }

    @Test
    fun autoBackupKeepsTheNewestFew() = runTest {
        val dir = folder.newFolder("auto")
        val old = (1..5).map { File(dir, "${BuildConfig.APPLICATION_ID}_2020-01-0${it}_10-00.tachibk") }
        old.forEach { it.writeText("old") }
        val unrelated = File(dir, "notes.txt").apply { writeText("keep") }
        val written = harness.creator(isAutoBackup = true).backup(Uri.fromFile(dir), BackupOptions())
        old.map { it.exists() } shouldBe listOf(false, false, true, true, true)
        unrelated.exists() shouldBe true
        File(checkNotNull(Uri.parse(written).path)).name shouldMatch
            Regex("""${BuildConfig.APPLICATION_ID}_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}\.tachibk""")
        harness.backupPreferences.lastAutoBackupTimestamp.get() shouldBeGreaterThan 0L
    }

    @Test
    fun unusableTargetsFail() = runTest {
        val message = "Couldn't create a backup file"
        val web = Uri.parse("https://example.org/backup")
        shouldThrow<IllegalStateException> { harness.creator(false).backup(web, BackupOptions()) }.message shouldBe
            message
        shouldThrow<IllegalStateException> { harness.creator(true).backup(web, BackupOptions()) }.message shouldBe
            message
        val directory = Uri.fromFile(folder.newFolder("dir"))
        shouldThrow<IllegalStateException> { harness.creator(false).backup(directory, BackupOptions()) }
        val missing = Uri.fromFile(File(folder.root, "missing/dir"))
        shouldThrow<IllegalStateException> { harness.creator(true).backup(missing, BackupOptions()) }
    }

    @Test
    fun emptyBackupIsRemoved() = runTest {
        harness.library = emptyList()
        val file = folder.newFile("empty.tachibk")
        shouldThrow<IllegalStateException> {
            harness.creator(isAutoBackup = false).backup(Uri.fromFile(file), BackupOptions())
        }.message shouldBe "No library entries to back up"
        file.exists() shouldBe false
    }

    @Test
    fun nonFileStreamsAreNotTruncated() = runTest {
        val target = folder.newFile("real.tachibk")
        val stream = ByteArrayOutputStream()
        val uniFile = mockk<UniFile>(relaxed = true) {
            every { isFile } returns true
            every { openOutputStream() } returns stream
            every { uri } answers {
                target.writeBytes(stream.toByteArray())
                Uri.fromFile(target)
            }
        }
        mockkStatic(UniFile::class)
        every { UniFile.fromUri(any(), any()) } returns uniFile
        harness.creator(isAutoBackup = false).backup(Uri.parse("content://docs/backup"), BackupOptions())
        (stream.size() > 0) shouldBe true
        verify(exactly = 0) { uniFile.delete() }
    }

    @Test
    fun sectionsFollowTheOptions() = runTest {
        val file = folder.newFile("sections.tachibk")
        val off = BackupOptions(
            categories = false,
            appSettings = false,
            extensionStores = false,
            sourceSettings = false,
            savedSearches = false,
        )
        harness.creator(isAutoBackup = false).backup(Uri.fromFile(file), off)
        verify(exactly = 0) { harness.preferenceCreator.createApp(any()) }
        verify(exactly = 0) { harness.preferenceCreator.createSource(any()) }
        coVerify(exactly = 0) { harness.categoriesCreator() }
    }

    @Test
    fun filenamesCarryTheDate() {
        BackupCreator.getFilename() shouldMatch
            Regex("""${BuildConfig.APPLICATION_ID}_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}\.tachibk""")
    }
}
