package tachiyomi.domain.manga.model

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test

internal class MangaUpdateTest {

    private val memo = JsonObject(mapOf("k" to JsonPrimitive("v")))

    private val full = MangaUpdate(
        id = 1L,
        source = 2L,
        favorite = true,
        lastUpdate = 3L,
        nextUpdate = 4L,
        fetchInterval = 5,
        dateAdded = 6L,
        viewerFlags = 7L,
        chapterFlags = 8L,
        coverLastModified = 9L,
        url = "/u",
        title = "t",
        artist = "ar",
        author = "au",
        description = "de",
        genre = listOf("g"),
        status = 10L,
        thumbnailUrl = "th",
        updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE,
        initialized = true,
        version = 11L,
        notes = "n",
        memo = memo,
        filteredScanlators = listOf("s"),
    )

    @Test
    fun defaultsAreNull() {
        val update = MangaUpdate(id = 5L)

        update.id shouldBe 5L
        update.source shouldBe null
        update.favorite shouldBe null
        update.title shouldBe null
        update.genre shouldBe null
        update.updateStrategy shouldBe null
        update.memo shouldBe null
        update.filteredScanlators shouldBe null
    }

    @Test
    fun componentsOneToTwelve() {
        full.component1() shouldBe 1L
        full.component2() shouldBe 2L
        full.component3() shouldBe true
        full.component4() shouldBe 3L
        full.component5() shouldBe 4L
        full.component6() shouldBe 5
        full.component7() shouldBe 6L
        full.component8() shouldBe 7L
        full.component9() shouldBe 8L
        full.component10() shouldBe 9L
        full.component11() shouldBe "/u"
        full.component12() shouldBe "t"
    }

    @Test
    fun componentsThirteenToEnd() {
        full.component13() shouldBe "ar"
        full.component14() shouldBe "au"
        full.component15() shouldBe "de"
        full.component16() shouldBe listOf("g")
        full.component17() shouldBe 10L
        full.component18() shouldBe "th"
        full.component19() shouldBe UpdateStrategy.ONLY_FETCH_ONCE
        full.component20() shouldBe true
        full.component21() shouldBe 11L
        full.component22() shouldBe "n"
        full.component23() shouldBe memo
        full.component24() shouldBe listOf("s")
    }

    @Test
    fun dataClassMembers() {
        val same = full.copy()
        val (id, source, favorite) = full

        same shouldBe full
        same.hashCode() shouldBe full.hashCode()
        same.toString() shouldBe full.toString()
        full.toString().startsWith("MangaUpdate(id=1, source=2, favorite=true") shouldBe true
        (full == full.copy(notes = "other")) shouldBe false
        id shouldBe 1L
        source shouldBe 2L
        favorite shouldBe true
    }

    @Test
    fun toMangaUpdateCopiesSource() {
        val manga = MangaFixtures.favorite(MangaFixtures.CUSTOM_ID).copy(
            source = 2L,
            lastUpdate = 3L,
            nextUpdate = 4L,
            fetchInterval = 5,
            dateAdded = 6L,
            viewerFlags = 7L,
            chapterFlags = 8L,
            coverLastModified = 9L,
            url = "/u",
            ogTitle = "t",
            ogArtist = "ar",
            ogAuthor = "au",
            ogThumbnailUrl = "th",
            ogDescription = "de",
            ogGenre = listOf("g"),
            ogStatus = 10L,
            updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE,
            initialized = true,
            version = 11L,
            notes = "n",
            memo = memo,
        )

        manga.title shouldBe "Custom title"
        manga.toMangaUpdate() shouldBe full.copy(id = MangaFixtures.CUSTOM_ID, filteredScanlators = null)
    }

    @Test
    fun toMangaUpdateKeepsNulls() {
        val update = Manga.create().toMangaUpdate()

        update.id shouldBe -1L
        update.favorite shouldBe false
        update.artist shouldBe null
        update.genre shouldBe null
        update.memo shouldBe JsonObject(emptyMap())
        update.filteredScanlators shouldBe null
    }
}
