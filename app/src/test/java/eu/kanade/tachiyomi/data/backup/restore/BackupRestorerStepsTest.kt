package eu.kanade.tachiyomi.data.backup.restore

import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.backupExtensionStore
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@RunWith(RobolectricTestRunner::class)
internal class BackupRestorerStepsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val harness by lazy { BackupRestorerHarness(folder.root) }

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun settingsStepsReportProgress() = runTest {
        val restorer = harness.restorer(isSync = true)
        restorer.restoreAmount = 4
        restorer.restoreCategories(emptyList())
        restorer.restoreSavedSearches(emptyList())
        restorer.restoreAppPreferences(emptyList(), null)
        restorer.restoreSourcePreferences(emptyList())
        verify { harness.notifier.showRestoreProgress("Categories", 1, 4, true) }
        verify { harness.notifier.showRestoreProgress("Saved Searches", 2, 4, true) }
        verify { harness.notifier.showRestoreProgress("App settings", 3, 4, true) }
        verify { harness.notifier.showRestoreProgress("Source settings", 4, 4, true) }
    }

    @Test
    fun mergedEntriesRestoreLast() = runTest {
        val merged = BackupManga(source = MERGED_SOURCE_ID, url = "/merged", title = "Merged")
        val plain = BackupManga(source = 1L, url = "/plain", title = "Plain")
        harness.restorer().restoreManga(listOf(merged, plain), emptyList())
        coVerifyOrder {
            harness.mangaRestorer.restore(plain, emptyList())
            harness.mangaRestorer.restore(merged, emptyList())
        }
        verify { harness.notifier.showRestoreProgress("Merged", 2, 0, false) }
    }

    @Test
    fun mangaRestoreRunsInBatches() = runTest {
        val library = List(RESTORE_BATCH_SIZE + 1) { BackupManga(source = 1L, url = "/$it", title = "T$it") }
        val restorer = harness.restorer()
        restorer.restoreManga(library, emptyList())
        restorer.restoreProgress.load() shouldBe library.size
        verify { harness.notifier.showRestoreProgress("T${RESTORE_BATCH_SIZE - 1}", RESTORE_BATCH_SIZE, 0, false) }
        verify { harness.notifier.showRestoreProgress("T$RESTORE_BATCH_SIZE", library.size, 0, false) }
    }

    @Test
    fun mangaErrorsNameTheSource() = runTest {
        val known = BackupManga(source = 7L, url = "/k", title = "Known")
        val unknown = BackupManga(source = 8L, url = "/u", title = "Unknown")
        coEvery { harness.mangaRestorer.restore(any(), any()) } throws IllegalStateException("boom")
        val restorer = harness.restorer()
        restorer.sourceMapping = mapOf(7L to "Seven")
        restorer.restoreManga(listOf(known, unknown), emptyList())
        restorer.errors.map { it.second } shouldBe listOf("Known [Seven]: boom", "Unknown [8]: boom")
    }

    @Test
    fun storeErrorsAreCollected() = runTest {
        val good = backupExtensionStore().copy(name = "Good")
        val bad = backupExtensionStore().copy(name = "Bad")
        coEvery { harness.extensionStoreRestorer(bad) } throws IllegalStateException("offline")
        val restorer = harness.restorer()
        restorer.restoreExtensionStores(listOf(good, bad))
        coVerify { harness.extensionStoreRestorer(good) }
        restorer.errors.single().second shouldBe "Error Adding Repo: Bad : offline"
        verify { harness.notifier.showRestoreProgress("Extension stores", 2, 0, false) }
    }
}
