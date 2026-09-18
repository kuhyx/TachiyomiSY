package xyz.nulldev.ts.api.http.serializer

import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test

internal class FilterSerializerErrorsTest {
    private val serializer = FilterSerializer()

    private fun deserializeText(json: JsonObject) {
        serializer.deserialize(FixtureText("T", "x").erased(), json)
    }

    @Test
    fun missingTypeThrowsNpe() {
        shouldThrow<NullPointerException> { deserializeText(JsonObject(emptyMap())) }
    }

    @Test
    fun unknownTypeIsRejected() {
        val json = buildJsonObject { put(FilterSerializer.TYPE, "NOPE") }
        val error = shouldThrow<IllegalArgumentException> { deserializeText(json) }
        error.message shouldBe "Cannot deserialize this type!"
    }

    @Test
    fun missingClassMappingsThrowsNpe() {
        val json = buildJsonObject {
            put(FilterSerializer.TYPE, "TEXT")
            put(TextSerializer.STATE, "x")
        }
        shouldThrow<NullPointerException> { deserializeText(json) }
    }

    @Test
    fun missingMappingEntryThrowsNpe() {
        val json = buildJsonObject {
            put(FilterSerializer.TYPE, "TEXT")
            put(TextSerializer.STATE, "x")
            put(FilterSerializer.CLASS_MAPPINGS, JsonObject(emptyMap()))
        }
        shouldThrow<NullPointerException> { deserializeText(json) }
    }

    @Test
    fun missingValueThrowsNpe() {
        val json = buildJsonObject {
            put(FilterSerializer.TYPE, "TEXT")
            putJsonObject(FilterSerializer.CLASS_MAPPINGS) { put(TextSerializer.STATE, JAVA_STRING) }
        }
        shouldThrow<NullPointerException> { deserializeText(json) }
    }

    @Test
    fun unknownClassNameIsRejected() {
        val json = buildJsonObject {
            put(FilterSerializer.TYPE, "TEXT")
            put(TextSerializer.STATE, "x")
            putJsonObject(FilterSerializer.CLASS_MAPPINGS) { put(TextSerializer.STATE, "java.util.Date") }
        }
        val error = shouldThrow<IllegalArgumentException> { deserializeText(json) }
        error.message shouldBe "Cannot deserialize this type!"
    }

    @Test
    fun nonObjectListEntryIsRejected() {
        val filters = FilterList(FixtureText("T", "x"))
        shouldThrow<IllegalArgumentException> {
            serializer.deserialize(filters, JsonArray(listOf(JsonPrimitive("not an object"))))
        }
    }

    @Test
    fun autoCompleteNoStateThrowsNpe() {
        val filter = DefaultAutoComplete(name = "AC", hint = "h", values = emptyList(), state = emptyList())
        val json = buildJsonObject { put(FilterSerializer.TYPE, "AUTOCOMPLETE") }
        shouldThrow<NullPointerException> { serializer.deserialize(filter.erased(), json) }
    }

    @Test
    fun groupMissingStateThrowsNpe() {
        val json = buildJsonObject { put(FilterSerializer.TYPE, "GROUP") }
        shouldThrow<NullPointerException> { serializer.deserialize(FixtureGroup("G", emptyList()).erased(), json) }
    }
}
