package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ArtistTest {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Artist.getTags1()
        tags.size shouldBe 2000
        tags.first() shouldBe "artist:-helic-"
        tags.last() shouldBe "artist:dd"
    }

    @Test
    fun getTags2SizeFirstLast() {
        val tags = Artist.getTags2()
        tags.size shouldBe 2000
        tags.first() shouldBe "artist:de"
        tags.last() shouldBe "artist:inushiro pochi"
    }

    @Test
    fun getTags3SizeFirstLast() {
        val tags = Artist.getTags3()
        tags.size shouldBe 2000
        tags.first() shouldBe "artist:inuta zetto"
        tags.last() shouldBe "artist:mamabliss"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Artist.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 6000
    }
}
