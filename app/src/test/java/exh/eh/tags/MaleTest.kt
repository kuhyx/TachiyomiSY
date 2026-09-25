package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MaleTest {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Male.getTags1()
        tags.size shouldBe 561
        tags.first() shouldBe "male:abortion"
        tags.last() shouldBe "male:zombie"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Male.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 561
    }
}
