package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test

internal class BackupSourceTest {

    @Test
    fun defaultName() {
        val source = BackupSource(sourceId = 7L)
        source.name shouldBe ""
        source.sourceId shouldBe 7L
    }

    @Test
    fun explicitName() {
        val source = BackupSource(name = "Store", sourceId = 9L)
        source.name shouldBe "Store"
        source.copy(name = "Other").name shouldBe "Other"
        source.toString() shouldNotBe ""
        source.hashCode() shouldBe BackupSource(name = "Store", sourceId = 9L).hashCode()
        source shouldBe BackupSource(name = "Store", sourceId = 9L)
        source shouldNotBe BackupSource(name = "Store", sourceId = 10L)
    }

    @Test
    fun protoRoundTrip() {
        val source = BackupSource(name = "Store", sourceId = 11L)
        val bytes = ProtoBuf.encodeToByteArray(BackupSource.serializer(), source)
        ProtoBuf.decodeFromByteArray(BackupSource.serializer(), bytes) shouldBe source
    }

    @Test
    fun protoMinimalPayload() {
        val bytes = ProtoBuf.encodeToByteArray(BackupSource.serializer(), BackupSource(sourceId = 3L))
        val decoded = ProtoBuf.decodeFromByteArray(BackupSource.serializer(), bytes)
        decoded.name shouldBe ""
        decoded.sourceId shouldBe 3L
    }
}
