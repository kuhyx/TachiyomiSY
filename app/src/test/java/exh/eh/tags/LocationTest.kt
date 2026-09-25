package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class LocationTest {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Location.getTags1()
        tags.size shouldBe 7
        tags.first() shouldBe "location:bathing room"
        tags.last() shouldBe "location:sentou"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Location.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 7
    }
}
