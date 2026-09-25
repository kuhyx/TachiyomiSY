package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ParodyTest {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Parody.getTags1()
        tags.size shouldBe 2000
        tags.first() shouldBe "parody:.hack"
        tags.last() shouldBe "parody:renkin san-kyuu magical pokaan"
    }

    @Test
    fun getTags2SizeFirstLast() {
        val tags = Parody.getTags2()
        tags.size shouldBe 891
        tags.first() shouldBe "parody:resident evil"
        tags.last() shouldBe "parody:zyuden sentai kyoryuger"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Parody.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 2891
    }
}
