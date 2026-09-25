package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class Artist3Test {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Artist3.getTags1()
        tags.size shouldBe 146
        tags.first() shouldBe "artist:yuuki sonisuke"
        tags.last() shouldBe "artist:zyd"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Artist3.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 146
    }
}
