package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.core.common.extensions.JsonObjectEmptyBytes
import org.junit.jupiter.api.Test
import tachiyomi.data.MemoColumnAdapter

internal class BackupChapterTest {

    @Test
    fun defaults() {
        val chapter = BackupChapter(url = "/c", name = "C")
        chapter.scanlator shouldBe null
        chapter.read shouldBe false
        chapter.bookmark shouldBe false
        chapter.lastPageRead shouldBe 0L
        chapter.dateFetch shouldBe 0L
        chapter.dateUpload shouldBe 0L
        chapter.chapterNumber shouldBe 0F
        chapter.sourceOrder shouldBe 0L
        chapter.lastModifiedAt shouldBe 0L
        chapter.version shouldBe 0L
        chapter.memo shouldBe JsonObjectEmptyBytes
    }

    @Test
    fun propertiesAreMutable() {
        val chapter = BackupChapter(url = "/c", name = "C")
        chapter.url = "/d"
        chapter.name = "D"
        chapter.scanlator = "s"
        chapter.read = true
        chapter.bookmark = true
        chapter.url shouldBe "/d"
        chapter.name shouldBe "D"
        chapter.scanlator shouldBe "s"
        chapter.read shouldBe true
        chapter.bookmark shouldBe true
    }

    @Test
    fun toChapterImplMapsFields() {
        val memo = JsonObject(mapOf("k" to JsonPrimitive("v")))
        val chapter = BackupChapter(
            url = "/c",
            name = "C",
            scanlator = "s",
            read = true,
            bookmark = true,
            lastPageRead = 4L,
            dateFetch = 5L,
            dateUpload = 6L,
            chapterNumber = 7.5F,
            sourceOrder = 8L,
            lastModifiedAt = 9L,
            version = 10L,
            memo = MemoColumnAdapter.encode(memo),
        )
        val impl = chapter.toChapterImpl()
        impl.url shouldBe "/c"
        impl.name shouldBe "C"
        impl.chapterNumber shouldBe 7.5
        impl.scanlator shouldBe "s"
        impl.read shouldBe true
        impl.bookmark shouldBe true
        impl.lastPageRead shouldBe 4L
        impl.dateFetch shouldBe 5L
        impl.dateUpload shouldBe 6L
        impl.sourceOrder shouldBe 8L
        impl.lastModifiedAt shouldBe 9L
        impl.version shouldBe 10L
        impl.memo shouldBe memo
    }

    @Test
    fun mapperNarrowsChapterNumber() {
        val memo = JsonObject(mapOf("k" to JsonPrimitive("v")))
        val chapter = backupChapterMapper(
            1L,
            2L,
            "/c",
            "C",
            "s",
            true,
            false,
            3L,
            4.5,
            5L,
            6L,
            7L,
            8L,
            9L,
            10L,
            memo,
        )
        chapter.url shouldBe "/c"
        chapter.name shouldBe "C"
        chapter.chapterNumber shouldBe 4.5F
        chapter.scanlator shouldBe "s"
        chapter.read shouldBe true
        chapter.bookmark shouldBe false
        chapter.lastPageRead shouldBe 3L
        chapter.sourceOrder shouldBe 5L
        chapter.dateFetch shouldBe 6L
        chapter.dateUpload shouldBe 7L
        chapter.lastModifiedAt shouldBe 8L
        chapter.version shouldBe 9L
        MemoColumnAdapter.decode(chapter.memo) shouldBe memo
    }

    @Test
    fun mapperWithNullScanlator() {
        val chapter = backupChapterMapper(
            1L,
            2L,
            "/c",
            "C",
            null,
            false,
            false,
            0L,
            0.0,
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            JsonObject(emptyMap()),
        )
        chapter.scanlator shouldBe null
    }

    @Test
    fun protoRoundTrip() {
        val chapter = BackupChapter(url = "/c", name = "C", scanlator = "s", read = true, chapterNumber = 2F)
        val bytes = ProtoBuf.encodeToByteArray(BackupChapter.serializer(), chapter)
        val decoded = ProtoBuf.decodeFromByteArray(BackupChapter.serializer(), bytes)
        decoded.url shouldBe "/c"
        decoded.name shouldBe "C"
        decoded.scanlator shouldBe "s"
        decoded.read shouldBe true
        decoded.chapterNumber shouldBe 2F
    }

    @Test
    fun protoMinimalPayload() {
        val bytes = ProtoBuf.encodeToByteArray(BackupChapter.serializer(), BackupChapter(url = "/c", name = "C"))
        val decoded = ProtoBuf.decodeFromByteArray(BackupChapter.serializer(), bytes)
        decoded.scanlator shouldBe null
        decoded.memo shouldBe JsonObjectEmptyBytes
        decoded.version shouldBe 0L
    }
}
