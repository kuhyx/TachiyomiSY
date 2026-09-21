package eu.kanade.tachiyomi.data.track.mangabaka

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** [MangaBaka.login]: the token exchange, the profile-derived score step and the fallback logout. */
internal class MangaBakaLoginTest {

    private val harness = MangaBakaHarness()
    private val tracker: MangaBaka
        get() = harness.tracker
    private val scoreType
        get() = harness.koin.trackPreferences.mangabakaScoreType

    @BeforeEach
    fun setUp() {
        harness.start(loggedIn = false)
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    private suspend fun loginWith(ratingSteps: Int, nickname: String? = null, preferred: String? = null) {
        harness.enqueue("oauth.json")
        harness.enqueueProfile(ratingSteps = ratingSteps, nickname = nickname, preferred = preferred)
        tracker.login("code-1")
    }

    @Test
    fun loginStoresTokenAndProfile() = runTest {
        loginWith(ratingSteps = 5, nickname = "Nick", preferred = "preferred")
        tracker.isLoggedIn shouldBe true
        tracker.getUsername() shouldBe "user"
        tracker.getPassword() shouldBe "access-1"
        tracker.getDisplayUsername() shouldBe "Nick"
        scoreType.get() shouldBe MangaBaka.STEP_5
        tracker.restoreToken()?.accessToken shouldBe "access-1"
        harness.takeRequest().url.encodedPath shouldBe "/auth/oauth2/token"
        harness.takeRequest().headers["Authorization"] shouldBe "Bearer access-1"
    }

    @Test
    fun displayNameFallsBack() = runTest {
        loginWith(ratingSteps = 1, preferred = "preferred")
        tracker.getDisplayUsername() shouldBe "preferred"
        scoreType.get() shouldBe MangaBaka.STEP_1

        loginWith(ratingSteps = 10)
        tracker.getDisplayUsername() shouldBe "uid"
        scoreType.get() shouldBe MangaBaka.STEP_10
    }

    @Test
    fun everyStepSizeIsKnown() = runTest {
        loginWith(ratingSteps = 20)
        scoreType.get() shouldBe MangaBaka.STEP_20
        loginWith(ratingSteps = 25)
        scoreType.get() shouldBe MangaBaka.STEP_25
    }

    @Test
    fun unknownStepSizeLogsOut() = runTest {
        loginWith(ratingSteps = 3)
        tracker.isLoggedIn shouldBe false
        tracker.restoreToken().shouldBeNull()
        scoreType.get() shouldBe MangaBaka.STEP_1
    }

    @Test
    fun failedTokenExchangeLogsOut() = runTest {
        harness.enqueueRaw("", code = 400)
        tracker.login("user", "bad-code")
        tracker.isLoggedIn shouldBe false
        tracker.restoreToken().shouldBeNull()
        harness.takeRequest().url.encodedPath shouldBe "/auth/oauth2/token"
    }
}
