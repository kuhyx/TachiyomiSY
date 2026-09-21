package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.track.TrackerManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AnilistUtilsTest {

    private val scoreType = TrackerHarness.trackPreferences.anilistScoreType

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun score(value: Double, type: String) = domainTrack(TrackerManager.ANILIST, score = value).also {
        scoreType.set(type)
    }

    @Test
    fun apiStatusForEveryStatus() {
        val expected = mapOf(
            Anilist.READING to "CURRENT",
            Anilist.COMPLETED to "COMPLETED",
            Anilist.ON_HOLD to "PAUSED",
            Anilist.DROPPED to "DROPPED",
            Anilist.PLAN_TO_READ to "PLANNING",
            Anilist.REREADING to "REPEATING",
        )
        expected.forEach { (status, api) ->
            dbTrack(TrackerManager.ANILIST, status = status).toApiStatus() shouldBe api
        }
    }

    @Test
    fun apiStatusRejectsUnknown() {
        shouldThrow<IllegalArgumentException> { dbTrack(TrackerManager.ANILIST, status = 42L).toApiStatus() }
    }

    @Test
    fun tenPointScore() {
        score(85.0, Anilist.POINT_10).toApiScore() shouldBe "8"
    }

    @Test
    fun hundredPointScore() {
        score(85.0, Anilist.POINT_100).toApiScore() shouldBe "85"
    }

    @Test
    fun tenPointDecimalScore() {
        score(85.0, Anilist.POINT_10_DECIMAL).toApiScore() shouldBe "8.5"
    }

    @Test
    fun starScoreBuckets() {
        listOf(0.0 to "0", 29.0 to "1", 49.0 to "2", 69.0 to "3", 89.0 to "4", 90.0 to "5").forEach { (raw, stars) ->
            score(raw, Anilist.POINT_5).toApiScore() shouldBe stars
        }
    }

    @Test
    fun smileyScoreBuckets() {
        listOf(0.0 to "0", 35.0 to ":(", 60.0 to ":|", 61.0 to ":)").forEach { (raw, smiley) ->
            score(raw, Anilist.POINT_3).toApiScore() shouldBe smiley
        }
    }

    @Test
    fun unknownScoreTypeFails() {
        shouldThrow<IllegalArgumentException> { score(1.0, "POINT_7").toApiScore() }
    }
}
