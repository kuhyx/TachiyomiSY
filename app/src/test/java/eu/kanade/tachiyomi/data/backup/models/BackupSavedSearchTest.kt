package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test

internal class BackupSavedSearchTest {

    @Test
    fun defaults() {
        val search = BackupSavedSearch(name = "S")
        search.name shouldBe "S"
        search.query shouldBe ""
        search.filterList shouldBe ""
        search.source shouldBe 0L
    }

    @Test
    fun dataClassMembers() {
        val search = BackupSavedSearch(name = "S", query = "q", filterList = "[]", source = 3L)
        search shouldBe BackupSavedSearch(name = "S", query = "q", filterList = "[]", source = 3L)
        search shouldNotBe BackupSavedSearch(name = "T", query = "q", filterList = "[]", source = 3L)
        search.hashCode() shouldBe
            BackupSavedSearch(name = "S", query = "q", filterList = "[]", source = 3L).hashCode()
        search.toString() shouldNotBe ""
        search.copy(query = "z").query shouldBe "z"
    }

    @Test
    fun mapperWithValues() {
        val search = backupSavedSearchMapper(1L, 2L, "Name", "query", "[{}]")
        search.source shouldBe 2L
        search.name shouldBe "Name"
        search.query shouldBe "query"
        search.filterList shouldBe "[{}]"
    }

    @Test
    fun mapperWithNulls() {
        val search = backupSavedSearchMapper(1L, 2L, "Name", null, null)
        search.query shouldBe ""
        search.filterList shouldBe "[]"
    }

    @Test
    fun protoRoundTrip() {
        val search = BackupSavedSearch(name = "S", query = "q", filterList = "[]", source = 4L)
        val bytes = ProtoBuf.encodeToByteArray(BackupSavedSearch.serializer(), search)
        ProtoBuf.decodeFromByteArray(BackupSavedSearch.serializer(), bytes) shouldBe search
    }

    @Test
    fun protoMinimalPayload() {
        val bytes = ProtoBuf.encodeToByteArray(BackupSavedSearch.serializer(), BackupSavedSearch(name = "S"))
        val decoded = ProtoBuf.decodeFromByteArray(BackupSavedSearch.serializer(), bytes)
        decoded.query shouldBe ""
        decoded.source shouldBe 0L
    }
}
