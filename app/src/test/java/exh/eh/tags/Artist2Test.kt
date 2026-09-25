package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class Artist2Test {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Artist2.getTags1()
        tags.size shouldBe 2000
        tags.first() shouldBe "artist:mame denkyuu"
        tags.last() shouldBe "artist:oroz-kun"
    }

    @Test
    fun getTags2SizeFirstLast() {
        val tags = Artist2.getTags2()
        tags.size shouldBe 2000
        tags.first() shouldBe "artist:orphen"
        tags.last() shouldBe "artist:stlemo"
    }

    @Test
    fun getTags3SizeFirstLast() {
        val tags = Artist2.getTags3()
        tags.size shouldBe 2000
        tags.first() shouldBe "artist:stogiegoatarts"
        tags.last() shouldBe "artist:yuuki shin"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Artist2.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 6000
    }
}
