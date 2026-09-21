package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.track.TrackerManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AnilistScoresTest {

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

    private fun useType(type: String) = anilist.scorePreference.set(type)

    private fun display(score: Double) = anilist.displayedScore(domainTrack(TrackerManager.ANILIST, score = score))

    @Test
    fun scoreListPerFormat() {
        useType(Anilist.POINT_10)
        anilist.scoreList() shouldBe (0..10).map(Int::toString)
        useType(Anilist.POINT_100)
        anilist.scoreList().size shouldBe 101
        useType(Anilist.POINT_5)
        anilist.scoreList() shouldBe listOf("0 ★", "1 ★", "2 ★", "3 ★", "4 ★", "5 ★")
        useType(Anilist.POINT_3)
        anilist.scoreList() shouldBe listOf("-", "😦", "😐", "😊")
        useType(Anilist.POINT_10_DECIMAL)
        anilist.scoreList().take(3) shouldBe listOf("0.0", "0.1", "0.2")
        anilist.scoreList().last() shouldBe "10.0"
    }

    @Test
    fun scoreListRejectsUnknownFormat() {
        useType("POINT_7")
        shouldThrow<IllegalStateException> { anilist.scoreList() }
    }

    @Test
    fun scoreForIndexPerFormat() {
        useType(Anilist.POINT_10)
        anilist.scoreForIndex(7) shouldBe 70.0
        useType(Anilist.POINT_100)
        anilist.scoreForIndex(42) shouldBe 42.0
        useType(Anilist.POINT_5)
        anilist.scoreForIndex(0) shouldBe 0.0
        anilist.scoreForIndex(3) shouldBe 50.0
        useType(Anilist.POINT_3)
        anilist.scoreForIndex(0) shouldBe 0.0
        anilist.scoreForIndex(2) shouldBe 60.0
        useType(Anilist.POINT_10_DECIMAL)
        anilist.scoreForIndex(85) shouldBe 85.0
    }

    @Test
    fun scoreForIndexRejectsUnknown() {
        useType("POINT_7")
        shouldThrow<IllegalStateException> { anilist.scoreForIndex(1) }
    }

    @Test
    fun displayedStars() {
        useType(Anilist.POINT_5)
        display(0.0) shouldBe "0 ★"
        display(50.0) shouldBe "3 ★"
    }

    @Test
    fun displayedSmileys() {
        useType(Anilist.POINT_3)
        display(0.0) shouldBe "0"
        display(35.0) shouldBe "😦"
        display(60.0) shouldBe "😐"
        display(61.0) shouldBe "😊"
    }

    @Test
    fun displayedNumbersUseApiScore() {
        useType(Anilist.POINT_100)
        display(85.0) shouldBe "85"
        useType(Anilist.POINT_10)
        display(85.0) shouldBe "8"
    }
}
