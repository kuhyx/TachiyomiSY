package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class GroupTest {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Group.getTags1()
        tags.size shouldBe 2000
        tags.first() shouldBe "group:---"
        tags.last() shouldBe "group:goo-paaa"
    }

    @Test
    fun getTags2SizeFirstLast() {
        val tags = Group.getTags2()
        tags.size shouldBe 2000
        tags.first() shouldBe "group:gorgeous lunch"
        tags.last() shouldBe "group:musashi-dou"
    }

    @Test
    fun getTags3SizeFirstLast() {
        val tags = Group.getTags3()
        tags.size shouldBe 2000
        tags.first() shouldBe "group:musha prune"
        tags.last() shouldBe "group:ten plus aku"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Group.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 6000
    }
}
