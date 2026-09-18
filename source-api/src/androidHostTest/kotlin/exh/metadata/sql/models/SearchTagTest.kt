package exh.metadata.sql.models

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class SearchTagTest {
    private val json = Json
    private val tag = SearchTag(id = 3, mangaId = 1, namespace = "ns", name = "n", type = 2)

    @Test
    fun equalsItselfAndEqualCopies() {
        val same = tag
        (tag == same) shouldBe true
        (tag == tag.copy()) shouldBe true
        tag.hashCode() shouldBe tag.copy().hashCode()
    }

    @Test
    fun notEqualToOtherTypes() {
        tag.equals("tag") shouldBe false
    }

    @Test
    fun notEqualWhenAFieldDiffers() {
        (tag == tag.copy(id = null)) shouldBe false
        (tag == tag.copy(mangaId = 2)) shouldBe false
        (tag == tag.copy(namespace = null)) shouldBe false
        (tag == tag.copy(name = "other")) shouldBe false
        (tag == tag.copy(type = 5)) shouldBe false
    }

    @Test
    fun hashCodeHandlesNullFields() {
        val bare = tag.copy(id = null, namespace = null)
        bare.hashCode() shouldNotBe tag.hashCode()
        bare.hashCode() shouldBe bare.copy().hashCode()
    }

    @Test
    fun toStringListsFields() {
        tag.toString() shouldBe "SearchTag(id=3, mangaId=1, namespace=ns, name=n, type=2)"
    }

    @Test
    fun copyReplacesOnlyGivenFields() {
        tag.copy(id = 8).id shouldBe 8
        tag.copy(mangaId = 7).mangaId shouldBe 7
        tag.copy(namespace = "x").namespace shouldBe "x"
        tag.copy(name = "y").name shouldBe "y"
        tag.copy(type = 9).type shouldBe 9
    }

    @Test
    fun componentsMatchFields() {
        val components = listOf(
            tag.component1(), tag.component2(), tag.component3(), tag.component4(), tag.component5(),
        )
        components shouldBe listOf(3L, 1L, "ns", "n", 2)
    }

    @Test
    fun jsonRoundTripsFullAndNull() {
        val encoded = json.encodeToString(SearchTag.serializer(), tag)
        encoded shouldBe """{"id":3,"mangaId":1,"namespace":"ns","name":"n","type":2}"""
        json.decodeFromString(SearchTag.serializer(), encoded) shouldBe tag
        val bare = tag.copy(id = null, namespace = null)
        val bareEncoded = json.encodeToString(SearchTag.serializer(), bare)
        bareEncoded shouldBe """{"id":null,"mangaId":1,"namespace":null,"name":"n","type":2}"""
        json.decodeFromString(SearchTag.serializer(), bareEncoded) shouldBe bare
    }

    @Test
    fun jsonRejectsEmptyObject() {
        shouldThrow<SerializationException> { json.decodeFromString(SearchTag.serializer(), "{}") }
    }
}
