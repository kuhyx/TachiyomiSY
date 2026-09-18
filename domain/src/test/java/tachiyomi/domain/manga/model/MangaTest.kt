package tachiyomi.domain.manga.model

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test

internal class MangaTest {

    private val sourceManga = MangaFixtures.manga().copy(
        ogTitle = "Source title",
        ogArtist = "Source artist",
        ogAuthor = "Source author",
        ogThumbnailUrl = "https://example.com/source.png",
        ogDescription = "Source description",
        ogGenre = listOf("Source genre"),
        ogStatus = 1L,
    )

    @Test
    fun createIsBlankAndNotFavorite() {
        val manga = Manga.create()

        manga.id shouldBe -1L
        manga.source shouldBe -1L
        manga.favorite shouldBe false
        manga.url shouldBe ""
        manga.ogTitle shouldBe ""
        manga.ogArtist shouldBe null
        manga.ogGenre shouldBe null
        manga.ogStatus shouldBe 0L
        manga.updateStrategy shouldBe UpdateStrategy.ALWAYS_UPDATE
        manga.initialized shouldBe false
        manga.favoriteModifiedAt shouldBe null
        manga.notes shouldBe ""
        manga.memo shouldBe JsonObject(emptyMap())
    }

    @Test
    fun nonFavoriteUsesSourceFields() {
        val manga = sourceManga

        manga.title shouldBe "Source title"
        manga.author shouldBe "Source author"
        manga.artist shouldBe "Source artist"
        manga.thumbnailUrl shouldBe "https://example.com/source.png"
        manga.description shouldBe "Source description"
        manga.genre shouldContainExactly listOf("Source genre")
        manga.status shouldBe 1L
    }

    @Test
    fun favoriteNoInfoUsesSource() {
        val manga = sourceManga.copy(id = MangaFixtures.PLAIN_FAVORITE_ID, favorite = true)

        manga.title shouldBe "Source title"
        manga.author shouldBe "Source author"
        manga.artist shouldBe "Source artist"
        manga.thumbnailUrl shouldBe "https://example.com/source.png"
        manga.description shouldBe "Source description"
        manga.genre shouldContainExactly listOf("Source genre")
        manga.status shouldBe 1L
    }

    @Test
    fun fullCustomInfoOverrides() {
        val manga = sourceManga.copy(id = MangaFixtures.CUSTOM_ID, favorite = true)

        manga.title shouldBe "Custom title"
        manga.author shouldBe "Custom author"
        manga.artist shouldBe "Custom artist"
        manga.thumbnailUrl shouldBe "https://example.com/custom.png"
        manga.description shouldBe "Custom description"
        manga.genre shouldContainExactly listOf("Custom genre")
        manga.status shouldBe 5L
        manga.ogTitle shouldBe "Source title"
    }

    @Test
    fun blankCustomInfoFallsBack() {
        val manga = sourceManga.copy(id = MangaFixtures.BLANK_CUSTOM_ID, favorite = true)

        manga.title shouldBe "Source title"
        manga.author shouldBe "Source author"
        manga.artist shouldBe "Source artist"
        manga.thumbnailUrl shouldBe "https://example.com/source.png"
        manga.description shouldBe "Source description"
        manga.genre shouldContainExactly listOf("Source genre")
        manga.status shouldBe 1L
    }

    @Test
    fun customInfoReadAtConstruct() {
        val before = sourceManga.copy(id = 9_100_050L, favorite = true)
        MangaFixtures.customInfos[9_100_050L] = CustomMangaInfo(id = 9_100_050L, title = "Late title")
        val after = before.copy()
        MangaFixtures.customInfos.remove(9_100_050L)

        before.title shouldBe "Source title"
        after.title shouldBe "Late title"
    }

    @Test
    fun equalsIgnoresCustomInfo() {
        val plain = sourceManga.copy(id = MangaFixtures.CUSTOM_ID, favorite = true)
        val same = sourceManga.copy(id = MangaFixtures.CUSTOM_ID, favorite = true)
        val other = sourceManga.copy(id = MangaFixtures.PLAIN_FAVORITE_ID, favorite = true)

        plain shouldBe same
        plain.hashCode() shouldBe same.hashCode()
        (plain == other) shouldBe false
        plain.toString() shouldBe same.toString()
    }

    @Test
    fun toStringNamesSourceFields() {
        val text = sourceManga.toString()

        text.startsWith("Manga(id=1, source=-1, favorite=false") shouldBe true
        text.contains("ogTitle=Source title") shouldBe true
        text.contains("notes=, memo={})") shouldBe true
    }

    @Test
    fun copyKeepsUntouchedFields() {
        val copy = sourceManga.copy(notes = "note", version = 3L, lastModifiedAt = 4L)

        copy.notes shouldBe "note"
        copy.version shouldBe 3L
        copy.lastModifiedAt shouldBe 4L
        copy.ogTitle shouldBe "Source title"
        copy.copy() shouldBe copy
    }
}
