package exh.favorites

import android.content.Context
import android.content.ContextWrapper
import android.net.wifi.WifiManager
import android.os.Looper
import android.os.PowerManager
import androidx.work.Operation
import eu.kanade.tachiyomi.source.online.all.fetchFavorites
import exh.eh.EHentaiUpdateWorker
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.domain.category.interactor.UpdateCategory

@RunWith(RobolectricTestRunner::class)
internal class FavoritesSyncHelperTest {
    private val harness = FavoritesSyncHarness()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Before
    fun setUp() {
        harness.start()
        harness.exhPreferences.enableExhentai.set(true)
    }

    @After
    fun tearDown() {
        scope.cancel()
        harness.stop()
    }

    private fun sync(helper: FavoritesSyncHelper): FavoritesSyncStatus {
        helper.runSync(scope)
        // Idle the main looper while waiting so the UI-thread steps of the sync can run.
        val deadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            val status = helper.status.value
            if (status !is FavoritesSyncStatus.Initializing && status !is FavoritesSyncStatus.Processing) return status
            Thread.sleep(20)
        }
        error("sync did not finish: ${helper.status.value}")
    }

    @Test
    fun requiresLogin() {
        harness.exhPreferences.enableExhentai.set(false)
        sync(harness.helper()) shouldBe FavoritesSyncStatus.SyncError.NotLoggedInSyncError
    }

    @Test
    fun refusesMultiCategoryGalleries() {
        val manga = favManga(1, "1")
        coEvery { harness.getLibraryManga.await() } returns
            listOf(libraryEntry(favManga(2, "2", source = 5L)), libraryEntry(manga), libraryEntry(manga))
        coEvery { harness.getCategories.await(1L) } returns listOf(category(1, "A", 0), category(2, "B", 1))
        val status = sync(harness.helper())
        status shouldBe FavoritesSyncStatus.BadLibraryState.MangaInMultipleCategories(1, "m1", listOf("A", "B"))
        coVerify(exactly = 0) { harness.exh.fetchFavorites() }
    }

    @Test
    fun reportsFailedDownloads() {
        coEvery { harness.exh.fetchFavorites() } throws IllegalStateException("down")
        sync(harness.helper()) shouldBe FavoritesSyncStatus.SyncError.FailedToFetchFavorites
    }

    @Test
    fun fullSyncEndsIdle() {
        val local = favManga(1, "1")
        coEvery { harness.getLibraryManga.await() } returns listOf(libraryEntry(local))
        coEvery { harness.getFavorites.await() } returns listOf(local)
        coEvery { harness.getCategories.await() } returns
            listOf(category(0, "Default", 0), category(1, "Cat A", 0), category(2, "Cat B", 1))
        coEvery { harness.getCategories.await(1L) } returns listOf(category(1, "Cat A", 0))
        coEvery { harness.exh.fetchFavorites() } returns
            (listOf(parsedManga("2", fav = 1)) to listOf("Cat A", "Renamed"))
        coEvery { harness.updateCategory.await(any()) } returns UpdateCategory.Result.Success
        coEvery { harness.getManga.await(any<String>(), any()) } returns favManga(2, "2")
        val helper = harness.helper()
        sync(helper) shouldBe FavoritesSyncStatus.Idle
        harness.requests.map { it.url.encodedPath } shouldContainExactly listOf("/gallerypopups.php")
        coVerify(exactly = 1) { harness.setMangaCategories.await(2, listOf(2L)) }
        coVerify(exactly = 1) { harness.deleteFavoriteEntries.await() }
        verify { harness.workManager.cancelAllWorkByTag(EHentaiUpdateWorker.TAG) }
        verify { harness.workManager.enqueueUniquePeriodicWork(EHentaiUpdateWorker.TAG, any(), any()) }
        helper.needWarnThrottle().shouldBeFalse()
    }

    @Test
    fun readOnlySyncSkipsRemoteChanges() {
        harness.exhPreferences.exhReadOnlySync.set(true)
        harness.exhPreferences.exhLenientSync.set(true)
        val local = favManga(1, "1")
        coEvery { harness.getFavorites.await() } returns listOf(local)
        coEvery { harness.getCategories.await() } returns listOf(category(1, "A", 0))
        coEvery { harness.getCategories.await(1L) } returns listOf(category(1, "A", 0))
        coEvery { harness.exh.fetchFavorites() } returns (listOf(parsedManga("2")) to listOf("A"))
        every { harness.exh.matchesUri(any()) } returns false
        val status = sync(harness.helper())
        val errors = status.shouldBeInstanceOf<FavoritesSyncStatus.CompleteWithErrors>().messages
        errors.single().shouldBeInstanceOf<FavoritesSyncStatus.SyncError.GallerySyncError.InvalidGalleryFail>()
        harness.requests.size shouldBe 0
    }

    @Test
    fun ignoredErrorsKeepTheirStatus() {
        coEvery { harness.exh.fetchFavorites() } returns (listOf(parsedManga("2")) to emptyList())
        every { harness.exh.matchesUri(any()) } returns false
        sync(harness.helper()).shouldBeInstanceOf<FavoritesSyncStatus.SyncError.GallerySyncError.InvalidGalleryFail>()
    }

    // Robolectric's locks throw when released without being acquired (as Android's do), which the
    // helper swallows; the second run then finds the previous run's locks still referenced.
    @Test
    fun unexpectedErrorsAreReported() {
        coEvery { harness.getCategories.await() } throws IllegalStateException("db")
        val helper = harness.helper()
        sync(helper) shouldBe FavoritesSyncStatus.SyncError.UnknownSyncError("db")
        helper.status.value = FavoritesSyncStatus.Idle
        coEvery { harness.getCategories.await() } throws IllegalStateException()
        sync(helper) shouldBe FavoritesSyncStatus.SyncError.UnknownSyncError("")
    }

    @Test
    fun acquiredLocksAreReleased() {
        val helper = harness.helper()
        // Acquire the locks once the helper holds them, so that releasing them is legal.
        every { harness.workManager.cancelAllWorkByTag(any()) } answers {
            (helper.lock("wakeLock") as PowerManager.WakeLock).acquire()
            (helper.lock("wifiLock") as WifiManager.WifiLock).acquire()
            mockk<Operation>()
        }
        sync(helper) shouldBe FavoritesSyncStatus.Idle
        helper.lock("wakeLock") shouldBe null
        helper.lock("wifiLock") shouldBe null
    }

    @Test
    fun missingLockServicesTolerated() {
        val context = object : ContextWrapper(harness.context) {
            override fun getApplicationContext(): Context = this
            override fun getSystemService(name: String): Any? = when (name) {
                Context.POWER_SERVICE, Context.WIFI_SERVICE -> null
                else -> super.getSystemService(name)
            }
        }
        val helper = FavoritesSyncHelper(context)
        sync(helper) shouldBe FavoritesSyncStatus.Idle
        helper.lock("wakeLock") shouldBe null
        helper.lock("wifiLock") shouldBe null
    }

    private fun FavoritesSyncHelper.lock(name: String): Any? =
        FavoritesSyncHelper::class.java.getDeclaredField(name).also { it.isAccessible = true }.get(this)
}
