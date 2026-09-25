package exh.search

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test

internal class TextTest {
    private fun text(vararg parts: TextComponent) = Text().apply { components += parts }

    @Test
    fun asQueryEscapesAndCaches() {
        val t = text(StringTextComponent("a_b%c\\"), SingleWildcard("?"), MultiWildcard("*"))
        t.asQuery() shouldBe """a\_b\%c\\_%"""
        t.asQuery() shouldBe """a\_b\%c\\_%"""
    }

    @Test
    fun lenientTitleQueryPercents() {
        val t = text(StringTextComponent("foo"))
        t.asLenientTitleQuery() shouldBe "%foo%"
        t.asLenientTitleQuery() shouldBe "%foo%"
    }

    @Test
    fun lenientTagQueriesFourForms() {
        val t = text(StringTextComponent("foo"))
        val expected = listOf("foo%", " foo ", " foo", "foo ")
        t.asLenientTagQueries() shouldBe expected
        t.asLenientTagQueries() shouldBe expected
    }

    @Test
    fun rawTextIgnoresUnknownParts() {
        val t = text(StringTextComponent("a"), TextComponent("?"), MultiWildcard("*"))
        t.rBaseBuilder().toString() shouldBe "a%"
        t.rawTextOnly() shouldBe "a?*"
        t.rawTextOnly() shouldBe "a?*"
        t.rawTextEscapedForLike() shouldBe "a?*"
    }

    @Test
    fun escapedRawTextEscapesLikeChars() {
        text(StringTextComponent("50%_")).rawTextEscapedForLike() shouldBe """50\%\_"""
    }

    @Test
    fun componentsExposeRawText() {
        val single = SingleWildcard("_")
        single.rawText shouldBe "_"
        val str = StringTextComponent("v")
        str.value shouldBeSameInstanceAs str.rawText
        val ns = Namespace("n", null)
        ns.namespace shouldBe "n"
        ns.tag = Text()
        ns.tag!!.components.size shouldBe 0
    }
}
