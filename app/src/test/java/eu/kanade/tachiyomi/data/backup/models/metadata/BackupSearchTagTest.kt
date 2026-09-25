package eu.kanade.tachiyomi.data.backup.models.metadata

import exh.metadata.sql.models.SearchTag
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test

internal class BackupSearchTagTest {

    @Test
    fun namespaceDefaultsToNull() {
        val tag = BackupSearchTag(name = "n", type = 1)
        tag.namespace shouldBe null
        tag.name shouldBe "n"
        tag.type shouldBe 1
    }

    @Test
    fun dataClassMembers() {
        val tag = BackupSearchTag(namespace = "ns", name = "n", type = 2)
        tag shouldBe BackupSearchTag(namespace = "ns", name = "n", type = 2)
        tag shouldNotBe BackupSearchTag(name = "n", type = 2)
        tag.hashCode() shouldNotBe 0
        tag.toString() shouldNotBe ""
        tag.copy(type = 3).type shouldBe 3
    }

    @Test
    fun copyFromSearchTag() {
        val source = SearchTag(id = 4L, mangaId = 5L, namespace = "ns", name = "n", type = 6)
        BackupSearchTag.copyFrom(source) shouldBe BackupSearchTag(namespace = "ns", name = "n", type = 6)
    }

    @Test
    fun getSearchTagHasNullId() {
        val tag = BackupSearchTag(namespace = "ns", name = "n", type = 6)
        tag.getSearchTag(8L) shouldBe SearchTag(
            id = null,
            mangaId = 8L,
            namespace = "ns",
            name = "n",
            type = 6,
        )
    }

    @Test
    fun protoRoundTrip() {
        val tag = BackupSearchTag(namespace = "ns", name = "n", type = 7)
        val bytes = ProtoBuf.encodeToByteArray(BackupSearchTag.serializer(), tag)
        ProtoBuf.decodeFromByteArray(BackupSearchTag.serializer(), bytes) shouldBe tag
    }
}
