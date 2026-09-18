package tachiyomi.domain.manga.model

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test

internal class MangaComponentsTest {

    private val manga = MangaFixtures.manga(id = 11L).copy(
        source = 12L,
        lastUpdate = 13L,
        nextUpdate = 14L,
        fetchInterval = 15,
        dateAdded = 16L,
        viewerFlags = 17L,
        chapterFlags = 18L,
        coverLastModified = 19L,
        url = "/u",
        ogTitle = "t",
        ogArtist = "ar",
        ogAuthor = "au",
        ogThumbnailUrl = "th",
        ogDescription = "de",
        ogGenre = listOf("g"),
        ogStatus = 20L,
        updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE,
        initialized = true,
        lastModifiedAt = 21L,
        favoriteModifiedAt = 22L,
        version = 23L,
        notes = "n",
        memo = JsonObject(mapOf("m" to JsonPrimitive(1))),
    )

    @Test
    fun componentsOneToNine() {
        manga.component1() shouldBe 11L
        manga.component2() shouldBe 12L
        manga.component3() shouldBe false
        manga.component4() shouldBe 13L
        manga.component5() shouldBe 14L
        manga.component6() shouldBe 15
        manga.component7() shouldBe 16L
        manga.component8() shouldBe 17L
        manga.component9() shouldBe 18L
    }

    @Test
    fun componentsTenToEighteen() {
        manga.component10() shouldBe 19L
        manga.component11() shouldBe "/u"
        manga.component12() shouldBe "t"
        manga.component13() shouldBe "ar"
        manga.component14() shouldBe "au"
        manga.component15() shouldBe "th"
        manga.component16() shouldBe "de"
        manga.component17() shouldBe listOf("g")
        manga.component18() shouldBe 20L
    }

    @Test
    fun componentsNineteenToEnd() {
        manga.component19() shouldBe UpdateStrategy.ONLY_FETCH_ONCE
        manga.component20() shouldBe true
        manga.component21() shouldBe 21L
        manga.component22() shouldBe 22L
        manga.component23() shouldBe 23L
        manga.component24() shouldBe "n"
        manga.component25() shouldBe JsonObject(mapOf("m" to JsonPrimitive(1)))
    }

    @Test
    fun destructuringReadsLeading() {
        val (id, source, favorite) = manga

        id shouldBe 11L
        source shouldBe 12L
        favorite shouldBe false
    }
}
