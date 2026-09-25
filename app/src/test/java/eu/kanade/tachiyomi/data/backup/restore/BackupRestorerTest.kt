package eu.kanade.tachiyomi.data.backup.restore

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.backupExtensionStore
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class BackupRestorerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val harness by lazy { BackupRestorerHarness(folder.root) }
    private val full = Backup(
        backupManga = listOf(BackupManga(source = 7L, url = "/a", title = "A")),
        backupCategories = listOf(BackupCategory(name = "C", order = 1)),
        backupSources = listOf(BackupSource(name = "Seven", sourceId = 7L)),
        backupExtensionStores = listOf(backupExtensionStore()),
    )

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun everySectionIsRestored() = runTest {
        val restorer = harness.restorer()
        restorer.restore(harness.write(full), RestoreOptions())
        coVerify { harness.categoriesRestorer(match { it.single().name == "C" }) }
        coVerify { harness.savedSearchRestorer.restoreSavedSearches(emptyList()) }
        coVerify { harness.preferenceRestorer.restoreApp(emptyList(), match { it.single().name == "C" }) }
        coVerify { harness.preferenceRestorer.restoreSource(emptyList()) }
        coVerify { harness.mangaRestorer.restore(match { it.url == "/a" }, match { it.single().name == "C" }) }
        coVerify { harness.extensionStoreRestorer(full.backupExtensionStores.single()) }
        verify { harness.graph.downloadCache.invalidateCache() }
        restorer.restoreAmount shouldBe 6
        restorer.sourceMapping shouldBe mapOf(7L to "Seven")
        verify { harness.notifier.showRestoreComplete(any(), 0, null, "", false) }
    }

    @Test
    fun disabledSectionsAreSkipped() = runTest {
        val off = RestoreOptions(
            libraryEntries = false,
            categories = false,
            appSettings = false,
            extensionStores = false,
            sourceSettings = false,
            savedSearches = false,
        )
        val restorer = harness.restorer(isSync = true)
        restorer.restore(harness.write(full), off)
        coVerify(exactly = 0) { harness.categoriesRestorer(any()) }
        coVerify(exactly = 0) { harness.mangaRestorer.restore(any(), any()) }
        verify(exactly = 0) { harness.graph.downloadCache.invalidateCache() }
        restorer.restoreAmount shouldBe 0
        verify { harness.notifier.showRestoreComplete(any(), 0, null, "", true) }
    }

    @Test
    fun uncategorisedMangaStillRestores() = runTest {
        harness.restorer().restore(harness.write(full), RestoreOptions(categories = false))
        coVerify { harness.mangaRestorer.restore(match { it.url == "/a" }, emptyList()) }
        coVerify { harness.preferenceRestorer.restoreApp(emptyList(), null) }
    }

    @Test
    fun cacheFailureIsLoggedOnly() = runTest {
        every { harness.graph.downloadCache.invalidateCache() } throws IllegalStateException("busy")
        harness.restorer().restore(harness.write(full), RestoreOptions())
        verify { harness.notifier.showRestoreComplete(any(), 0, null, "", false) }
    }

    @Test
    fun errorsAreWrittenToALog() = runTest {
        coEvery { harness.mangaRestorer.restore(any(), any()) } throws IllegalStateException("bad row")
        val path = slot<String>()
        val name = slot<String>()
        harness.restorer().restore(harness.write(full), RestoreOptions())
        verify { harness.notifier.showRestoreComplete(any(), 1, capture(path), capture(name), false) }
        name.captured shouldBe "mihon_restore_error.txt"
        File(path.captured, name.captured).readText() shouldContain "A [Seven]: bad row"
    }

    @Test
    fun unwritableLogIsSkipped() = runTest {
        coEvery { harness.mangaRestorer.restore(any(), any()) } throws IllegalStateException("bad row")
        val notADirectory = folder.newFile("cache")
        harness.restorer(CacheContext(harness.app, notADirectory)).restore(harness.write(full), RestoreOptions())
        verify { harness.notifier.showRestoreComplete(any(), 1, null, "", false) }
    }

    @Test
    fun stepsFollowTheOptions() {
        val backup = full.copy(backupExtensionStores = List(3) { backupExtensionStore() })
        RestoreOptions().stepCount(backup) shouldBe 1 + 1 + 1 + 1 + 3 + 1
        RestoreOptions(libraryEntries = false, extensionStores = false).stepCount(backup) shouldBe 4
    }

    @Test
    fun defaultsComeFromInjekt() {
        val restorer = BackupRestorer(harness.app, harness.notifier, isSync = true)
        restorer.database shouldBe harness.graph.database
        restorer.mangaRestorer.isSync shouldBe true
        restorer.errors.size shouldBe 0
    }
}
