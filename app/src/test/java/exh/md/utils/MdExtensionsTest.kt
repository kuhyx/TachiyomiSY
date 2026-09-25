package exh.md.utils

import exh.md.dto.ListCallDto
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

private data class Page(
    override val limit: Int,
    override val offset: Int,
    override val total: Int,
    override val data: List<String>,
) : ListCallDto<String>

internal class MdExtensionsTest {
    @Test
    fun mdListCallPagesUntilTotal() = runTest {
        val offsets = mutableListOf<Int>()
        val all = mdListCall { offset ->
            offsets += offset
            Page(limit = 2, offset = offset, total = 5, data = listOf("$offset-a", "$offset-b"))
        }
        offsets shouldContainExactly listOf(0, 2, 4)
        all shouldContainExactly listOf("0-a", "0-b", "2-a", "2-b", "4-a", "4-b")
    }

    @Test
    fun mdListCallSinglePage() = runTest {
        val all = mdListCall { Page(limit = 10, offset = it, total = 1, data = listOf("only")) }
        all shouldContainExactly listOf("only")
    }

    @Test
    fun asMdMapDecodesObjects() {
        buildJsonObject {
            put("en", "Title")
            put("ja", "タイトル")
        }.asMdMap<String>() shouldContainExactly
            mapOf("en" to "Title", "ja" to "タイトル")
    }

    @Test
    fun asMdMapFallsBackToEmpty() {
        JsonPrimitive("not an object").asMdMap<String>() shouldContainExactly emptyMap()
        buildJsonObject { put("en", buildJsonObject { }) }.asMdMap<String>() shouldContainExactly emptyMap()
    }
}
