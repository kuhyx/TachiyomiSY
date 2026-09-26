package eu.kanade.test

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

internal class DummyTrackerTest {
    private val tracker = DummyTracker(id = 1, name = "Dummy")
    private val track = Track.create(1)

    @Test
    fun describesItself() = runTest {
        tracker.isLoggedIn shouldBe false
        tracker.isLoggedInFlow.first() shouldBe false
        tracker.supportsReadingDates shouldBe false
        tracker.supportsPrivateTracking shouldBe false
        tracker.getStatusList() shouldBe (1L..6L).toList()
        tracker.getReadingStatus() shouldBe 1L
        tracker.getRereadingStatus() shouldBe 1L
        tracker.getCompletionStatus() shouldBe 2L
        tracker.getLogo() shouldBe tracker.valLogo
        shouldThrow<UnsupportedOperationException> { tracker.client }
    }

    @Test
    fun namesEveryStatus() {
        tracker.getStatus(1) shouldBe MR.strings.reading
        tracker.getStatus(2) shouldBe MR.strings.plan_to_read
        tracker.getStatus(3) shouldBe MR.strings.completed
        tracker.getStatus(4) shouldBe MR.strings.on_hold
        tracker.getStatus(5) shouldBe MR.strings.dropped
        tracker.getStatus(6) shouldBe MR.strings.repeating
        tracker.getStatus(7).shouldBeNull()
    }

    @Test
    fun scoresComeFromTheFields() {
        tracker.getScoreList().size shouldBe 11
        tracker.get10PointScore(domainTrack()) shouldBe 5.4
        tracker.indexToScore(3) shouldBe 3.0
        tracker.displayScore(domainTrack(score = 2.0)) shouldBe "2.0"
    }

    @Test
    fun remoteCallsAreEchoes() = runTest {
        tracker.update(track, didReadChapter = true) shouldBeSameInstanceAs track
        tracker.bind(track, hasReadChapters = false) shouldBeSameInstanceAs track
        tracker.refresh(track) shouldBeSameInstanceAs track
        tracker.search("q") shouldBe emptyList()
        DummyTracker(id = 1, name = "D", valSearchResults = listOf(TrackSearch.create(1))).search("q").size shouldBe 1
        tracker.searchById("1").shouldBeNull()
        tracker.getMangaMetadata(domainTrack()).title shouldBe "test"
    }

    @Test
    fun writesAreNoOps() = runTest {
        tracker.login("u", "p")
        tracker.logout()
        tracker.saveDisplayUsername("x")
        tracker.saveCredentials("u", "p")
        tracker.register(track, mangaId = 1)
        tracker.setRemoteStatus(track, status = 1)
        tracker.setRemoteLastChapterRead(track, chapterNumber = 1)
        tracker.setRemoteScore(track, scoreString = "1")
        tracker.setRemoteStartDate(track, epochMillis = 1)
        tracker.setRemoteFinishDate(track, epochMillis = 1)
        tracker.setRemotePrivate(track, private = true)
        tracker.getUsername() shouldBe "username"
        tracker.getDisplayUsername() shouldBe "UserName"
        tracker.getPassword() shouldBe "passw0rd"
    }
}
