package tachiyomi.domain.manga.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MangaCoverTest {

    private fun cover(mangaId: Long, favorite: Boolean, ogUrl: String? = "https://example.com/og.png"): MangaCover =
        MangaFixtures.cover(mangaId = mangaId, favorite = favorite, ogUrl = ogUrl)

    @Test
    fun nonFavoriteUsesSourceUrl() {
        cover(mangaId = MangaFixtures.CUSTOM_ID, favorite = false).url shouldBe "https://example.com/og.png"
    }

    @Test
    fun favoriteNoInfoUsesSource() {
        cover(mangaId = MangaFixtures.PLAIN_FAVORITE_ID, favorite = true).url shouldBe "https://example.com/og.png"
    }

    @Test
    fun favoriteWithCustomUrlUsesIt() {
        cover(mangaId = MangaFixtures.CUSTOM_ID, favorite = true).url shouldBe "https://example.com/custom.png"
    }

    @Test
    fun blankCustomInfoUsesSource() {
        cover(mangaId = MangaFixtures.BLANK_CUSTOM_ID, favorite = true).url shouldBe "https://example.com/og.png"
    }

    @Test
    fun nullEverywhereStaysNull() {
        cover(mangaId = MangaFixtures.BLANK_CUSTOM_ID, favorite = true, ogUrl = null).url shouldBe null
        cover(mangaId = 1L, favorite = false, ogUrl = null).url shouldBe null
    }

    @Test
    fun dataClassMembers() {
        val a = cover(mangaId = 1L, favorite = false)
        val b = cover(mangaId = 1L, favorite = false)
        val (mangaId, sourceId, favorite) = a

        a shouldBe b
        a.hashCode() shouldBe b.hashCode()
        a.toString() shouldBe b.toString()
        a.copy(lastModified = 10L).lastModified shouldBe 10L
        a.copy(lastModified = 10L).url shouldBe "https://example.com/og.png"
        mangaId shouldBe 1L
        sourceId shouldBe 3L
        favorite shouldBe false
        a.component4() shouldBe "https://example.com/og.png"
        a.component5() shouldBe 9L
        a.toString().contains("mangaId=1") shouldBe true
    }

    @Test
    fun asMangaCoverCopiesFields() {
        val manga = MangaFixtures.manga(id = 5L).copy(
            source = 6L,
            ogThumbnailUrl = "https://example.com/manga.png",
            coverLastModified = 77L,
        )

        val result = manga.asMangaCover()

        result shouldBe MangaCover(
            mangaId = 5L,
            sourceId = 6L,
            isMangaFavorite = false,
            ogUrl = "https://example.com/manga.png",
            lastModified = 77L,
        )
        result.url shouldBe "https://example.com/manga.png"
    }

    @Test
    fun asMangaCoverAppliesCustomUrl() {
        val manga = MangaFixtures.favorite(MangaFixtures.CUSTOM_ID).copy(ogThumbnailUrl = "https://example.com/x.png")

        val result = manga.asMangaCover()

        result.isMangaFavorite shouldBe true
        result.ogUrl shouldBe "https://example.com/custom.png"
        result.url shouldBe "https://example.com/custom.png"
    }
}
