package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.MergedMangaReference

internal class BackupMergedMangaReferenceTest {

    @Test
    fun holdsEveryField() {
        val reference = backupMergedMangaReference()
        reference.isInfoManga shouldBe true
        reference.getChapterUpdates shouldBe false
        reference.chapterSortMode shouldBe 1
        reference.chapterPriority shouldBe 2
        reference.downloadChapters shouldBe true
        reference.mergeUrl shouldBe "merge://1"
        reference.mangaUrl shouldBe "/m"
        reference.mangaSourceId shouldBe 3L
    }

    @Test
    fun dataClassMembers() {
        val reference = backupMergedMangaReference()
        reference shouldBe backupMergedMangaReference()
        reference shouldNotBe backupMergedMangaReference().copy(mangaUrl = "/n")
        reference.hashCode() shouldBe backupMergedMangaReference().hashCode()
        reference.toString() shouldNotBe ""
        reference.copy(chapterPriority = 9).chapterPriority shouldBe 9
    }

    @Test
    fun getMergedReferenceBlanksIds() {
        backupMergedMangaReference().getMergedMangaReference() shouldBe MergedMangaReference(
            id = -1L,
            isInfoManga = true,
            getChapterUpdates = false,
            chapterSortMode = 1,
            chapterPriority = 2,
            downloadChapters = true,
            mergeId = null,
            mergeUrl = "merge://1",
            mangaId = null,
            mangaUrl = "/m",
            mangaSourceId = 3L,
        )
    }

    @Test
    fun mapperNarrowsLongsToInts() {
        val reference = backupMergedMangaReferenceMapper(
            9L,
            true,
            false,
            1L,
            2L,
            true,
            8L,
            "merge://1",
            null,
            "/m",
            3L,
        )
        reference shouldBe backupMergedMangaReference()
    }

    @Test
    fun protoRoundTrip() {
        val reference = backupMergedMangaReference()
        val bytes = ProtoBuf.encodeToByteArray(BackupMergedMangaReference.serializer(), reference)
        ProtoBuf.decodeFromByteArray(BackupMergedMangaReference.serializer(), bytes) shouldBe reference
    }
}
