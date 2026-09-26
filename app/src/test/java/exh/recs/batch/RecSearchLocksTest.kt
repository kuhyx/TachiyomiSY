package exh.recs.batch

import android.content.Context
import android.content.ContextWrapper
import android.net.wifi.WifiManager
import android.os.PowerManager
import eu.kanade.tachiyomi.data.track.TrackerManager
import exh.recs.sources.sourceManga
import exh.recs.sources.track
import exh.util.createPartialWakeLock
import exh.util.createWifiLock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The wake/wifi locks, the paths that leave the search early, and the library filters that keep a result. */
@RunWith(RobolectricTestRunner::class)
internal class RecSearchLocksTest {
    private val f = RecSearchFixture()

    @After
    fun tearDown() = f.uninstall()

    private fun lockless(): Context = object : ContextWrapper(f.harness.application) {
        override fun getApplicationContext(): Context = this
        override fun getSystemService(name: String): Any? = when (name) {
            Context.POWER_SERVICE, Context.WIFI_SERVICE -> null
            else -> super.getSystemService(name)
        }
    }

    @Test
    fun staleLocksFreedFirst() {
        f.install()
        val wake = f.harness.application.createPartialWakeLock("stale").apply { acquire() }
        val wifi = f.harness.application.createWifiLock("stale").apply { acquire() }
        f.helper.plant("wakeLock", wake)
        f.helper.plant("wifiLock", wifi)
        f.runToCompletion(sourceManga()) shouldBe SearchStatus.Finished.WithoutResults
        wake.isHeld shouldBe false
        wifi.isHeld shouldBe false
    }

    @Test
    fun errorWithoutMessage() {
        f.install()
        every { f.sourceManager.getOrStub(any()) } throws IllegalStateException()
        f.runToCompletion(sourceManga()) shouldBe SearchStatus.Error("")
    }

    // Cancels the search while it looks up the source, so it leaves through the CancellationException catch.
    private fun cancelMidSearch(acquire: Boolean) {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { f.sourceManager.getOrStub(any()) } answers {
            // The helper creates its locks but never acquires them; releasing an unheld one throws.
            if (acquire) {
                (f.helper.lock("wakeLock") as PowerManager.WakeLock).acquire()
                (f.helper.lock("wifiLock") as WifiManager.WifiLock).acquire()
            }
            entered.countDown()
            release.await(WAIT_MS, TimeUnit.MILLISECONDS)
            f.comickSource
        }
        val job = checkNotNull(f.start(sourceManga()))
        entered.await(WAIT_MS, TimeUnit.MILLISECONDS) shouldBe true
        job.cancel()
        release.countDown()
        runBlocking { withTimeout(WAIT_MS) { job.join() } }
        f.helper.status.value.shouldBeInstanceOf<SearchStatus.Processing>()
    }

    @Test
    fun cancelledMidSearch() {
        f.install()
        cancelMidSearch(acquire = true)
        f.helper.lock("wakeLock") shouldBe null
    }

    @Test
    fun cancelledWithoutLocks() {
        f.install(lockless())
        cancelMidSearch(acquire = false)
    }

    @Test
    fun noFlagsQueriesNothing() {
        f.install()
        f.preferences.recommendationSearchFlags.set(0)
        // Comick joins the sources, and is dropped with the trackers.
        every { f.comickSource.name } returns "Comick"
        f.runToCompletion(sourceManga()) shouldBe SearchStatus.Finished.WithoutResults
        f.server.requests.isEmpty() shouldBe true
    }

    @Test
    fun untrackedResultsAreKept() {
        f.install()
        f.preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_TRACKERS or SearchFlags.HIDE_LIBRARY_RESULTS)
        f.server.body = MAL_RECS
        // Another tracker's entry, then this tracker's entry for a different title: neither hides the result.
        f.tracks = listOf(
            track(trackerId = TrackerManager.ANILIST, remoteUrl = "https://mal/1"),
            track(trackerId = TrackerManager.MYANIMELIST, remoteUrl = "https://mal/2"),
        )
        val status = f.runToCompletion(sourceManga()) as SearchStatus.Finished.WithResults
        status.results.single { it.recSourceName == "MyAnimeList" }.results.size shouldBe 1
    }

    @Test
    fun unownedSourceResultsKept() {
        f.install()
        f.preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_SOURCES or SearchFlags.HIDE_LIBRARY_RESULTS)
        f.server.body = """{"comic":{"recommendations":[""" +
            """{"relates":{"title":"Rec","hid":"h","md_covers":[{"b2key":"c"}]}}]}}"""
        every { f.comickSource.name } returns "Comick"
        f.library += libraryEntryOf(title = "Something else")
        val status = f.runToCompletion(sourceManga()) as SearchStatus.Finished.WithResults
        status.results.single().results.size shouldBe 1
    }

    private fun RecommendationSearchHelper.plant(name: String, lock: Any) =
        RecommendationSearchHelper::class.java.getDeclaredField(name).also { it.isAccessible = true }.set(this, lock)

    private fun RecommendationSearchHelper.lock(name: String): Any? =
        RecommendationSearchHelper::class.java.getDeclaredField(name).also { it.isAccessible = true }.get(this)
}
