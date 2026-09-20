package eu.kanade.tachiyomi.data.track

import exh.md.utils.FollowStatus
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class TrackStatusTest {

    /** The MangaDex table is keyed by an enum: a new [FollowStatus] must land in it, UNFOLLOWED excepted. */
    @Test
    fun followStatusesAllMapped() {
        val mapped = TrackStatus.mdListStatuses.keys
        val expected = FollowStatus.entries.filterNot { it == FollowStatus.UNFOLLOWED }.map { it.long }
        mapped shouldContainExactly expected.toSet()
    }

    @Test
    fun unfollowedIsNotAReadingState() {
        TrackStatus.mdListStatuses[FollowStatus.UNFOLLOWED.long] shouldBe null
    }
}
