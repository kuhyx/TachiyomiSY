package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class Group2Test {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Group2.getTags1()
        tags.size shouldBe 805
        tags.first() shouldBe "group:tenchuugumi"
        tags.last() shouldBe "group:zzz comics"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Group2.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 805
    }
}
