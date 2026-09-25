package exh.search

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

internal class QueryParserTest {
    private fun parse(query: String, wildcard: Boolean = true) = QueryParser(wildcard).parse(query)

    @Test
    fun plainWordsBecomeTexts() {
        val result = parse("Foo  bar")
        result.size shouldBe 2
        val foo = result[0].shouldBeInstanceOf<Text>()
        foo.rawTextOnly() shouldBe "foo"
        foo.excluded.shouldBeFalse()
        foo.exact.shouldBeFalse()
        result[1].shouldBeInstanceOf<Text>().rawTextOnly() shouldBe "bar"
    }

    @Test
    fun emptyQueryYieldsNothing() {
        parse("").shouldBeEmpty()
        parse("   ").shouldBeEmpty()
    }

    @Test
    fun namespaceAliasesResolve() {
        val result = parse("a:foo char:bar unknown:baz")
        result.size shouldBe 3
        val artist = result[0].shouldBeInstanceOf<Namespace>()
        artist.namespace shouldBe "artist"
        artist.tag.shouldNotBeNull().rawTextOnly() shouldBe "foo"
        result[1].shouldBeInstanceOf<Namespace>().namespace shouldBe "character"
        result[2].shouldBeInstanceOf<Namespace>().namespace shouldBe "unknown"
    }

    @Test
    fun namespaceWithoutTagIsEmpty() {
        val result = parse("f:")
        result.size shouldBe 1
        val ns = result[0].shouldBeInstanceOf<Namespace>()
        ns.namespace shouldBe "female"
        ns.tag.shouldNotBeNull().components.shouldBeEmpty()
    }

    @Test
    fun exclusionAndExactMarkers() {
        val result = parse("-foo \$bar a-b")
        result.size shouldBe 3
        result[0].excluded.shouldBeTrue()
        result[1].exact.shouldBeTrue()
        result[1].excluded.shouldBeTrue()
        result[2].shouldBeInstanceOf<Text>().rawTextOnly() shouldBe "a-b"
    }

    @Test
    fun quotesKeepSpacesAndDashes() {
        val result = parse("\"foo -bar\" baz")
        result.size shouldBe 2
        result[0].shouldBeInstanceOf<Text>().rawTextOnly() shouldBe "foo -bar"
        result[0].excluded.shouldBeFalse()
    }

    @Test
    fun dashAfterQuotedSpaceExcludes() {
        val result = parse("\"a \"-b")
        result.size shouldBe 1
        result[0].excluded.shouldBeTrue()
        result[0].shouldBeInstanceOf<Text>().rawTextOnly() shouldBe "a b"
    }

    @Test
    fun wildcardsSplitComponents() {
        val result = parse("f?o_o*b%")
        val text = result.single().shouldBeInstanceOf<Text>()
        text.components.map { it::class.simpleName } shouldBe listOf(
            "StringTextComponent",
            "SingleWildcard",
            "StringTextComponent",
            "SingleWildcard",
            "StringTextComponent",
            "MultiWildcard",
            "StringTextComponent",
            "MultiWildcard",
        )
        text.rawTextOnly() shouldBe "f?o_o*b%"
    }

    @Test
    fun wildcardsDisabledAreLiteral() {
        val result = parse("f?o*", wildcard = false)
        val text = result.single().shouldBeInstanceOf<Text>()
        text.components.single().shouldBeInstanceOf<StringTextComponent>().value shouldBe "f?o*"
    }

    @Test
    fun namespaceWithWildcardTag() {
        val result = parse("g:*")
        val ns = result.single().shouldBeInstanceOf<Namespace>()
        ns.tag.shouldNotBeNull().components.single().shouldBeInstanceOf<MultiWildcard>()
        Namespace("x").tag.shouldBeNull()
    }
}
