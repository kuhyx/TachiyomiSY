package eu.kanade.tachiyomi.data.track.myanimelist

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

internal class MyAnimeListTest {

    private lateinit var mal: MyAnimeList

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
        mal = MyAnimeList(TrackerManager.MYANIMELIST)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun identityAndFeatureFlags() {
        mal.id shouldBe TrackerManager.MYANIMELIST
        mal.name shouldBe "MyAnimeList"
        mal.getLogo() shouldBe R.drawable.brand_myanimelist
        mal.supportsReadingDates shouldBe true
        mal.supportsPrivateTracking shouldBe false
    }

    @Test
    fun statusVocabulary() {
        mal.getStatusList() shouldBe listOf(1L, 2L, 3L, 4L, 6L, 7L)
        mal.getReadingStatus() shouldBe MyAnimeList.READING
        mal.getRereadingStatus() shouldBe MyAnimeList.REREADING
        mal.getCompletionStatus() shouldBe MyAnimeList.COMPLETED
    }

    @Test
    fun statusStringsForEveryStatus() {
        mal.getStatus(MyAnimeList.READING) shouldBe MR.strings.reading
        mal.getStatus(MyAnimeList.PLAN_TO_READ) shouldBe MR.strings.plan_to_read
        mal.getStatus(MyAnimeList.COMPLETED) shouldBe MR.strings.completed
        mal.getStatus(MyAnimeList.ON_HOLD) shouldBe MR.strings.on_hold
        mal.getStatus(MyAnimeList.DROPPED) shouldBe MR.strings.dropped
        mal.getStatus(MyAnimeList.REREADING) shouldBe MR.strings.repeating
        mal.getStatus(5L).shouldBeNull()
    }

    @Test
    fun scoresAreWholeNumbers() {
        mal.getScoreList() shouldBe (0..10).map(Int::toString)
        mal.indexToScore(7) shouldBe 7.0
        mal.displayScore(domainTrack(TrackerManager.MYANIMELIST, score = 8.0)) shouldBe "8"
    }

    @Test
    fun authExpiryFlag() {
        mal.getIfAuthExpired() shouldBe false
        mal.setAuthExpired()
        mal.getIfAuthExpired() shouldBe true
        mal.saveCredentials("kuhy", "tok")
        mal.getIfAuthExpired() shouldBe false
    }

    @Test
    fun oauthRoundTrip() {
        mal.loadOAuth().shouldBeNull()
        val oauth = MALOAuth(
            tokenType = "Bearer",
            refreshToken = "r",
            accessToken = "a",
            expiresIn = 1L,
            createdAt = 2L,
        )
        mal.saveOAuth(oauth)
        mal.loadOAuth() shouldBe oauth
        mal.saveOAuth(null)
        mal.loadOAuth().shouldBeNull()
    }

    @Test
    fun logoutClearsEverything() {
        mal.saveCredentials("kuhy", "tok")
        mal.saveOAuth(MALOAuth(tokenType = "Bearer", refreshToken = "r", accessToken = "a", expiresIn = 1L))
        mal.logout()
        mal.isLoggedIn shouldBe false
        mal.loadOAuth().shouldBeNull()
    }
}
