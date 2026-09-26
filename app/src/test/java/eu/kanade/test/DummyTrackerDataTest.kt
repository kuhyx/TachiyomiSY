package eu.kanade.test

import eu.kanade.tachiyomi.data.track.model.TrackSearch
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test

internal class DummyTrackerDataTest {
    private val base = DummyTracker(id = 1, name = "Dummy")

    @Test
    fun equalCopiesAreEqual() {
        base shouldBe base
        base shouldBe base.copy()
        base.hashCode() shouldBe base.copy().hashCode()
        base shouldNotBe Any()
        base.toString() shouldContain "Dummy"
    }

    @Test
    fun everyFieldTakesPart() {
        val flow = flowOf(true)
        val variants = listOf(
            base.copy(id = 2),
            base.copy(name = "Other"),
            base.copy(supportsReadingDates = true),
            base.copy(supportsPrivateTracking = true),
            base.copy(isLoggedIn = true),
            base.copy(isLoggedInFlow = flow),
            base.copy(valLogo = 0),
            base.copy(valStatuses = emptyList()),
            base.copy(valReadingStatus = 9),
            base.copy(valRereadingStatus = 9),
            base.copy(valCompletionStatus = 9),
            base.copy(valScoreList = emptyList()),
            base.copy(val10PointScore = 1.0),
            base.copy(valSearchResults = listOf(TrackSearch.create(1))),
        )
        variants.forEach { it shouldNotBe base }
    }

    @Test
    fun componentsMatchFields() {
        val (id, name, readingDates) = base
        id shouldBe 1L
        name shouldBe "Dummy"
        readingDates shouldBe false
        base.component4() shouldBe false
        base.component5() shouldBe false
        base.component7() shouldBe base.valLogo
        base.component8() shouldBe base.valStatuses
        base.component9() shouldBe 1L
        base.component10() shouldBe 1L
    }

    @Test
    fun laterComponentsMatchFields() {
        base.component6() shouldBe base.isLoggedInFlow
        base.component11() shouldBe 2L
        base.component12() shouldBe base.valScoreList
        base.component13() shouldBe 5.4
        base.component14() shouldBe emptyList()
    }
}
