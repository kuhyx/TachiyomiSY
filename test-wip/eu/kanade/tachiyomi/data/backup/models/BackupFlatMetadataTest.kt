package eu.kanade.tachiyomi.data.backup.models

import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test

internal class BackupFlatMetadataTest {

    @Test
    fun listsDefaultToEmpty() {
        val meta = BackupFlatMetadata(searchMetadata = backupSearchMetadata)
        meta.searchTags shouldBe emptyList()
        meta.searchTitles shouldBe emptyList()
    }

    @Test
    fun dataClassMembers() {
        val meta = backupFlatMetadata()
        meta shouldBe backupFlatMetadata()
        meta shouldNotBe BackupFlatMetadata(searchMetadata = backupSearchMetadata)
        meta.hashCode() shouldNotBe 0
        meta.toString() shouldNotBe ""
        meta.copy(searchTags = emptyList()).searchTags shouldBe emptyList()
    }

    @Test
    fun copyFromFlatMetadata() {
        val flat = FlatMetadata(
            metadata = SearchMetadata(
                mangaId = 1L,
                uploader = "u",
                extra = "{}",
                indexedExtra = "i",
                extraVersion = 1,
            ),
            tags = listOf(SearchTag(id = null, mangaId = 1L, namespace = "ns", name = "n", type = 2)),
            titles = listOf(SearchTitle(id = null, mangaId = 1L, title = "t", type = 3)),
        )
        BackupFlatMetadata.copyFrom(flat) shouldBe backupFlatMetadata()
    }

    @Test
    fun getFlatMetadataStampsMangaId() {
        val flat = backupFlatMetadata().getFlatMetadata(7L)
        flat.metadata.mangaId shouldBe 7L
        flat.tags.single().mangaId shouldBe 7L
        flat.titles.single().mangaId shouldBe 7L
    }

    @Test
    fun protoRoundTrip() {
        val meta = backupFlatMetadata()
        val bytes = ProtoBuf.encodeToByteArray(BackupFlatMetadata.serializer(), meta)
        ProtoBuf.decodeFromByteArray(BackupFlatMetadata.serializer(), bytes) shouldBe meta
    }
}
