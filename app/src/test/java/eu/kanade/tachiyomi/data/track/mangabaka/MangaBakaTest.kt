package eu.kanade.tachiyomi.data.track.mangabaka

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.dbTrack
import eu.kanade.tachiyomi.data.track.domainTrack
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

/** The [MangaBaka] tracker's vocabulary, score scale and token handling. */
internal class MangaBakaTest {

    private val harness = MangaBakaHarness()
    private val tracker: MangaBaka
        get() = harness.tracker

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun identityAndFlags() {
        tracker.id shouldBe 11L
        tracker.name shouldBe "MangaBaka"
        tracker.getLogo() shouldBe R.drawable.brand_mangabaka
        tracker.supportsReadingDates shouldBe true
        tracker.supportsPrivateTracking shouldBe true
        tracker.getReadingStatus() shouldBe MangaBaka.READING
        tracker.getRereadingStatus() shouldBe MangaBaka.REREADING
        tracker.getCompletionStatus() shouldBe MangaBaka.COMPLETED
        tracker.displayScore(domainTrack(score = 87.6)) shouldBe "87"
    }

    @Test
    fun statusesHaveLabels() {
        tracker.getStatusList() shouldBe listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L)
        tracker.getStatus(MangaBaka.CONSIDERING) shouldBe MR.strings.considering
        tracker.getStatus(MangaBaka.COMPLETED) shouldBe MR.strings.completed
        tracker.getStatus(MangaBaka.DROPPED) shouldBe MR.strings.dropped
        tracker.getStatus(MangaBaka.PAUSED) shouldBe MR.strings.paused
        tracker.getStatus(MangaBaka.PLAN_TO_READ) shouldBe MR.strings.plan_to_read
        tracker.getStatus(MangaBaka.READING) shouldBe MR.strings.reading
        tracker.getStatus(MangaBaka.REREADING) shouldBe MR.strings.repeating
        tracker.getStatus(99L).shouldBeNull()
    }

    @Test
    fun scoreListFollowsStepPref() {
        val preference = harness.koin.trackPreferences.mangabakaScoreType
        tracker.getScoreList().size shouldBe 101
        preference.set(MangaBaka.STEP_5)
        tracker.getScoreList().size shouldBe 21
        preference.set(MangaBaka.STEP_10)
        tracker.getScoreList().size shouldBe 11
        preference.set(MangaBaka.STEP_20)
        tracker.getScoreList() shouldBe listOf("0", "20", "40", "60", "80", "100")
        preference.set(MangaBaka.STEP_25)
        tracker.getScoreList() shouldBe listOf("0", "25", "50", "75", "100")
        preference.set("STEP_7")
        shouldThrow<IllegalStateException> { tracker.getScoreList() }.message shouldBe "Unknown score type"
    }

    @Test
    fun tokensRoundTripThroughPrefs() {
        val stored = checkNotNull(tracker.restoreToken())
        stored.accessToken shouldBe "access-1"
        harness.koin.trackPreferences.trackToken(tracker).set("not json")
        tracker.restoreToken().shouldBeNull()
        tracker.saveToken(null)
        tracker.restoreToken().shouldBeNull()
    }

    @Test
    fun logoutClearsEverything() = runTest {
        tracker.saveCredentials("user", "access-1")
        tracker.isLoggedIn shouldBe true
        tracker.logout()
        tracker.isLoggedIn shouldBe false
        // The interceptor re-saves the cleared token as JSON `null` after the preference is deleted.
        harness.koin.trackPreferences.trackToken(tracker).get() shouldBe "null"
        tracker.restoreToken().shouldBeNull()
    }

    @Test
    fun deleteRemovesTheEntry() = runTest {
        harness.enqueueRaw("")
        tracker.delete(domainTrack(trackerId = 11L).copy(remoteId = 1234L))
        val request = harness.takeRequest()
        request.method shouldBe "DELETE"
        request.url.encodedPath shouldBe "/v1/my/library/1234"
    }

    /** A stubbed API answers without suspending, which is the only way past the tail call. */
    @Test
    fun deleteReturnsWhenApiDoesNot() = runTest {
        mockkConstructor(MangaBakaApi::class)
        try {
            coEvery { anyConstructed<MangaBakaApi>().deleteLibManga(any()) } returns Unit
            tracker.delete(domainTrack(trackerId = 11L))
            coVerify(exactly = 1) { anyConstructed<MangaBakaApi>().deleteLibManga(any()) }
        } finally {
            unmockkAll()
        }
    }

    @Test
    fun refreshCopiesTheRemoteEntry() = runTest {
        harness.enqueue("library_entry.json")
        harness.enqueue("series.json")
        val track = dbTrack(trackerId = 11L, remoteId = 1234L)
        tracker.refresh(track) shouldBe track
        track.title shouldBe "Testing Story"
        track.status shouldBe MangaBaka.READING
        track.score shouldBe 80.0
        track.lastChapterRead shouldBe 12.5
        track.private shouldBe true
    }

    @Test
    fun refreshWithoutEntryFails() = runTest {
        harness.enqueueRaw("", code = 404)
        val error = shouldThrow<NoSuchElementException> { tracker.refresh(dbTrack(trackerId = 11L, remoteId = 1L)) }
        error.message shouldBe "Could not find manga"
    }
}
