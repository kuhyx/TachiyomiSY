package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class OtherTest {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Other.getTags1()
        tags.size shouldBe 59
        tags.first() shouldBe "other:3d"
        tags.last() shouldBe "other:yukkuri"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Other.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 59
    }
}
