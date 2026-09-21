package eu.kanade.tachiyomi.data.track.bangumi

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMOAuth
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

internal class BangumiTest {

    private lateinit var bangumi: Bangumi

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
        bangumi = Bangumi(TrackerManager.BANGUMI)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun identityAndFeatureFlags() {
        bangumi.id shouldBe TrackerManager.BANGUMI
        bangumi.name shouldBe "Bangumi"
        bangumi.getLogo() shouldBe R.drawable.brand_bangumi
        bangumi.supportsReadingDates shouldBe false
        bangumi.supportsPrivateTracking shouldBe true
    }

    @Test
    fun statusVocabulary() {
        bangumi.getStatusList() shouldBe listOf(3L, 2L, 4L, 5L, 1L)
        bangumi.getReadingStatus() shouldBe Bangumi.READING
        bangumi.getRereadingStatus() shouldBe -1L
        bangumi.getCompletionStatus() shouldBe Bangumi.COMPLETED
    }

    @Test
    fun statusStringsForEveryStatus() {
        bangumi.getStatus(Bangumi.READING) shouldBe MR.strings.reading
        bangumi.getStatus(Bangumi.PLAN_TO_READ) shouldBe MR.strings.plan_to_read
        bangumi.getStatus(Bangumi.COMPLETED) shouldBe MR.strings.completed
        bangumi.getStatus(Bangumi.ON_HOLD) shouldBe MR.strings.on_hold
        bangumi.getStatus(Bangumi.DROPPED) shouldBe MR.strings.dropped
        bangumi.getStatus(42L).shouldBeNull()
    }

    @Test
    fun scoresAreWholeNumbers() {
        bangumi.getScoreList() shouldBe (0..10).map(Int::toString)
        bangumi.displayScore(domainTrack(TrackerManager.BANGUMI, score = 8.0)) shouldBe "8"
    }

    @Test
    fun tokenRoundTrip() {
        bangumi.restoreToken().shouldBeNull()
        val token = BGMOAuth("a", "Bearer", 1L, 2L, "r", 3L)
        bangumi.saveToken(token)
        bangumi.restoreToken() shouldBe token
        bangumi.saveToken(null)
        bangumi.restoreToken().shouldBeNull()
    }

    @Test
    fun logoutClearsEverything() {
        bangumi.saveCredentials("kuhy42", "tok")
        bangumi.saveToken(BGMOAuth("a", "Bearer", 1L, 2L, "r", 3L))
        bangumi.logout()
        bangumi.isLoggedIn shouldBe false
        bangumi.restoreToken().shouldBeNull()
    }
}
