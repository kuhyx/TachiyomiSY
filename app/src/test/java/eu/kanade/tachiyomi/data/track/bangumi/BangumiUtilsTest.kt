package eu.kanade.tachiyomi.data.track.bangumi

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class BangumiUtilsTest {

    private fun status(value: Long) = dbTrack(TrackerManager.BANGUMI, status = value)

    @Test
    fun collectionTypesMirrorStatuses() {
        status(Bangumi.PLAN_TO_READ).toApiStatus() shouldBe 1
        status(Bangumi.READING).toApiStatus() shouldBe 3
        status(Bangumi.DROPPED).toApiStatus() shouldBe 5
    }

    @Test
    fun outOfRangeStatusFails() {
        shouldThrow<IllegalArgumentException> { status(0L).toApiStatus() }
        shouldThrow<IllegalArgumentException> { status(6L).toApiStatus() }
    }
}
