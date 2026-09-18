package tachiyomi.data

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class UpdatesFilterTest {
    private val filter = UpdatesFilter(
        after = 1L,
        limit = 2L,
        read = true,
        started = 1L,
        bookmarked = false,
        hideExcludedScanlators = 1L,
    )

    @Test
    fun exposesEveryField() {
        filter.after shouldBe 1L
        filter.limit shouldBe 2L
        filter.read shouldBe true
        filter.started shouldBe 1L
        filter.bookmarked shouldBe false
        filter.hideExcludedScanlators shouldBe 1L
    }

    @Test
    fun equalsComparesEveryField() {
        filter shouldBe filter
        filter shouldBe filter.copy()
        filter shouldNotBe filter.copy(after = 9L)
        filter shouldNotBe filter.copy(limit = 9L)
        filter shouldNotBe filter.copy(read = null)
        filter shouldNotBe filter.copy(started = null)
        filter shouldNotBe filter.copy(bookmarked = null)
        filter shouldNotBe filter.copy(hideExcludedScanlators = 0L)
        val other: Any = "filter"
        (filter == other) shouldBe false
    }

    @Test
    fun hashCodeFollowsEquals() {
        filter.hashCode() shouldBe filter.copy().hashCode()
        val nulls = filter.copy(read = null, started = null, bookmarked = null)
        nulls.hashCode() shouldBe nulls.copy().hashCode()
        nulls.hashCode() shouldNotBe filter.hashCode()
    }

    @Test
    fun toStringNamesFields() {
        filter.toString() shouldBe
            "UpdatesFilter(after=1, limit=2, read=true, started=1, bookmarked=false, hideExcludedScanlators=1)"
    }
}
