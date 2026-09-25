package eu.kanade.tachiyomi.data.backup.models.metadata

import exh.metadata.sql.models.SearchTitle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test

internal class BackupSearchTitleTest {

    @Test
    fun holdsTitleAndType() {
        val title = BackupSearchTitle(title = "t", type = 1)
        title.title shouldBe "t"
        title.type shouldBe 1
    }

    @Test
    fun dataClassMembers() {
        val title = BackupSearchTitle(title = "t", type = 1)
        title shouldBe BackupSearchTitle(title = "t", type = 1)
        title shouldNotBe BackupSearchTitle(title = "u", type = 1)
        title.hashCode() shouldNotBe 0
        title.toString() shouldNotBe ""
        title.copy(type = 2).type shouldBe 2
    }

    @Test
    fun copyFromSearchTitle() {
        val source = SearchTitle(id = 3L, mangaId = 4L, title = "t", type = 5)
        BackupSearchTitle.copyFrom(source) shouldBe BackupSearchTitle(title = "t", type = 5)
    }

    @Test
    fun getSearchTitleHasNullId() {
        BackupSearchTitle(title = "t", type = 5).getSearchTitle(6L) shouldBe SearchTitle(
            id = null,
            mangaId = 6L,
            title = "t",
            type = 5,
        )
    }

    @Test
    fun protoRoundTrip() {
        val title = BackupSearchTitle(title = "t", type = 9)
        val bytes = ProtoBuf.encodeToByteArray(BackupSearchTitle.serializer(), title)
        ProtoBuf.decodeFromByteArray(BackupSearchTitle.serializer(), bytes) shouldBe title
    }
}
