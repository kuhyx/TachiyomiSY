package mihon.core.common

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY
import mihon.core.common.extensions.JsonObjectEmpty
import mihon.core.common.extensions.JsonObjectEmptyBytes
import mihon.core.common.utils.mutate
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.callStatic
import tachiyomi.core.common.preference.staticMethod

private const val UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

internal class MihonCommonTest {
    @Test
    fun installationIdsAreRandomUuids() {
        val first = FeatureFlags.newInstallationId()
        first shouldMatch UUID_PATTERN
        FeatureFlags.newInstallationId() shouldNotBe first
    }

    @Test
    fun mutateCopiesTheSet() {
        val original = setOf(1, 2)
        val mutated = original.mutate {
            it -= 1
            it += 3
        }
        mutated shouldBe setOf(2, 3)
        original shouldBe setOf(1, 2)
    }

    @Test
    fun emptyJsonObjectConstants() {
        JsonObjectEmpty shouldBe JsonObject(emptyMap())
        Json.encodeToString(JsonObject.serializer(), JsonObjectEmpty) shouldBe "{}"
        JsonObjectEmptyBytes.decodeToString() shouldBe "{}"
        JsonObject.EMPTY shouldBe JsonObjectEmpty
    }

    @Test
    fun emptyGetterNonInlinedCopy() {
        val facade = "mihon.core.common.extensions.JsonObjectKt"
        val getter = staticMethod(facade, "getEMPTY", listOf(JsonObject.Companion::class.java))
        getter.callStatic(listOf(JsonObject.Companion)) shouldBe JsonObjectEmpty
    }
}
