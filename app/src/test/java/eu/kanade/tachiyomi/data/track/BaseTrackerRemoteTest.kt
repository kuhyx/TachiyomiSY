package eu.kanade.tachiyomi.data.track

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.util.system.toast
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

/** The single-field remote pushes and [BaseTracker.register]; the failure arms end in a toast. */
internal class BaseTrackerRemoteTest {

    private val koin = TrackKoin()
    private lateinit var tracker: FakeTracker

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        koin.start()
        coEvery { koin.insertTrack.await(any()) } returns Unit
        mockkStatic("eu.kanade.tachiyomi.util.system.ToastExtensionsKt")
        every { any<Context>().toast(text = any<String>()) } returns mockk()
        tracker = FakeTracker()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        koin.stop()
        Dispatchers.resetMain()
    }

    private fun track(
        status: Long = 0L,
        lastChapterRead: Double = 0.0,
        totalChapters: Long = 0L,
    ): Track = dbTrack(
        trackerId = 42L,
        status = status,
        lastChapterRead = lastChapterRead,
        totalChapters = totalChapters,
    )

    @Test
    fun registerBindsOrToasts() = runTest {
        val item = track()
        coEvery { koin.addTracks.bind(tracker, item, 7L) } returns Unit
        tracker.register(item, 7L)
        item.mangaId shouldBe 7L
        coVerify(exactly = 1) { koin.addTracks.bind(tracker, item, 7L) }

        coEvery { koin.addTracks.bind(tracker, item, 8L) } throws IOException("offline")
        tracker.register(item, 8L)
        item.mangaId shouldBe 8L
        verify(exactly = 1) { koin.application.toast("offline") }
    }

    @Test
    fun setRemoteStatusFillsChapters() = runTest {
        val done = track(totalChapters = 12L)
        tracker.setRemoteStatus(done, FakeTracker.COMPLETED)
        done.lastChapterRead shouldBe 12.0

        val noTotal = track()
        tracker.setRemoteStatus(noTotal, FakeTracker.COMPLETED)
        noTotal.lastChapterRead shouldBe 0.0

        val reading = track(totalChapters = 12L)
        tracker.setRemoteStatus(reading, FakeTracker.READING)
        reading.lastChapterRead shouldBe 0.0
        tracker.updated.size shouldBe 3
        coVerify(exactly = 3) { koin.insertTrack.await(any()) }
    }

    @Test
    fun lastChapterStartsReading() = runTest {
        val fresh = track()
        tracker.setRemoteLastChapterRead(fresh, 3)
        fresh.status shouldBe FakeTracker.READING
        fresh.lastChapterRead shouldBe 3.0

        val ongoing = track(lastChapterRead = 2.0)
        tracker.setRemoteLastChapterRead(ongoing, 3)
        ongoing.status shouldBe 0L

        val zero = track()
        tracker.setRemoteLastChapterRead(zero, 0)
        zero.status shouldBe 0L

        val rereading = track(status = FakeTracker.REREADING)
        tracker.setRemoteLastChapterRead(rereading, 3)
        rereading.status shouldBe FakeTracker.REREADING
    }

    @Test
    fun lastChapterCompletes() = runTest {
        val before = System.currentTimeMillis()
        val last = track(status = FakeTracker.READING, lastChapterRead = 4.0, totalChapters = 5L)
        tracker.setRemoteLastChapterRead(last, 5)
        last.status shouldBe FakeTracker.COMPLETED
        last.finishedReadingDate shouldBeGreaterThan before - 1

        val mid = track(status = FakeTracker.READING, lastChapterRead = 1.0, totalChapters = 5L)
        tracker.setRemoteLastChapterRead(mid, 2)
        mid.status shouldBe FakeTracker.READING
        mid.finishedReadingDate shouldBe 0L
    }

    @Test
    fun scoreDatesAndPrivate() = runTest {
        val item = track()
        tracker.setRemoteScore(item, "5")
        item.score shouldBe 1.0
        tracker.setRemoteScore(item, "missing")
        item.score shouldBe -1.0
        tracker.setRemoteStartDate(item, 100L)
        item.startedReadingDate shouldBe 100L
        tracker.setRemoteFinishDate(item, 200L)
        item.finishedReadingDate shouldBe 200L
        tracker.setRemotePrivate(item, true)
        item.private shouldBe true
        tracker.setRemotePrivate(item, false)
        item.private shouldBe false
        tracker.updated.size shouldBe 6
    }

    @Test
    fun updateFailureToasts() = runTest {
        val failing = FakeTracker(onUpdate = { throw IllegalStateException("remote down") })
        val item = track()
        failing.setRemotePrivate(item, true)
        item.private shouldBe true
        verify(exactly = 1) { koin.application.toast("remote down") }
        coVerify(exactly = 0) { koin.insertTrack.await(any()) }
    }
}
