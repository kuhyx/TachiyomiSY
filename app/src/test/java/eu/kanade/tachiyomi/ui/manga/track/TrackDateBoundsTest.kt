package eu.kanade.tachiyomi.ui.manga.track

import androidx.compose.material3.SelectableDates
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.domainTrack
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** Which days and years the date picker offers: none in the future, none past the other end of the reading span. */
internal class TrackDateBoundsTest {
    private val day = 86_400_000L
    private val base = domainTrack(trackerId = 1L)
    private val nextYear = LocalDate.now(ZoneOffset.UTC).year + 1

    // The screen hands its bounds to the picker, which only asks them while it draws; ask them directly.
    private fun bounds(screen: TrackDateSelectorScreen): SelectableDates {
        val field = TrackDateSelectorScreen::class.java.getDeclaredField("selectableDates")
        field.isAccessible = true
        return field.get(screen) as SelectableDates
    }

    @Test
    fun startStaysBeforeFinish() {
        val dates = bounds(TrackDateSelectorScreen(base.copy(finishDate = 10 * day), 1L, start = true))
        dates.isSelectableDate(5 * day) shouldBe true
        dates.isSelectableDate(20 * day) shouldBe false
        dates.isSelectableYear(1970) shouldBe true
        dates.isSelectableYear(1971) shouldBe false
    }

    @Test
    fun finishStaysAfterStart() {
        val dates = bounds(TrackDateSelectorScreen(base.copy(startDate = 10 * day), 1L, start = false))
        dates.isSelectableDate(5 * day) shouldBe false
        dates.isSelectableDate(20 * day) shouldBe true
        dates.isSelectableYear(1969) shouldBe false
        dates.isSelectableYear(1970) shouldBe true
    }

    @Test
    fun openEndsAllowThePast() {
        val dates = bounds(TrackDateSelectorScreen(base, 1L, start = true))
        dates.isSelectableDate(5 * day) shouldBe true
        dates.isSelectableYear(1999) shouldBe true
        dates.isSelectableYear(nextYear) shouldBe false
        dates.isSelectableDate(System.currentTimeMillis() + 400 * day) shouldBe false
    }

    @Test
    fun itemOpensItsSelector() {
        val tracker = mockk<Tracker> { every { id } returns 4L }
        val screen = dateSelector(TrackItem(base, tracker), start = false)
        bounds(screen).isSelectableYear(nextYear) shouldBe false
    }
}
