package exh.metadata.metadata

import exh.metadata.metadata.base.RaisedTag
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class RaisedSearchMetadataExtensionsTest {
    private val metadata = RankedSearchMetadata()

    @Test
    fun getItemSkipsNull() {
        val item: Int? = null
        metadata.getItem(item) { "label" } shouldBe null
    }

    @Test
    fun getItemRendersWithToString() {
        metadata.getItem(42) { "answer $it" } shouldBe ("answer 42" to "42")
    }

    @Test
    fun getItemUsesCustomRenderer() {
        metadata.getItem(item = 42, toString = { "0x" + it.toString(16) }) { "hex" } shouldBe ("hex" to "0x2a")
    }

    @Test
    fun ofNamespaceKeepsMatchingTags() {
        val artist = RaisedTag(namespace = "artist", name = "a", type = 0)
        val group = RaisedTag(namespace = "group", name = "g", type = 0)
        val bare = RaisedTag(namespace = null, name = "n", type = 0)
        listOf(artist, group, bare).ofNamespace("artist") shouldContainExactly listOf(artist)
        listOf(artist, group, bare).ofNamespace("missing") shouldBe emptyList()
    }
}
