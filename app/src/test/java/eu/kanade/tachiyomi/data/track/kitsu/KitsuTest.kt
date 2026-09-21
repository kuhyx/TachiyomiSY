package eu.kanade.tachiyomi.data.track.kitsu

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuOAuth
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

internal class KitsuTest {

    private lateinit var kitsu: Kitsu

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
        kitsu = Kitsu(TrackerManager.KITSU)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun identityAndFeatureFlags() {
        kitsu.id shouldBe TrackerManager.KITSU
        kitsu.name shouldBe "Kitsu"
        kitsu.getLogo() shouldBe R.drawable.brand_kitsu
        kitsu.supportsReadingDates shouldBe true
        kitsu.supportsPrivateTracking shouldBe true
    }

    @Test
    fun statusVocabulary() {
        kitsu.getStatusList() shouldBe listOf(1L, 2L, 3L, 4L, 5L)
        kitsu.getReadingStatus() shouldBe Kitsu.READING
        kitsu.getRereadingStatus() shouldBe -1L
        kitsu.getCompletionStatus() shouldBe Kitsu.COMPLETED
    }

    @Test
    fun statusStringsForEveryStatus() {
        kitsu.getStatus(Kitsu.READING) shouldBe MR.strings.reading
        kitsu.getStatus(Kitsu.PLAN_TO_READ) shouldBe MR.strings.plan_to_read
        kitsu.getStatus(Kitsu.COMPLETED) shouldBe MR.strings.completed
        kitsu.getStatus(Kitsu.ON_HOLD) shouldBe MR.strings.on_hold
        kitsu.getStatus(Kitsu.DROPPED) shouldBe MR.strings.dropped
        kitsu.getStatus(42L).shouldBeNull()
    }

    @Test
    fun scoreListIsHalfStars() {
        val scores = kitsu.getScoreList()
        scores.size shouldBe 20
        scores.take(4) shouldBe listOf("0", "1", "1.5", "2")
        scores.last() shouldBe "10"
    }

    @Test
    fun indexToScoreMapsHalfSteps() {
        kitsu.indexToScore(0) shouldBe 0.0
        kitsu.indexToScore(1) shouldBe 1.0
        kitsu.indexToScore(2) shouldBe 1.5
        kitsu.indexToScore(19) shouldBe 10.0
    }

    @Test
    fun displayScoreDropsTrailingZero() {
        kitsu.displayScore(domainTrack(TrackerManager.KITSU, score = 7.5)) shouldBe "7.5"
        kitsu.displayScore(domainTrack(TrackerManager.KITSU, score = 7.0)) shouldBe "7"
    }

    @Test
    fun tokenRoundTrip() {
        kitsu.restoreToken().shouldBeNull()
        val token = TrackerHarness.json.decodeFromString<KitsuOAuth>(
            """{"access_token":"a","token_type":"b","created_at":1,"expires_in":2,"refresh_token":"r"}""",
        )
        kitsu.saveToken(token)
        kitsu.restoreToken() shouldBe token
        kitsu.saveToken(null)
        kitsu.restoreToken().shouldBeNull()
    }

    @Test
    fun logoutClearsEverything() {
        kitsu.saveCredentials("user", "9001")
        kitsu.isLoggedIn shouldBe true
        kitsu.logout()
        kitsu.isLoggedIn shouldBe false
        kitsu.getUsername() shouldBe ""
        kitsu.restoreToken().shouldBeNull()
    }
}
