package eu.kanade.tachiyomi.data.track.shikimori

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMOAuth
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

internal class ShikimoriTest {

    private lateinit var shikimori: Shikimori

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
        shikimori = Shikimori(TrackerManager.SHIKIMORI)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun identityAndFeatureFlags() {
        shikimori.id shouldBe TrackerManager.SHIKIMORI
        shikimori.name shouldBe "Shikimori"
        shikimori.getLogo() shouldBe R.drawable.brand_shikimori
        shikimori.supportsReadingDates shouldBe false
        shikimori.supportsPrivateTracking shouldBe false
    }

    @Test
    fun statusVocabulary() {
        shikimori.getStatusList() shouldBe listOf(1L, 2L, 3L, 4L, 5L, 6L)
        shikimori.getReadingStatus() shouldBe Shikimori.READING
        shikimori.getRereadingStatus() shouldBe Shikimori.REREADING
        shikimori.getCompletionStatus() shouldBe Shikimori.COMPLETED
    }

    @Test
    fun statusStringsForEveryStatus() {
        shikimori.getStatus(Shikimori.READING) shouldBe MR.strings.reading
        shikimori.getStatus(Shikimori.PLAN_TO_READ) shouldBe MR.strings.plan_to_read
        shikimori.getStatus(Shikimori.COMPLETED) shouldBe MR.strings.completed
        shikimori.getStatus(Shikimori.ON_HOLD) shouldBe MR.strings.on_hold
        shikimori.getStatus(Shikimori.DROPPED) shouldBe MR.strings.dropped
        shikimori.getStatus(Shikimori.REREADING) shouldBe MR.strings.repeating
        shikimori.getStatus(42L).shouldBeNull()
    }

    @Test
    fun scoresAreWholeNumbers() {
        shikimori.getScoreList() shouldBe (0..10).map(Int::toString)
        shikimori.displayScore(domainTrack(TrackerManager.SHIKIMORI, score = 8.0)) shouldBe "8"
    }

    @Test
    fun tokenRoundTrip() {
        shikimori.restoreToken().shouldBeNull()
        val token = SMOAuth("a", "Bearer", 1L, 2L, "r")
        shikimori.saveToken(token)
        shikimori.restoreToken() shouldBe token
        shikimori.saveToken(null)
        shikimori.restoreToken().shouldBeNull()
    }

    @Test
    fun logoutClearsEverything() {
        shikimori.saveCredentials("31337", "tok")
        shikimori.saveToken(SMOAuth("a", "Bearer", 1L, 2L, "r"))
        shikimori.logout()
        shikimori.isLoggedIn shouldBe false
        shikimori.restoreToken().shouldBeNull()
    }
}
