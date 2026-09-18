package mihon.domain.manga.model

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test

internal class SMangaTest {

    @Test
    fun toDomainMangaCopiesTheSource() {
        val source = SManga(
            url = "/manga/1",
            title = "Title",
            artist = "Artist",
            author = "Author",
            description = "Description",
            genre = "Action, Drama",
            status = SManga.COMPLETED,
            thumbnailUrl = "/thumb.png",
            initialized = true,
        )
        source.update_strategy = UpdateStrategy.ONLY_FETCH_ONCE
        source.memo = JsonObject(mapOf("key" to JsonPrimitive("value")))

        val manga = source.toDomainManga(7L)

        manga.id shouldBe -1L
        manga.source shouldBe 7L
        manga.favorite shouldBe false
        manga.url shouldBe "/manga/1"
        manga.ogTitle shouldBe "Title"
        manga.ogArtist shouldBe "Artist"
        manga.ogAuthor shouldBe "Author"
        manga.ogDescription shouldBe "Description"
        manga.ogGenre shouldBe listOf("Action", "Drama")
        manga.ogStatus shouldBe SManga.COMPLETED.toLong()
        manga.ogThumbnailUrl shouldBe "/thumb.png"
        manga.updateStrategy shouldBe UpdateStrategy.ONLY_FETCH_ONCE
        manga.initialized shouldBe true
        manga.memo shouldBe source.memo
    }

    @Test
    fun blankSourceKeepsNulls() {
        val manga = SManga(url = "/manga/2", title = "Bare").toDomainManga(3L)

        manga.source shouldBe 3L
        manga.ogTitle shouldBe "Bare"
        manga.ogArtist shouldBe null
        manga.ogAuthor shouldBe null
        manga.ogDescription shouldBe null
        manga.ogGenre shouldBe null
        manga.ogStatus shouldBe 0L
        manga.ogThumbnailUrl shouldBe null
        manga.updateStrategy shouldBe UpdateStrategy.ALWAYS_UPDATE
        manga.initialized shouldBe false
    }
}
