package tachiyomi.data.manga

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.data.Search_metadata
import tachiyomi.data.Search_tags
import tachiyomi.data.Search_titles

internal class MangaMetadataMapperTest {
    @Test
    fun mapMetadataCopiesColumns() {
        val row = Search_metadata(
            manga_id = 1L, uploader = "up", extra = "{}", indexed_extra = "idx", extra_version = 2L,
        )

        val metadata = MangaMetadataMapper.mapMetadata(row)

        metadata.mangaId shouldBe 1L
        metadata.uploader shouldBe "up"
        metadata.extra shouldBe "{}"
        metadata.indexedExtra shouldBe "idx"
        metadata.extraVersion shouldBe 2
    }

    @Test
    fun mapMetadataKeepsNulls() {
        val row = Search_metadata(manga_id = 1L, uploader = null, extra = "", indexed_extra = null, extra_version = 0L)

        val metadata = MangaMetadataMapper.mapMetadata(row)

        metadata.uploader shouldBe null
        metadata.indexedExtra shouldBe null
    }

    @Test
    fun mapTitleCopiesColumns() {
        val title = MangaMetadataMapper.mapTitle(Search_titles(_id = 3L, manga_id = 1L, title = "t", type = 4L))

        title.id shouldBe 3L
        title.mangaId shouldBe 1L
        title.title shouldBe "t"
        title.type shouldBe 4
    }

    @Test
    fun mapTagCopiesColumns() {
        val row = Search_tags(_id = 3L, manga_id = 1L, namespace = "ns", name = "n", type = 4L)

        val tag = MangaMetadataMapper.mapTag(row)

        tag.id shouldBe 3L
        tag.mangaId shouldBe 1L
        tag.namespace shouldBe "ns"
        tag.name shouldBe "n"
        tag.type shouldBe 4
    }

    @Test
    fun mapTagKeepsNullNamespace() {
        val row = Search_tags(_id = 3L, manga_id = 1L, namespace = null, name = "n", type = 0L)

        MangaMetadataMapper.mapTag(row).namespace shouldBe null
    }
}
