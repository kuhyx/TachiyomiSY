package eu.kanade.tachiyomi.data.track.kitsu

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class KitsuUtilsTest {

    private fun track(status: Long = Kitsu.READING, score: Double = 0.0) =
        dbTrack(trackerId = TrackerManager.KITSU, status = status, score = score)

    @Test
    fun apiStatusForEveryKnownStatus() {
        track(Kitsu.READING).toApiStatus() shouldBe "current"
        track(Kitsu.COMPLETED).toApiStatus() shouldBe "completed"
        track(Kitsu.ON_HOLD).toApiStatus() shouldBe "on_hold"
        track(Kitsu.DROPPED).toApiStatus() shouldBe "dropped"
        track(Kitsu.PLAN_TO_READ).toApiStatus() shouldBe "planned"
    }

    @Test
    fun apiStatusRejectsUnknown() {
        shouldThrow<IllegalStateException> { track(status = 99L).toApiStatus() }
    }

    @Test
    fun apiScoreDoublesPositiveScores() {
        track(score = 7.5).toApiScore() shouldBe "15"
        track(score = 10.0).toApiScore() shouldBe "20"
    }

    @Test
    fun apiScoreIsNullWhenUnrated() {
        track(score = 0.0).toApiScore().shouldBeNull()
        track(score = -1.0).toApiScore().shouldBeNull()
    }
}
