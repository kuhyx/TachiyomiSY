package xyz.nulldev.ts.api.http.serializer

import eu.kanade.tachiyomi.source.model.Filter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test

internal class FilterSerializerCompoundTest {
    private val serializer = FilterSerializer()

    private val sortValues = arrayOf("Popular", "Latest")

    private val sortJson = mappedJson("SORT", Triple("name", "S", JAVA_STRING))
        .withField(
            SortSerializer.VALUES,
            buildJsonArray {
                add("Popular")
                add("Latest")
            },
        )

    @Test
    fun groupSerializesNestedFilters() {
        val group = FixtureGroup("G", listOf(FixtureCheckBox("A", true), "not a filter"))
        val json = serializer.serialize(group.erased())
        val state = json.getValue(GroupSerializer.STATE).jsonArray
        state[0] shouldBe serializer.serialize(FixtureCheckBox("A", true).erased())
        state[1] shouldBe JsonNull
        json.getValue(FilterSerializer.TYPE).jsonPrimitive.content shouldBe "GROUP"
        json.getValue(GroupSerializer.NAME).jsonPrimitive.content shouldBe "G"
    }

    @Test
    fun groupDeserializesSkippingNulls() {
        val source = FixtureGroup("G", listOf(FixtureCheckBox("A", true), "plain", FixtureText("B", "z")))
        val target = FixtureGroup("G", listOf(DefaultCheckBox("A"), "plain", DefaultText("B")))
        serializer.deserialize(target.erased(), serializer.serialize(source.erased()))
        target.state shouldContainExactly listOf(FixtureCheckBox("A", true), "plain", FixtureText("B", "z"))
    }

    @Test
    fun sortWithoutSelectionWritesNull() {
        val json = serializer.serialize(DefaultSort("S", sortValues).erased())
        json shouldBe sortJson.withField(SortSerializer.STATE, JsonNull)
    }

    @Test
    fun sortWithSelectionWritesObject() {
        val json = serializer.serialize(FixtureSort("S", sortValues, Filter.Sort.Selection(1, false)).erased())
        val state = buildJsonObject {
            put(SortSerializer.STATE_INDEX, 1)
            put(SortSerializer.STATE_ASCENDING, false)
        }
        json shouldBe sortJson.withField(SortSerializer.STATE, state)
    }

    @Test
    fun sortRoundTrip() {
        val target = DefaultSort("S", sortValues)
        val source = FixtureSort("S", sortValues, Filter.Sort.Selection(1, true))
        serializer.deserialize(target.erased(), serializer.serialize(source.erased()))
        target.state shouldBe Filter.Sort.Selection(1, true)
    }

    @Test
    fun sortNullStateClearsSelection() {
        val target = FixtureSort("S", sortValues, Filter.Sort.Selection(0, true))
        serializer.deserialize(target.erased(), sortJson.withField(SortSerializer.STATE, JsonNull))
        target.state shouldBe null

        target.state = Filter.Sort.Selection(1, false)
        serializer.deserialize(target.erased(), sortJson)
        target.state shouldBe null
    }

    private fun deserializeSortState(state: JsonObject) {
        serializer.deserialize(DefaultSort("S", sortValues).erased(), sortJson.withField(SortSerializer.STATE, state))
    }

    @Test
    fun sortMissingIndexThrowsNpe() {
        val state = buildJsonObject { put(SortSerializer.STATE_ASCENDING, true) }
        shouldThrow<NullPointerException> { deserializeSortState(state) }
    }

    @Test
    fun sortMissingAscendingThrowsNpe() {
        val state = buildJsonObject { put(SortSerializer.STATE_INDEX, 0) }
        shouldThrow<NullPointerException> { deserializeSortState(state) }
    }

    @Test
    fun autoCompleteJson() {
        val filter = DefaultAutoComplete(name = "AC", hint = "h", values = listOf("a", "b"), state = listOf("b"))
        val json = serializer.serialize(filter.erased())
        val expected = mappedJson("AUTOCOMPLETE", Triple("name", "AC", JAVA_STRING))
            .withField(AutoCompleteSerializer.STATE, buildJsonArray { add("b") })
        json shouldBe expected
    }

    @Test
    fun autoCompleteRoundTrip() {
        val target = DefaultAutoComplete(name = "AC", hint = "h", values = listOf("a", "b"), state = emptyList())
        val json = buildJsonObject {
            put(FilterSerializer.TYPE, "AUTOCOMPLETE")
            putJsonObject(FilterSerializer.CLASS_MAPPINGS) { put("name", JAVA_STRING) }
            put("name", "AC")
            put(
                AutoCompleteSerializer.STATE,
                buildJsonArray {
                    add("a")
                    add("-b")
                },
            )
        }
        serializer.deserialize(target.erased(), json)
        target.state shouldContainExactly listOf("a", "-b")
    }
}
