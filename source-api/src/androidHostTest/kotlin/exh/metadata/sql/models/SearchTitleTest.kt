package exh.metadata.sql.models

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class SearchTitleTest {
    private val json = Json
    private val title = SearchTitle(id = 3, mangaId = 1, title = "t", type = 2)

    @Test
    fun equalsItselfAndEqualCopies() {
        val same = title
        (title == same) shouldBe true
        (title == title.copy()) shouldBe true
        title.hashCode() shouldBe title.copy().hashCode()
    }

    @Test
    fun notEqualToOtherTypes() {
        title.equals("title") shouldBe false
    }

    @Test
    fun notEqualWhenAFieldDiffers() {
        (title == title.copy(id = null)) shouldBe false
        (title == title.copy(mangaId = 2)) shouldBe false
        (title == title.copy(title = "other")) shouldBe false
        (title == title.copy(type = 5)) shouldBe false
    }

    @Test
    fun hashCodeHandlesNullId() {
        val bare = title.copy(id = null)
        bare.hashCode() shouldNotBe title.hashCode()
        bare.hashCode() shouldBe bare.copy().hashCode()
    }

    @Test
    fun toStringListsFields() {
        title.toString() shouldBe "SearchTitle(id=3, mangaId=1, title=t, type=2)"
    }

    @Test
    fun copyReplacesOnlyGivenFields() {
        title.copy(id = 8).id shouldBe 8
        title.copy(mangaId = 7).mangaId shouldBe 7
        title.copy(title = "x").title shouldBe "x"
        title.copy(type = 9).type shouldBe 9
    }

    @Test
    fun componentsMatchFields() {
        val components = listOf(title.component1(), title.component2(), title.component3(), title.component4())
        components shouldBe listOf(3L, 1L, "t", 2)
    }

    @Test
    fun jsonRoundTripsFullAndNullId() {
        val encoded = json.encodeToString(SearchTitle.serializer(), title)
        encoded shouldBe """{"id":3,"mangaId":1,"title":"t","type":2}"""
        json.decodeFromString(SearchTitle.serializer(), encoded) shouldBe title
        val bare = title.copy(id = null)
        val bareEncoded = json.encodeToString(SearchTitle.serializer(), bare)
        bareEncoded shouldBe """{"id":null,"mangaId":1,"title":"t","type":2}"""
        json.decodeFromString(SearchTitle.serializer(), bareEncoded) shouldBe bare
    }

    @Test
    fun jsonRejectsEmptyObject() {
        shouldThrow<SerializationException> { json.decodeFromString(SearchTitle.serializer(), "{}") }
    }
}
