package eu.kanade.tachiyomi.data.track.hikka

import eu.kanade.tachiyomi.data.track.dbTrack
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class HikkaUtilsTest {

    private fun apiStatus(status: Long): String = dbTrack(trackerId = 10L, status = status).toApiStatus()

    @Test
    fun trackStatusToApi() {
        apiStatus(Hikka.READING) shouldBe "reading"
        apiStatus(Hikka.COMPLETED) shouldBe "completed"
        apiStatus(Hikka.ON_HOLD) shouldBe "on_hold"
        apiStatus(Hikka.DROPPED) shouldBe "dropped"
        apiStatus(Hikka.PLAN_TO_READ) shouldBe "planned"
        apiStatus(Hikka.REREADING) shouldBe "reading"
        shouldThrow<IllegalArgumentException> { apiStatus(9L) }.message shouldBe "Hikka: Unknown status: 9"
    }

    @Test
    fun apiStatusToTrack() {
        toTrackStatus("reading") shouldBe Hikka.READING
        toTrackStatus("completed") shouldBe Hikka.COMPLETED
        toTrackStatus("on_hold") shouldBe Hikka.ON_HOLD
        toTrackStatus("dropped") shouldBe Hikka.DROPPED
        toTrackStatus("planned") shouldBe Hikka.PLAN_TO_READ
        shouldThrow<IllegalArgumentException> { toTrackStatus("rereading") }.message shouldBe
            "Hikka: Unknown status: rereading"
    }

    @Test
    fun slugsHashToStableIds() {
        stringToNumber("test-manga-abc123") shouldBe stringToNumber("test-manga-abc123")
        stringToNumber("test-manga-abc123") shouldNotBe stringToNumber("other")
        stringToNumber("other") shouldBeGreaterThanOrEqual 0L
        stringToNumber("") shouldBeGreaterThanOrEqual 0L
    }
}
