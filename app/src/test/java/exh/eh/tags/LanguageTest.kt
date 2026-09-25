package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class LanguageTest {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Language.getTags1()
        tags.size shouldBe 43
        tags.first() shouldBe "language:arabic"
        tags.last() shouldBe "language:zulu"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Language.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 43
    }
}
