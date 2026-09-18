package xyz.nulldev.ts.api.http.serializer

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test

/** Every `parsePrimitive` branch, reached by lying about the state's class in `_cmaps`. */
internal class FilterSerializerPrimitiveTest {
    private val serializer = FilterSerializer()

    private fun restore(className: String, value: String): Any? {
        val filter = FixtureText("T", "").erased()
        val json = buildJsonObject {
            put(TextSerializer.STATE, value)
            putJsonObject(FilterSerializer.CLASS_MAPPINGS) { put(TextSerializer.STATE, className) }
            put(FilterSerializer.TYPE, "TEXT")
        }
        serializer.deserialize(filter, json)
        return filter.state
    }

    @Test
    fun restoresInteger() {
        restore(JAVA_INTEGER, "42") shouldBe 42
    }

    @Test
    fun restoresLong() {
        restore("java.lang.Long", "9000000000") shouldBe 9_000_000_000L
    }

    @Test
    fun restoresFloat() {
        restore("java.lang.Float", "1.5") shouldBe 1.5f
    }

    @Test
    fun restoresDouble() {
        restore("java.lang.Double", "2.25") shouldBe 2.25
    }

    @Test
    fun restoresString() {
        restore(JAVA_STRING, "abc") shouldBe "abc"
    }

    @Test
    fun restoresBoolean() {
        restore(JAVA_BOOLEAN, "true") shouldBe true
    }

    @Test
    fun restoresByte() {
        restore("java.lang.Byte", "7") shouldBe 7.toByte()
    }

    @Test
    fun restoresShort() {
        restore("java.lang.Short", "300") shouldBe 300.toShort()
    }

    @Test
    fun restoresCharacter() {
        restore("java.lang.Character", "xy") shouldBe 'x'
    }

    @Test
    fun restoresNull() {
        restore("null", "anything") shouldBe null
    }
}
