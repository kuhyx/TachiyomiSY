package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class FemaleTest {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Female.getTags1()
        tags.size shouldBe 600
        tags.first() shouldBe "female:abortion"
        tags.last() shouldBe "female:zombie"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Female.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 600
    }
}
