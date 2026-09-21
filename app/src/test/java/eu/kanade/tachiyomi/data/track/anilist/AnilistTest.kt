package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.dto.ALOAuth
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

internal class AnilistTest {

    private lateinit var anilist: Anilist

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
        anilist = Anilist(TrackerManager.ANILIST)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun identityAndFeatureFlags() {
        anilist.id shouldBe TrackerManager.ANILIST
        anilist.name shouldBe "AniList"
        anilist.getLogo() shouldBe R.drawable.brand_anilist
        anilist.supportsReadingDates shouldBe true
        anilist.supportsPrivateTracking shouldBe true
    }

    @Test
    fun statusVocabulary() {
        anilist.getStatusList() shouldBe listOf(1L, 2L, 3L, 4L, 5L, 6L)
        anilist.getReadingStatus() shouldBe Anilist.READING
        anilist.getRereadingStatus() shouldBe Anilist.REREADING
        anilist.getCompletionStatus() shouldBe Anilist.COMPLETED
    }

    @Test
    fun statusStringsForEveryStatus() {
        anilist.getStatus(Anilist.READING) shouldBe MR.strings.reading
        anilist.getStatus(Anilist.PLAN_TO_READ) shouldBe MR.strings.plan_to_read
        anilist.getStatus(Anilist.COMPLETED) shouldBe MR.strings.completed
        anilist.getStatus(Anilist.ON_HOLD) shouldBe MR.strings.on_hold
        anilist.getStatus(Anilist.DROPPED) shouldBe MR.strings.dropped
        anilist.getStatus(Anilist.REREADING) shouldBe MR.strings.repeating
        anilist.getStatus(42L).shouldBeNull()
    }

    @Test
    fun scoresUseFormatHelpers() {
        anilist.scorePreference.set(Anilist.POINT_10)
        anilist.getScoreList() shouldBe (0..10).map(Int::toString)
        anilist.indexToScore(7) shouldBe 70.0
        anilist.get10PointScore(domainTrack(TrackerManager.ANILIST, score = 85.0)) shouldBe 8.5
        anilist.displayScore(domainTrack(TrackerManager.ANILIST, score = 85.0)) shouldBe "8"
    }

    @Test
    fun oauthRoundTrip() {
        anilist.loadOAuth().shouldBeNull()
        val oauth = ALOAuth(accessToken = "tok", tokenType = "Bearer", expires = 5L, expiresIn = 4L)
        anilist.saveOAuth(oauth)
        anilist.loadOAuth() shouldBe oauth
        anilist.saveOAuth(null)
        anilist.loadOAuth().shouldBeNull()
    }

    @Test
    fun logoutClearsEverything() {
        anilist.saveCredentials("777", "tok")
        anilist.saveOAuth(ALOAuth(accessToken = "tok", tokenType = "Bearer", expires = 5L, expiresIn = 4L))
        anilist.isLoggedIn shouldBe true
        anilist.logout()
        anilist.isLoggedIn shouldBe false
        anilist.loadOAuth().shouldBeNull()
    }

    @Test
    fun legacyIntScoreTypeForcesLogout() {
        anilist.saveCredentials("777", "tok")
        anilist.scorePreference.set(Anilist.POINT_5)
        TrackerHarness.store.scriptString("anilist_score_type") { throw ClassCastException("int stored") }
        Anilist(TrackerManager.ANILIST)
        TrackerHarness.store.clear()
        anilist.isLoggedIn shouldBe false
        anilist.scorePreference.get() shouldBe Anilist.POINT_10
    }
}
