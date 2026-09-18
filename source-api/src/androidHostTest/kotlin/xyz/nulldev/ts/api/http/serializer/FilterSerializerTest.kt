package xyz.nulldev.ts.api.http.serializer

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test

internal class FilterSerializerTest {
    private val serializer = FilterSerializer()

    private fun source(): FilterList = FilterList(
        FixtureText("T", "hello"),
        FixtureCheckBox("C", true),
        FixtureTriState("S", Filter.TriState.STATE_EXCLUDE),
    )

    private fun target(): FilterList = FilterList(DefaultText("T"), DefaultCheckBox("C"), DefaultTriState("S"))

    @Test
    fun serializesEveryFilterInOrder() {
        val filters = FilterList(Filter.Header("H"), FixtureText("T", "x"), FixtureCheckBox("C", true))
        val json = serializer.serialize(filters)
        json.size shouldBe 3
        val types = json.map { it.jsonObject.getValue(FilterSerializer.TYPE).jsonPrimitive.content }
        types shouldContainExactly listOf("HEADER", "TEXT", "CHECKBOX")
    }

    @Test
    fun emptyListGivesEmptyArray() {
        serializer.serialize(FilterList()) shouldBe JsonArray(emptyList())
    }

    @Test
    fun listRoundTrip() {
        val target = target()
        serializer.deserialize(target, serializer.serialize(source()))
        target.list shouldContainExactly source().list
    }

    @Test
    fun shorterJsonLeavesTailUntouched() {
        val target = target()
        val json = JsonArray(serializer.serialize(source()).take(2))
        serializer.deserialize(target, json)
        val expected = listOf(FixtureText("T", "hello"), FixtureCheckBox("C", true), DefaultTriState("S"))
        target.list shouldContainExactly expected
    }

    @Test
    fun longerJsonIgnoresExtraEntries() {
        val target = FilterList(DefaultText("T"))
        serializer.deserialize(target, serializer.serialize(source()))
        target.list shouldContainExactly listOf(FixtureText("T", "hello"))
    }

    @Test
    fun emptyJsonChangesNothing() {
        val target = target()
        serializer.deserialize(target, JsonArray(emptyList()))
        target.list shouldContainExactly target().list
    }

    @Test
    fun writesTypeAndClassMappings() {
        val json = serializer.serialize(FixtureText("Title", "abc").erased())
        val expectedKeys = listOf("name", "state", FilterSerializer.CLASS_MAPPINGS, FilterSerializer.TYPE)
        json.keys.toList() shouldContainExactly expectedKeys
        json.getValue(FilterSerializer.TYPE).jsonPrimitive.content shouldBe "TEXT"
        val classes = json.getValue(FilterSerializer.CLASS_MAPPINGS).jsonObject
        classes.getValue("name").jsonPrimitive.content shouldBe JAVA_STRING
        classes.getValue("state").jsonPrimitive.content shouldBe JAVA_STRING
    }

    @Test
    fun readOnlyMappingIsSkipped() {
        val text = FixtureText("Original", "old")
        val json = buildJsonObject {
            put("name", "Renamed")
            put("state", "new")
            putJsonObject(FilterSerializer.CLASS_MAPPINGS) {
                put("name", JAVA_STRING)
                put("state", JAVA_STRING)
            }
            put(FilterSerializer.TYPE, "TEXT")
        }
        serializer.deserialize(text.erased(), json)
        text.name shouldBe "Original"
        text.state shouldBe "new"
    }

    @Test
    fun nullStateRoundTrip() {
        val source = FixtureText("T", "x").erased()
        source.state = null
        val json = serializer.serialize(source)
        json.getValue("state").jsonPrimitive.content shouldBe "null"
        val classes = json.getValue(FilterSerializer.CLASS_MAPPINGS).jsonObject
        classes.getValue("state").jsonPrimitive.content shouldBe "null"

        val target = FixtureText("T", "y").erased()
        serializer.deserialize(target, json)
        target.state shouldBe null
    }

    @Test
    fun singleFilterMatchesListEntry() {
        val text = FixtureText("T", "x")
        serializer.serialize(FilterList(text)).single() shouldBe serializer.serialize(text.erased())
    }
}
