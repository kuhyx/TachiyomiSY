package exh.util

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

internal class ExhUtilTest {
    @Test
    fun nullIfEmptyOnCollections() {
        emptyList<Int>().nullIfEmpty().shouldBeNull()
        listOf(1).nullIfEmpty() shouldBe listOf(1)
        setOf("a").nullIfEmpty() shouldBe setOf("a")
    }

    @Test
    fun stringBuilderPlusAssign() {
        val builder = StringBuilder("a")
        builder += "b"
        builder.toString() shouldBe "ab"
    }

    @Test
    fun trimAllDropBlankDropEmpty() {
        val strings = listOf(" a ", "", "  ", "b")
        strings.trimAll() shouldBe listOf("a", "", "", "b")
        strings.dropBlank() shouldBe listOf(" a ", "b")
        strings.dropEmpty() shouldBe listOf(" a ", "  ", "b")
    }

    @Test
    fun removeArticlesStripsLeading() {
        "The Cat".removeArticles() shouldBe "Cat"
        "an apple".removeArticles() shouldBe "apple"
        "A Tale".removeArticles() shouldBe "Tale"
        "Another".removeArticles() shouldBe "Another"
        "Cat the".removeArticles() shouldBe "Cat the"
    }

    @Test
    fun trimOrNullAndNullIfBlank() {
        " a ".trimOrNull() shouldBe "a"
        "  ".trimOrNull().shouldBeNull()
        "".nullIfBlank().shouldBeNull()
        "x".nullIfBlank() shouldBe "x"
    }

    @Test
    fun capitalizeFirstCharacter() {
        "abc".capitalize() shouldBe "Abc"
        "Abc".capitalize() shouldBe "Abc"
        "".capitalize() shouldBe ""
        "1a".capitalize() shouldBe "1a"
        "istanbul".capitalize(Locale.forLanguageTag("tr")) shouldBe "İstanbul"
        "istanbul".capitalize(Locale.ENGLISH) shouldBe "Istanbul"
    }
}
