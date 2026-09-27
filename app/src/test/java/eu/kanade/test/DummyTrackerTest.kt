package eu.kanade.test

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import exh.recs.sources.track
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

internal class DummyTrackerTest {
    private val tracker = DummyTracker(id = 3L, name = "Dummy")

    @Test
    fun defaultsDescribeATracker() {
        tracker.supportsReadingDates shouldBe false
        tracker.supportsPrivateTracking shouldBe false
        tracker.isLoggedIn shouldBe false
        runBlocking { tracker.isLoggedInFlow.first() } shouldBe false
        tracker.getLogo() shouldBe R.drawable.brand_anilist
        tracker.getStatusList() shouldContainExactly (1L..6L).toList()
        tracker.getReadingStatus() shouldBe 1L
        tracker.getRereadingStatus() shouldBe 1L
        tracker.getCompletionStatus() shouldBe 2L
        tracker.getScoreList().size shouldBe 11
        shouldThrow<UnsupportedOperationException> { tracker.client }
    }

    @Test
    fun statusesHaveLabels() {
        (1L..7L).map { tracker.getStatus(it) } shouldBe listOf(
            MR.strings.reading,
            MR.strings.plan_to_read,
            MR.strings.completed,
            MR.strings.on_hold,
            MR.strings.dropped,
            MR.strings.repeating,
            null,
        )
    }

    @Test
    fun scoresComeFromTheList() {
        val domain = track(trackerId = 3L).copy(score = 7.0)
        tracker.get10PointScore(domain) shouldBe 5.4
        tracker.indexToScore(4) shouldBe 4.0
        tracker.displayScore(domain) shouldBe "7.0"
    }

    @Test
    fun remoteCallsEchoOrDoNothing() {
        runBlocking { remoteCalls() }
    }

    private suspend fun remoteCalls() {
        val track = Track.create(3L)
        tracker.update(track, didReadChapter = true) shouldBe track
        tracker.bind(track, hasReadChapters = false) shouldBe track
        tracker.refresh(track) shouldBe track
        tracker.search("q") shouldBe emptyList()
        tracker.login("u", "p")
        tracker.register(track, 1L)
        tracker.setRemoteStatus(track, 1L)
        tracker.setRemoteLastChapterRead(track, 2)
        tracker.setRemoteScore(track, "5")
        tracker.setRemoteStartDate(track, 1L)
        tracker.setRemoteFinishDate(track, 2L)
        tracker.setRemotePrivate(track, private = true)
        tracker.searchById("1").shouldBeNull()
        tracker.getMangaMetadata(track(trackerId = 3L)).title shouldBe "test"
    }

    @Test
    fun credentialsAreFixed() {
        tracker.logout()
        tracker.saveDisplayUsername("x")
        tracker.saveCredentials("u", "p")
        tracker.getUsername() shouldBe "username"
        tracker.getDisplayUsername() shouldBe "UserName"
        tracker.getPassword() shouldBe "passw0rd"
    }

    @Test
    fun itIsAValue() {
        val copy = tracker.copy(name = "Other", isLoggedIn = true)
        copy.name shouldBe "Other"
        copy.isLoggedIn shouldBe true
        tracker shouldBe tracker.copy()
        tracker.hashCode() shouldBe tracker.copy().hashCode()
        tracker.toString().contains("Dummy") shouldBe true
        tracker.valSearchResults shouldBe emptyList()
        tracker.val10PointScore shouldBe 5.4
        tracker.valReadingStatus shouldBe 1L
    }

    @Test
    fun everyValueCanBeGiven() {
        val custom = DummyTracker(
            id = 4L,
            name = "Custom",
            supportsReadingDates = true,
            supportsPrivateTracking = true,
            isLoggedIn = true,
            isLoggedInFlow = flowOf(true),
            valLogo = 9,
            valStatuses = listOf(1L),
            valReadingStatus = 2L,
            valRereadingStatus = 3L,
            valCompletionStatus = 4L,
            valScoreList = listOf("1"),
            val10PointScore = 1.0,
            valSearchResults = emptyList(),
        )
        custom.getLogo() shouldBe 9
        custom.getRereadingStatus() shouldBe 3L
        custom.getCompletionStatus() shouldBe 4L
        custom.indexToScore(0) shouldBe 1.0
    }
}
