package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ReclassTest {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Reclass.getTags1()
        tags.size shouldBe 10
        tags.first() shouldBe "reclass:artistcg"
        tags.last() shouldBe "reclass:western"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Reclass.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 10
    }
}
