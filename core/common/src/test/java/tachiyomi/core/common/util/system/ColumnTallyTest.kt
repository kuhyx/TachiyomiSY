package tachiyomi.core.common.util.system

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ColumnTallyTest {
    @Test
    fun longWhiteRunFromTopIsStreak() {
        val tally = ColumnTally()
        repeat(20) { tally.white(it) }
        tally.finish()
        tally.whitePixels shouldBe 20
        tally.blackPixels shouldBe 0
        tally.hasWhiteStreak shouldBe true
        tally.hasBlackStreak shouldBe false
        tally.topWhiteRun shouldBe 20
        tally.bottomWhiteRun shouldBe 20
        tally.topBlackRun.shouldBeNull()
        tally.bottomBlackRun.shouldBeNull()
    }

    @Test
    fun shortWhiteRunSetsNothing() {
        val tally = ColumnTally()
        repeat(5) { tally.white(it) }
        tally.black()
        tally.finish()
        tally.hasWhiteStreak shouldBe false
        tally.topWhiteRun.shouldBeNull()
        tally.bottomWhiteRun.shouldBeNull()
        tally.bottomBlackRun.shouldBeNull()
        tally.blackPixels shouldBe 1
    }

    @Test
    fun lateWhiteRunIsNotTopRun() {
        val tally = ColumnTally()
        repeat(3) { tally.other(it) }
        repeat(8) { tally.white(3 + it) }
        tally.finish()
        tally.topWhiteRun.shouldBeNull()
        tally.bottomWhiteRun shouldBe 8
        tally.whitePixels shouldBe 8
    }

    @Test
    fun blackStreakNeedsFourteen() {
        val tally = ColumnTally()
        repeat(13) { tally.black() }
        tally.hasBlackStreak shouldBe false
        tally.black()
        tally.hasBlackStreak shouldBe true
        tally.white(14)
        tally.topBlackRun shouldBe 14
        tally.blackPixels shouldBe 14
    }

    @Test
    fun lateBlackRunIsNotTopRun() {
        val tally = ColumnTally()
        repeat(3) { tally.white(it) }
        repeat(8) { tally.black() }
        tally.other(11)
        tally.finish()
        tally.topBlackRun.shouldBeNull()
        tally.bottomBlackRun.shouldBeNull()
        tally.bottomWhiteRun.shouldBeNull()
    }

    @Test
    fun openBlackRunIsBottomRun() {
        val tally = ColumnTally()
        repeat(7) { tally.black() }
        tally.finish()
        tally.bottomBlackRun shouldBe 7
        tally.bottomWhiteRun.shouldBeNull()
        tally.topBlackRun.shouldBeNull()
    }

    @Test
    fun shortRunsAtBottomSetNothing() {
        val tally = ColumnTally()
        tally.black()
        tally.finish()
        tally.bottomBlackRun.shouldBeNull()
        tally.bottomWhiteRun.shouldBeNull()
    }

    @Test
    fun shortBlackRunIsIgnored() {
        val tally = ColumnTally()
        repeat(2) { tally.black() }
        tally.white(2)
        tally.topBlackRun.shouldBeNull()
        tally.hasBlackStreak shouldBe false
    }

    @Test
    fun otherResetsWhiteRun() {
        val tally = ColumnTally()
        repeat(10) { tally.white(it) }
        tally.other(10)
        repeat(6) { tally.white(11 + it) }
        tally.finish()
        tally.hasWhiteStreak shouldBe false
        tally.topWhiteRun shouldBe 10
        tally.bottomWhiteRun.shouldBeNull()
    }

    @Test
    fun constantsMatchTheHeuristic() {
        ColumnTally.WHITE_STREAK_MIN shouldBe 15
        ColumnTally.BLACK_STREAK_MIN shouldBe 14
        ColumnTally.SHORT_RUN_MIN shouldBe 7
    }
}
