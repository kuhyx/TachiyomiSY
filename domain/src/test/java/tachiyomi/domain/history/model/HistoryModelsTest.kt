package tachiyomi.domain.history.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.util.Date

internal class HistoryModelsTest {

    @Test
    fun createIsBlankRow() {
        val history = History.create()

        history.id shouldBe -1L
        history.chapterId shouldBe -1L
        history.readAt shouldBe null
        history.readDuration shouldBe -1L
    }

    @Test
    fun historyDataClassSurface() {
        val readAt = Date(1_000L)
        val history = History(id = 1L, chapterId = 2L, readAt = readAt, readDuration = 3L)

        history.component1() shouldBe 1L
        history.component2() shouldBe 2L
        history.component3() shouldBe readAt
        history.component4() shouldBe 3L
        history.copy(readDuration = 4L) shouldBe History(id = 1L, chapterId = 2L, readAt = readAt, readDuration = 4L)
        history shouldBe history.copy()
        history shouldNotBe History.create()
        history.hashCode() shouldBe history.copy().hashCode()
        history.toString() shouldBe "History(id=1, chapterId=2, readAt=$readAt, readDuration=3)"
    }

    @Test
    fun historyUpdateSurface() {
        val readAt = Date(5_000L)
        val update = HistoryUpdate(chapterId = 7L, readAt = readAt, sessionReadDuration = 9L)

        update.component1() shouldBe 7L
        update.component2() shouldBe readAt
        update.component3() shouldBe 9L
        update shouldBe update.copy()
        update.copy(sessionReadDuration = 1L) shouldNotBe update
        update.hashCode() shouldBe update.copy().hashCode()
        update.toString() shouldBe "HistoryUpdate(chapterId=7, readAt=$readAt, sessionReadDuration=9)"
    }

    @Test
    fun toHistoryUpdateKeepsReadAt() {
        val readAt = Date(42_000L)
        val history = History(id = 1L, chapterId = 2L, readAt = readAt, readDuration = 3L)

        history.toHistoryUpdate() shouldBe HistoryUpdate(chapterId = 2L, readAt = readAt, sessionReadDuration = 3L)
    }

    @Test
    fun toHistoryUpdateNullIsEpoch() {
        val history = History(id = 1L, chapterId = 2L, readAt = null, readDuration = 3L)

        history.toHistoryUpdate() shouldBe HistoryUpdate(chapterId = 2L, readAt = Date(0), sessionReadDuration = 3L)
    }
}
