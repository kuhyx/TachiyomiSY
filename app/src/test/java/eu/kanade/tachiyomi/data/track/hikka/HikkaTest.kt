package eu.kanade.tachiyomi.data.track.hikka

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.data.track.hikka.dto.hikkaOAuth
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

/** The [Hikka] tracker's vocabulary, login and token storage. */
internal class HikkaTest {

    private val harness = HikkaHarness()
    private val tracker: Hikka
        get() = harness.tracker

    @BeforeEach
    fun setUp() {
        harness.start(loggedIn = false)
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun identityAndFlags() {
        tracker.id shouldBe 10L
        tracker.name shouldBe "Hikka"
        tracker.getLogo() shouldBe R.drawable.brand_hikka
        tracker.supportsReadingDates shouldBe true
        tracker.supportsPrivateTracking shouldBe false
        tracker.getReadingStatus() shouldBe Hikka.READING
        tracker.getRereadingStatus() shouldBe Hikka.REREADING
        tracker.getCompletionStatus() shouldBe Hikka.COMPLETED
        tracker.getScoreList() shouldBe (0..10).map { it.toString() }
        tracker.displayScore(domainTrack(score = 7.9)) shouldBe "7"
    }

    @Test
    fun statusesHaveLabels() {
        tracker.getStatusList() shouldBe listOf(0L, 1L, 2L, 3L, 4L, 5L)
        tracker.getStatus(Hikka.READING) shouldBe MR.strings.reading
        tracker.getStatus(Hikka.PLAN_TO_READ) shouldBe MR.strings.plan_to_read
        tracker.getStatus(Hikka.COMPLETED) shouldBe MR.strings.completed
        tracker.getStatus(Hikka.ON_HOLD) shouldBe MR.strings.on_hold
        tracker.getStatus(Hikka.DROPPED) shouldBe MR.strings.dropped
        tracker.getStatus(Hikka.REREADING) shouldBe MR.strings.repeating
        tracker.getStatus(42L).shouldBeNull()
    }

    @Test
    fun loginStoresTokenAndUser() = runTest {
        harness.enqueue("oauth.json")
        harness.enqueue("user.json")
        tracker.login("user", "reference-1")
        tracker.isLoggedIn shouldBe true
        tracker.getUsername() shouldBe "user-ref"
        tracker.getPassword() shouldBe "hikka-secret"
        tracker.getDisplayUsername() shouldBe "hikka_user"
        tracker.loadOAuth()?.accessToken shouldBe "hikka-secret"
        harness.takeRequest().url.encodedPath shouldBe "/auth/token"
        harness.takeRequest().headers["auth"] shouldBe "hikka-secret"
    }

    @Test
    fun failedLoginLogsOut() = runTest {
        harness.enqueueRaw("", code = 400)
        tracker.login("reference-1")
        tracker.isLoggedIn shouldBe false
        tracker.loadOAuth().shouldBeNull()

        harness.enqueue("oauth.json")
        harness.enqueueRaw("", code = 500)
        tracker.login("reference-1")
        tracker.isLoggedIn shouldBe false
        tracker.loadOAuth().shouldBeNull()
    }

    @Test
    fun oauthRoundTripsThroughPrefs() {
        tracker.loadOAuth().shouldBeNull()
        harness.koin.trackPreferences.trackToken(tracker).set("garbage")
        tracker.loadOAuth().shouldBeNull()
        tracker.saveOAuth(hikkaOAuth(expiration = 5L, accessToken = "a"))
        checkNotNull(tracker.loadOAuth()).accessToken shouldBe "a"
        tracker.saveOAuth(null)
        tracker.loadOAuth().shouldBeNull()
    }

    @Test
    fun logoutClearsCredentials() {
        tracker.saveCredentials("user", "pass")
        tracker.saveOAuth(hikkaOAuth(expiration = 5L))
        tracker.logout()
        tracker.isLoggedIn shouldBe false
        tracker.loadOAuth().shouldBeNull()
    }
}
