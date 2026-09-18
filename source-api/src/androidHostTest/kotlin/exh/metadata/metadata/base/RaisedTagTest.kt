package exh.metadata.metadata.base

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class RaisedTagTest {
    private val json = Json
    private val tag = RaisedTag(namespace = "ns", name = "n", type = 1)

    @Test
    fun equalsItselfAndEqualCopies() {
        val same = tag
        (tag == same) shouldBe true
        (tag == RaisedTag(namespace = "ns", name = "n", type = 1)) shouldBe true
        tag.hashCode() shouldBe RaisedTag(namespace = "ns", name = "n", type = 1).hashCode()
    }

    @Test
    fun notEqualToOtherTypes() {
        tag.equals("ns") shouldBe false
    }

    @Test
    fun notEqualWhenAFieldDiffers() {
        (tag == tag.copy(namespace = "other")) shouldBe false
        (tag == tag.copy(name = "other")) shouldBe false
        (tag == tag.copy(type = 2)) shouldBe false
    }

    @Test
    fun hashCodeHandlesNullNamespace() {
        val bare = tag.copy(namespace = null)
        bare.hashCode() shouldNotBe tag.hashCode()
        bare.hashCode() shouldBe bare.copy().hashCode()
    }

    @Test
    fun toStringListsFields() {
        tag.toString() shouldBe "RaisedTag(namespace=ns, name=n, type=1)"
    }

    @Test
    fun copyReplacesOnlyGivenFields() {
        tag.copy() shouldBe tag
        tag.copy(namespace = "x").namespace shouldBe "x"
        tag.copy(name = "y").name shouldBe "y"
        tag.copy(type = 3).type shouldBe 3
        tag.copy(namespace = null, name = "z", type = 4) shouldBe RaisedTag(namespace = null, name = "z", type = 4)
    }

    @Test
    fun componentsMatchFields() {
        val (namespace, name, type) = tag
        namespace shouldBe "ns"
        name shouldBe "n"
        type shouldBe 1
    }

    @Test
    fun jsonRoundTripsNullNamespace() {
        val encoded = json.encodeToString(RaisedTag.serializer(), tag)
        encoded shouldBe """{"namespace":"ns","name":"n","type":1}"""
        json.decodeFromString(RaisedTag.serializer(), encoded) shouldBe tag
        val bare = tag.copy(namespace = null)
        val bareEncoded = json.encodeToString(RaisedTag.serializer(), bare)
        bareEncoded shouldBe """{"namespace":null,"name":"n","type":1}"""
        json.decodeFromString(RaisedTag.serializer(), bareEncoded) shouldBe bare
    }

    @Test
    fun jsonRejectsMissingFields() {
        shouldThrow<SerializationException> { json.decodeFromString(RaisedTag.serializer(), "{}") }
    }
}
