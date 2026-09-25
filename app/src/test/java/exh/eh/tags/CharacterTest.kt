package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class CharacterTest {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Character.getTags1()
        tags.size shouldBe 2000
        tags.first() shouldBe "character:.giffany"
        tags.last() shouldBe "character:kaori kanzaki"
    }

    @Test
    fun getTags2SizeFirstLast() {
        val tags = Character.getTags2()
        tags.size shouldBe 2000
        tags.first() shouldBe "character:kaori misaka"
        tags.last() shouldBe "character:ryouta suzui"
    }

    @Test
    fun getTags3SizeFirstLast() {
        val tags = Character.getTags3()
        tags.size shouldBe 1326
        tags.first() shouldBe "character:ryoutarou tsuboi"
        tags.last() shouldBe "character:zunko tohoku"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Character.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 5326
    }
}
