package eu.kanade.tachiyomi.data.backup.models.metadata

import exh.metadata.sql.models.SearchMetadata
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test

internal class BackupSearchMetadataTest {

    @Test
    fun defaultsAreNull() {
        val meta = BackupSearchMetadata(extra = "{}", extraVersion = 2)
        meta.uploader shouldBe null
        meta.indexedExtra shouldBe null
        meta.extra shouldBe "{}"
        meta.extraVersion shouldBe 2
    }

    @Test
    fun dataClassMembers() {
        val meta = BackupSearchMetadata(
            uploader = "u",
            extra = "{}",
            indexedExtra = "i",
            extraVersion = 1,
        )
        meta shouldBe BackupSearchMetadata(
            uploader = "u",
            extra = "{}",
            indexedExtra = "i",
            extraVersion = 1,
        )
        meta shouldNotBe BackupSearchMetadata(extra = "{}", extraVersion = 1)
        meta.hashCode() shouldNotBe 0
        meta.toString() shouldNotBe ""
        meta.copy(uploader = "v").uploader shouldBe "v"
    }

    @Test
    fun copyFromSearchMetadata() {
        val source = SearchMetadata(
            mangaId = 5L,
            uploader = "u",
            extra = "{}",
            indexedExtra = "i",
            extraVersion = 3,
        )
        BackupSearchMetadata.copyFrom(source) shouldBe BackupSearchMetadata(
            uploader = "u",
            extra = "{}",
            indexedExtra = "i",
            extraVersion = 3,
        )
    }

    @Test
    fun getSearchMetadataAddsMangaId() {
        val meta = BackupSearchMetadata(
            uploader = "u",
            extra = "{}",
            indexedExtra = "i",
            extraVersion = 3,
        )
        meta.getSearchMetadata(9L) shouldBe SearchMetadata(
            mangaId = 9L,
            uploader = "u",
            extra = "{}",
            indexedExtra = "i",
            extraVersion = 3,
        )
    }

    @Test
    fun protoRoundTrip() {
        val meta = BackupSearchMetadata(uploader = "u", extra = "{}", extraVersion = 1)
        val bytes = ProtoBuf.encodeToByteArray(BackupSearchMetadata.serializer(), meta)
        ProtoBuf.decodeFromByteArray(BackupSearchMetadata.serializer(), bytes) shouldBe meta
    }
}
