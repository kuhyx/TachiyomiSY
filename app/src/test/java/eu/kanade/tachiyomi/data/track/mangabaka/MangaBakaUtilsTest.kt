package eu.kanade.tachiyomi.data.track.mangabaka

import eu.kanade.tachiyomi.data.track.dbTrack
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MangaBakaUtilsTest {

    private fun status(status: Long): String = dbTrack(trackerId = 11L, status = status).toApiStatus()

    @Test
    fun everyStatusHasAnApiName() {
        status(MangaBaka.CONSIDERING) shouldBe "considering"
        status(MangaBaka.COMPLETED) shouldBe "completed"
        status(MangaBaka.DROPPED) shouldBe "dropped"
        status(MangaBaka.PAUSED) shouldBe "paused"
        status(MangaBaka.PLAN_TO_READ) shouldBe "plan_to_read"
        status(MangaBaka.READING) shouldBe "reading"
        status(MangaBaka.REREADING) shouldBe "rereading"
    }

    @Test
    fun unknownStatusThrows() {
        shouldThrow<IllegalArgumentException> { status(42L) }.message shouldBe "Unknown status: 42"
    }
}
