package exh.md.utils

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class FollowStatusTest {
    @Test
    fun toDexIsLowercaseName() {
        FollowStatus.PLAN_TO_READ.toDex() shouldBe "plan_to_read"
        FollowStatus.RE_READING.long shouldBe 6L
    }

    @Test
    fun fromDexFallsBack() {
        FollowStatus.fromDex("reading") shouldBe FollowStatus.READING
        FollowStatus.fromDex("on_hold") shouldBe FollowStatus.ON_HOLD
        FollowStatus.fromDex("nope") shouldBe FollowStatus.UNFOLLOWED
        FollowStatus.fromDex(null) shouldBe FollowStatus.UNFOLLOWED
    }

    @Test
    fun fromLongFallsBack() {
        FollowStatus.fromLong(2L) shouldBe FollowStatus.COMPLETED
        FollowStatus.fromLong(5L) shouldBe FollowStatus.DROPPED
        FollowStatus.fromLong(99L) shouldBe FollowStatus.UNFOLLOWED
    }
}
