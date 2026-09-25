package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MixedTest {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Mixed.getTags1()
        tags.size shouldBe 22
        tags.first() shouldBe "mixed:animal on animal"
        tags.last() shouldBe "mixed:twins"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Mixed.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 22
    }
}
