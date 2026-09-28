package exh.recs.batch

import android.content.Context
import android.content.ContextWrapper
import android.net.wifi.WifiManager
import eu.kanade.tachiyomi.source.model.MangasPage
import exh.recs.sources.sourceManga
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPowerManager
import java.util.concurrent.CopyOnWriteArrayList

/** The helper's error, cancellation and lock paths, around a source tied to neither a source nor a tracker. */
@RunWith(RobolectricTestRunner::class)
internal class RecommendationSearchFallbackTest {
    private val rig = RecsSearchRig()
    private var answer: suspend () -> MangasPage = { pageOf("Kept title", "Owned title") }
    private lateinit var helper: RecommendationSearchHelper

    @Before
    fun setUp() {
        rig.sources = { listOf(FakeRecsSource("Loose") { answer() }) }
        helper = rig.install()
    }

    @After
    fun tearDown() {
        ShadowPowerManager.clearWakeLocks()
        unmockkAll()
        rig.uninstall()
    }

    @Test
    fun smartSearchFindsLibrary() {
        rig.preferences.recommendationSearchFlags.set(SearchFlags.HIDE_LIBRARY_RESULTS)
        val status = helper.finish().shouldBeInstanceOf<SearchStatus.Finished.WithResults>()
        status.results.single().results.keys.map { it.title } shouldBe listOf("Kept title")
    }

    @Test
    fun locksAreHeldWhileSearching() {
        val held = CopyOnWriteArrayList<Boolean>()
        answer = {
            held += ShadowPowerManager.getLatestWakeLock()?.isHeld == true
            held += wifiLocks() == 1
            pageOf("Kept title")
        }
        helper.finish().shouldBeInstanceOf<SearchStatus.Finished.WithResults>()
        held shouldContainExactly listOf(true, true)
        val wake = checkNotNull(ShadowPowerManager.getLatestWakeLock())
        wake.isHeld shouldBe false
        shadowOf(wake).timesHeld shouldBe 1
        wifiLocks() shouldBe 0
    }

    @Test
    fun missingLockServicesTolerated() {
        val noLocks = object : ContextWrapper(rig.application) {
            override fun getApplicationContext(): Context = this
            override fun getSystemService(name: String): Any? = when (name) {
                Context.POWER_SERVICE, Context.WIFI_SERVICE -> null
                else -> super.getSystemService(name)
            }
        }
        RecommendationSearchHelper(noLocks).finish().shouldBeInstanceOf<SearchStatus.Finished.WithResults>()
        ShadowPowerManager.getLatestWakeLock() shouldBe null
    }

    @Test
    fun aNamelessFailureHasNoMessage() {
        every { rig.sourceManager.getOrStub(any()) } throws IllegalStateException()
        helper.finish() shouldBe SearchStatus.Error("")
    }

    @Test
    fun errorsEscapeAfterRelease() {
        every { rig.sourceManager.getOrStub(any()) } throws NotImplementedError("fatal")
        val errors = mutableListOf<Throwable>()
        helper.finish(errors).shouldBeInstanceOf<SearchStatus.Processing>()
        errors.map { it.message } shouldContainExactly listOf("fatal")
    }

    @Test
    fun cancellingStopsQuietly() {
        val started = CompletableDeferred<Unit>()
        answer = {
            started.complete(Unit)
            CompletableDeferred<MangasPage>().await()
        }
        val job = checkNotNull(helper.runSearch(CoroutineScope(Dispatchers.IO), listOf(sourceManga())))
        runBlocking {
            withTimeout(FAKE_WAIT_MS) {
                started.await()
                job.cancel()
                job.join()
            }
        }
        helper.status.value.shouldBeInstanceOf<SearchStatus.Processing>()
    }

    private fun wifiLocks(): Int = shadowOf(rig.application.getSystemService(WifiManager::class.java)).activeLockCount
}
