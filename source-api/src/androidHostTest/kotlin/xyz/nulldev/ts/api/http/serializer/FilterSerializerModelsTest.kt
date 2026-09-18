package xyz.nulldev.ts.api.http.serializer

import eu.kanade.tachiyomi.source.model.Filter
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class FilterSerializerModelsTest {
    private val serializer = FilterSerializer()

    // Overrides nothing but the abstract members, so every interface default runs.
    private val plain = object : Serializer<Filter.Header> {
        override val serializer: FilterSerializer = FilterSerializer()
        override val type: String = "PLAIN"
        override val clazz: KClass<in Filter.Header> = Filter.Header::class
    }

    @Test
    fun headerJson() {
        val json = serializer.serialize(Filter.Header("Head").erased())
        json shouldBe mappedJson("HEADER", Triple("name", "Head", JAVA_STRING))
    }

    @Test
    fun separatorJson() {
        val json = serializer.serialize(Filter.Separator("Sep").erased())
        json shouldBe mappedJson("SEPARATOR", Triple("name", "Sep", JAVA_STRING))
    }

    @Test
    fun selectJson() {
        val json = serializer.serialize(FixtureSelect("Sel", arrayOf(10, 20), 1).erased())
        val expected = mappedJson("SELECT", Triple("name", "Sel", JAVA_STRING), Triple("state", "1", JAVA_INTEGER))
            .withField(
                SelectSerializer.VALUES,
                buildJsonArray {
                    add("10")
                    add("20")
                },
            )
        json shouldBe expected
    }

    @Test
    fun textJson() {
        val json = serializer.serialize(FixtureText("Title", "abc").erased())
        json shouldBe mappedJson("TEXT", Triple("name", "Title", JAVA_STRING), Triple("state", "abc", JAVA_STRING))
    }

    @Test
    fun checkBoxJson() {
        val json = serializer.serialize(FixtureCheckBox("Box", true).erased())
        json shouldBe mappedJson("CHECKBOX", Triple("name", "Box", JAVA_STRING), Triple("state", "true", JAVA_BOOLEAN))
    }

    @Test
    fun triStateJson() {
        val json = serializer.serialize(FixtureTriState("Tri", Filter.TriState.STATE_EXCLUDE).erased())
        json shouldBe mappedJson("TRISTATE", Triple("name", "Tri", JAVA_STRING), Triple("state", "2", JAVA_INTEGER))
    }

    @Test
    fun selectRoundTrip() {
        val target = DefaultSelect("Sel", arrayOf("a", "b"))
        val source = FixtureSelect("Sel", arrayOf("a", "b"), 1)
        serializer.deserialize(target.erased(), serializer.serialize(source.erased()))
        target.state shouldBe 1
    }

    @Test
    fun textRoundTrip() {
        val target = DefaultText("Title")
        serializer.deserialize(target.erased(), serializer.serialize(FixtureText("Title", "typed").erased()))
        target.state shouldBe "typed"
    }

    @Test
    fun toggleRoundTrips() {
        val box = DefaultCheckBox("Box")
        serializer.deserialize(box.erased(), serializer.serialize(FixtureCheckBox("Box", true).erased()))
        box.state shouldBe true

        val tri = DefaultTriState("Tri")
        val included = FixtureTriState("Tri", Filter.TriState.STATE_INCLUDE)
        serializer.deserialize(tri.erased(), serializer.serialize(included.erased()))
        tri.state shouldBe Filter.TriState.STATE_INCLUDE
    }

    @Test
    fun interfaceDefaultsAreNoOps() {
        val header = Filter.Header("Head")
        val written = buildJsonObject { with(plain) { serialize(header) } }
        written shouldBe JsonObject(emptyMap())
        plain.deserialize(JsonObject(emptyMap()), header)
        plain.mappings().shouldBeEmpty()
        header.name shouldBe "Head"
        header.state shouldBe 0
    }
}
