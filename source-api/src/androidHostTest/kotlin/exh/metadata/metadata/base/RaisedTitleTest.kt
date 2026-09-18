package exh.metadata.metadata.base

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class RaisedTitleTest {
    private val json = Json
    private val title = RaisedTitle(title = "t", type = 2)

    @Test
    fun typeDefaultsToZero() {
        RaisedTitle("only").type shouldBe 0
        RaisedTitle("only").title shouldBe "only"
    }

    @Test
    fun equalsItselfAndEqualCopies() {
        val same = title
        (title == same) shouldBe true
        (title == RaisedTitle(title = "t", type = 2)) shouldBe true
        title.hashCode() shouldBe RaisedTitle(title = "t", type = 2).hashCode()
    }

    @Test
    fun notEqualToOtherTypes() {
        title.equals("t") shouldBe false
    }

    @Test
    fun notEqualWhenAFieldDiffers() {
        (title == title.copy(title = "other")) shouldBe false
        (title == title.copy(type = 9)) shouldBe false
    }

    @Test
    fun toStringListsFields() {
        title.toString() shouldBe "RaisedTitle(title=t, type=2)"
    }

    @Test
    fun copyReplacesOnlyGivenFields() {
        title.copy() shouldBe title
        title.copy(title = "x").title shouldBe "x"
        title.copy(type = 5).type shouldBe 5
        title.copy(title = "y", type = 6) shouldBe RaisedTitle(title = "y", type = 6)
    }

    @Test
    fun componentsMatchFields() {
        val (text, type) = title
        text shouldBe "t"
        type shouldBe 2
    }

    @Test
    fun jsonRoundTripsFull() {
        val encoded = json.encodeToString(RaisedTitle.serializer(), title)
        encoded shouldBe """{"title":"t","type":2}"""
        json.decodeFromString(RaisedTitle.serializer(), encoded) shouldBe title
    }

    @Test
    fun jsonOmitsDefaultType() {
        val plain = RaisedTitle("p")
        json.encodeToString(RaisedTitle.serializer(), plain) shouldBe """{"title":"p"}"""
        json.decodeFromString(RaisedTitle.serializer(), """{"title":"p"}""") shouldBe plain
        json.decodeFromString(RaisedTitle.serializer(), """{"title":"p","type":0}""") shouldBe plain
    }

    @Test
    fun jsonRejectsMissingTitle() {
        shouldThrow<SerializationException> { json.decodeFromString(RaisedTitle.serializer(), """{"type":1}""") }
    }
}
