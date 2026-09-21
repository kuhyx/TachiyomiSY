package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.track.model.TrackSearch
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BaseTrackerTest {

    private val koin = TrackKoin()
    private lateinit var tracker: FakeTracker

    @BeforeEach
    fun setUp() {
        koin.start()
        tracker = FakeTracker()
    }

    @AfterEach
    fun tearDown() {
        koin.stop()
    }

    @Test
    fun identityAndDefaults() {
        tracker.id shouldBe 42L
        tracker.name shouldBe "Fake"
        tracker.client shouldBe koin.networkHelper.client
        tracker.supportsReadingDates shouldBe false
        tracker.supportsPrivateTracking shouldBe false
        tracker.trackPreferences shouldBe koin.trackPreferences
    }

    @Test
    fun isLoggedInNeedsBothCredentials() {
        tracker.isLoggedIn shouldBe false
        koin.trackPreferences.trackUsername(tracker).set("user")
        tracker.isLoggedIn shouldBe false
        koin.trackPreferences.trackPassword(tracker).set("pass")
        tracker.isLoggedIn shouldBe true
    }

    @Test
    fun isLoggedInFlowIsLazy() = runTest {
        val flow = tracker.isLoggedInFlow
        tracker.isLoggedInFlow shouldBe flow
        flow.first() shouldBe false
        koin.trackPreferences.trackUsername(tracker).set("user")
        flow.first() shouldBe false
        koin.trackPreferences.trackPassword(tracker).set("pass")
        flow.first() shouldBe true
    }

    @Test
    fun scoresPassThrough() {
        tracker.get10PointScore(domainTrack(score = 7.5)) shouldBe 7.5
        tracker.indexToScore(3) shouldBe 3.0
    }

    @Test
    fun credentialsRoundTrip() = runTest {
        tracker.login("name", "secret")
        tracker.getUsername() shouldBe "name"
        tracker.getPassword() shouldBe "secret"
        tracker.getDisplayUsername() shouldBe ""
        tracker.saveDisplayUsername("Shown")
        tracker.getDisplayUsername() shouldBe "Shown"

        tracker.logout()
        tracker.getUsername() shouldBe ""
        tracker.getPassword() shouldBe ""
        tracker.isLoggedIn shouldBe false
    }

    @Test
    fun syMetadataDefaultsToNothing() = runTest {
        tracker.getMangaMetadata(domainTrack()).shouldBeNull()
        tracker.searchById("1").shouldBeNull()
        tracker.search("q") shouldBe emptyList<TrackSearch>()
        val item = dbTrack(trackerId = 42L)
        tracker.bind(item) shouldBe item
        tracker.refresh(item) shouldBe item
    }

    @Test
    fun getStatusIsTheSubclass() {
        tracker.getStatus(FakeTracker.READING) shouldNotBe null
        tracker.getStatus(99L).shouldBeNull()
        tracker.getStatusList().size shouldBe 3
        tracker.displayScore(domainTrack(score = 2.0)) shouldBe "2.0"
    }
}
